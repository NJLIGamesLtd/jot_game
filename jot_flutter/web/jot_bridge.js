// Each game owns a worker and one C++ handle. Requests are queued by the worker,
// and native JSON responses are copied before another command uses that handle.
(() => {
  let nextHandle = 1;
  const engines = new Map();

  function fail(engine, error) {
    if (engine.failure) return;
    engine.failure = error;
    for (const pending of engine.pending.values()) pending.reject(error);
    engine.pending.clear();
    engine.worker.terminate();
  }

  function request(engine, op, data = {}) {
    if (engine.failure) return Promise.reject(engine.failure);
    if (engine.closing && op !== 'close') {
      return Promise.reject(new Error('Engine is closed.'));
    }
    return new Promise((resolve, reject) => {
      const id = engine.nextId++;
      engine.pending.set(id, { resolve, reject });
      try {
        engine.worker.postMessage({ id, op, ...data });
      } catch (error) {
        engine.pending.delete(id);
        reject(error);
        fail(engine, error);
      }
    });
  }

  window.jotBridge = {
    async create(dictionary, seed, robot) {
      const worker = new Worker(new URL('jot_worker.js', document.baseURI));
      const engine = {
        worker, nextId: 1, pending: new Map(), failure: null,
        closing: false, closePromise: null,
      };
      worker.onmessage = ({ data }) => {
        if (data.fatal) {
          fail(engine, new Error(data.error || 'The WebAssembly worker failed.'));
          return;
        }
        const pending = engine.pending.get(data.id);
        if (!pending) return;
        engine.pending.delete(data.id);
        if (data.error) pending.reject(new Error(data.error));
        else pending.resolve(data.result);
      };
      worker.onerror = (event) => {
        fail(engine, new Error(event.message || 'C++ worker failed to start.'));
      };
      worker.onmessageerror = () => {
        fail(engine, new Error('The WebAssembly worker returned unreadable data.'));
      };
      try {
        await request(engine, 'create', { dictionary, seed, robot });
      } catch (error) {
        fail(engine, error);
        throw error;
      }
      const handle = nextHandle++;
      engines.set(handle, engine);
      return handle;
    },

    execute(handle, command) {
      const engine = engines.get(handle);
      if (!engine) return Promise.reject(new Error('Engine is closed.'));
      return request(engine, 'execute', { command });
    },

    close(handle) {
      const engine = engines.get(handle);
      if (!engine) return Promise.resolve();
      if (engine.closePromise) return engine.closePromise;
      engine.closing = true;
      engine.closePromise = request(engine, 'close').finally(() => {
        engines.delete(handle);
        engine.worker.terminate();
      });
      return engine.closePromise;
    },
  };
})();
