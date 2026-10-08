// Pages keeps the game and API on one origin; game state lives in the Worker.
export default {
  async fetch(request, env) {
    if (new URL(request.url).pathname.startsWith('/api/')) {
      return env.MULTIPLAYER.fetch(request);
    }
    return env.ASSETS.fetch(request);
  },
};
