importScripts('jot_core.js');
let moduleInstance;
let handle = 0;
let execute;
let failed = false;
// Queue messages while the asynchronous WebAssembly module is loading, and
// preserve the same command ordering as the native isolate transport.
let queue = Promise.resolve();
self.onmessage = ({ data }) => {
  queue = queue.then(async () => {
    try {
      if (failed) throw new Error('The WebAssembly engine is unavailable.');
      let result = null;
      if (data.op === 'create') {
        if (moduleInstance) throw new Error('The engine was already created.');
        moduleInstance = await createJotModule({
          onAbort: (reason) => {
            failed = true;
            self.postMessage({ fatal: true, error: String(reason) });
          },
        });
        const create = moduleInstance.cwrap('jot_create', 'number', ['string', 'number', 'number']);
        execute = moduleInstance.cwrap('jot_execute', 'string', ['number', 'string']);
        handle = create(data.dictionary, data.seed, data.robot);
        if (!handle) throw new Error('The C++ engine could not load the dictionary.');
      } else if (data.op === 'execute') {
        if (!handle) throw new Error('Engine is closed.');
        result = execute(handle, data.command);
      } else if (data.op === 'close') {
        if (handle) moduleInstance._jot_destroy(handle);
        handle = 0;
      } else {
        throw new Error('Unknown worker request.');
      }
      self.postMessage({ id: data.id, result });
    } catch (error) {
      self.postMessage({ id: data.id, error: error.message || String(error) });
    }
  }).catch((error) => {
    failed = true;
    self.postMessage({ fatal: true, error: error.message || String(error) });
  });
};
