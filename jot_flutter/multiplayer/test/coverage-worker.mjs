import actualWorker, { GameRoom as ProductionGameRoom } from '../src/index.ts';
import { createGame } from '../server_core.mjs';

// This entry point is used only by coverage-build.mjs. It is absent from the
// production Wrangler entry point and never included in a deploy command.
export class GameRoom extends ProductionGameRoom {
  async testAlarm() { await this.alarm(); }
}
export default {
  async fetch(request, env) {
    const path = new URL(request.url).pathname;
    if (!path.startsWith('/__test__/')) return actualWorker.fetch(request, env);
    const input = await request.json();
    if (path === '/__test__/fault') {
      globalThis.__jotFault = input;
      return Response.json({ ok: true });
    }
    if (path === '/__test__/core') {
      let game;
      try {
        game = await createGame(input.seed ?? 1, input.save);
        if (input.closeFirst) game.close();
        const result = game.execute(input.command ?? { op: 'state' });
        game.close();
        if (input.doubleClose) game.close();
        return Response.json(result);
      } catch (error) { return Response.json({ error: error.message }, { status: 500 }); }
      finally { game?.close(); }
    }
    if (path === '/__test__/rpc') {
      const stub = env.ROOMS.getByName(input.roomId);
      const result = await stub[input.method](...input.args);
      return Response.json(result === undefined ? {} : JSON.parse(result));
    }
    if (path === '/__test__/invoke') {
      const testEnv = { ...env };
      if (input.faultIngress) testEnv.INGRESS_LIMITER = { limit() { throw input.faultIngress; } };
      if (input.faultError) testEnv.INGRESS_LIMITER = { limit() { throw new Error('Simulated rate limiter outage'); } };
      if (input.rejectIngress) testEnv.INGRESS_LIMITER = { async limit() { return { success: false }; } };
      if (input.rejectApi) testEnv.API_LIMITER = { async limit() { return { success: false }; } };
      let body = input.body;
      if (input.chunks) body = new ReadableStream({
        start(controller) {
          for (const chunk of input.chunks) controller.enqueue(Uint8Array.from(chunk));
          if (input.streamError) controller.error(new Error('Simulated interrupted request body'));
          else controller.close();
        },
      });
      return actualWorker.fetch(new Request(input.url ?? 'https://jot-game.pages.dev/api/rooms', {
        method: input.method ?? 'POST', headers: input.headers, ...(body === undefined ? {} : { body }),
      }), testEnv);
    }
    return new Response('Unknown coverage helper', { status: 404 });
  },
};
