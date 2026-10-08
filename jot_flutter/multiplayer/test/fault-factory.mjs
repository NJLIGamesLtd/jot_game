import actualFactory from '../generated/jot_server_core.mjs';

// Fault injection lives entirely in this undeployable test build. It replaces
// only the generated C ABI boundary; normal commands run the real C++ WASM.
export default async function factory(options) {
  if (globalThis.__jotFault?.kind === 'pause') {
    const url = globalThis.__jotFault.url;
    globalThis.__jotFault = undefined;
    await fetch(url);
  }
  if (globalThis.__jotFault?.kind === 'factory') {
    globalThis.__jotFault = undefined;
    throw new Error('Simulated runtime initialization failure');
  }
  const module = await actualFactory(options);
  const actualCwrap = module.cwrap;
  module.cwrap = (name, ...args) => {
    const actual = actualCwrap(name, ...args);
    return (...parameters) => {
      const fault = globalThis.__jotFault;
      if (name === 'jot_create' && fault?.kind === 'create') {
        globalThis.__jotFault = undefined;
        return 0;
      }
      if (name === 'jot_execute' && fault) {
        const command = JSON.parse(parameters[1]);
        if (command.op === fault.op) {
          globalThis.__jotFault = undefined;
          if (fault.kind === 'malformed') return fault.payload ?? '{"ok":true}';
          if (fault.kind === 'missing') return '{"ok":true,"state":{}}';
          if (fault.kind === 'throw') throw new Error('Simulated C ABI failure');
          return JSON.stringify({ ok: false, message: 'Simulated C ABI rejection', state: {} });
        }
      }
      return actual(...parameters);
    };
  };
  return module;
}
