import WebSocket from 'ws';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { randomUUID } from 'node:crypto';

export async function collectCoverage(mf) {
  const inspector = await mf.getInspectorURL();
  const discovery = new URL(inspector);
  discovery.protocol = 'http:'; discovery.pathname = '/json';
  const targets = await fetch(discovery).then((response) => response.json());
  const target = targets.find((item) => item.id.includes('core:user:jot-multiplayer'));
  if (!target) throw new Error('Cannot locate instrumented workerd isolate.');
  const socket = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((accept, reject) => { socket.once('open', accept); socket.once('error', reject); });
  try {
    const contexts = [];
    let sequence = 0;
    const pending = new Map();
    socket.on('message', (raw) => {
      const message = JSON.parse(String(raw));
      if (message.method === 'Runtime.executionContextCreated') contexts.push(message.params.context);
      if (message.id && pending.has(message.id)) {
        const { accept, reject, timer } = pending.get(message.id); pending.delete(message.id); clearTimeout(timer);
        if (message.error || message.result?.exceptionDetails) reject(new Error(JSON.stringify(message)));
        else accept(message.result);
      }
    });
    const command = (method, params = {}) => new Promise((accept, reject) => {
      const id = ++sequence;
      const timer = setTimeout(() => reject(new Error(`Timed out reading workerd ${method}`)), 5000);
      pending.set(id, { accept, reject, timer });
      socket.send(JSON.stringify({ id, method, params }));
    });
    await command('Runtime.enable');
    if (!contexts.length) throw new Error(`Inspector has no Worker context: ${JSON.stringify(target)}`);
    const result = await new Promise((accept, reject) => {
      command('Runtime.evaluate', { expression: 'JSON.stringify(globalThis.__coverage__ ?? {})', returnByValue: true,
        contextId: contexts[0].id }).then((message) => accept(message.result.value), reject);
    });
    const directory = resolve('../coverage/multiplayer/raw');
    await mkdir(directory, { recursive: true });
    await writeFile(resolve(directory, `${randomUUID()}.json`), result);
  } finally { socket.close(); }
}
