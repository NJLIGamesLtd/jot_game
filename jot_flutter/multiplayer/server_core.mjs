import createJotModule from './generated/jot_server_core.mjs';
import wasmModule from './generated/jot_server_core.wasm';
import dictionary from './generated/jot_dictionary.txt';

// This runtime and its lexicon are shared immutable infrastructure. All mutable
// game state belongs to a temporary handle created for one room operation.
let runtimePromise;

async function runtime() {
  if (!runtimePromise) {
    runtimePromise = Promise.resolve().then(async () => {
      let memory;
      const module = await createJotModule({
        // Workers supplies a precompiled WebAssembly.Module at this import.
        // Never fetch or compile Wasm bytes dynamically in a request handler.
        instantiateWasm(imports, receiveInstance) {
          const instance = new WebAssembly.Instance(wasmModule, imports);
          memory = Object.values(instance.exports).find((value) => value instanceof WebAssembly.Memory);
          receiveInstance(instance, wasmModule);
          return instance.exports;
        },
      });
      const create = module.cwrap('jot_create', 'number', ['number', 'number', 'number']);
      const allocate = module.cwrap('malloc', 'number', ['number']);
      const release = module.cwrap('free', null, ['number']);
      const bytes = new TextEncoder().encode(dictionary);
      const dictionaryPointer = allocate(bytes.length + 1);
      if (!dictionaryPointer) throw new Error('The server dictionary could not be allocated.');
      try {
        // One immutable UTF8 allocation per runtime avoids encoding the entire
        // dictionary and copying it through cwrap's stack on every room read.
        // Numeric pointers remain valid when WebAssembly grows its memory.
        const heap = new Uint8Array(memory.buffer);
        heap.set(bytes, dictionaryPointer);
        heap[dictionaryPointer + bytes.length] = 0;
        return {
          dictionaryPointer, create,
          execute: module.cwrap('jot_execute', 'string', ['number', 'string']),
          destroy: module.cwrap('jot_destroy', null, ['number']),
        };
      } catch (error) {
        release(dictionaryPointer);
        throw error;
      }
    }).catch((error) => {
      runtimePromise = undefined;
      throw error;
    });
  }
  return runtimePromise;
}

export async function createGame(seed, save) {
  const api = await runtime();
  let handle = api.create(api.dictionaryPointer, seed, -1);
  if (!handle) throw new Error('The server game engine could not initialize.');
  const game = {
    execute(command) {
      if (!handle) throw new Error('This server game handle is closed.');
      // Parsing copies the borrowed C response before any subsequent execute.
      const result = JSON.parse(api.execute(handle, JSON.stringify(command)));
      if (!result || typeof result.ok !== 'boolean' || !result.state ||
          typeof result.state !== 'object' || Array.isArray(result.state)) {
        throw new Error('The server game engine returned an invalid response.');
      }
      return result;
    },
    close() {
      if (handle) api.destroy(handle);
      handle = 0;
    },
  };
  if (save !== undefined) {
    try {
      const result = game.execute({ op: 'restore', save });
      if (!result.ok) throw new Error(`Unable to restore the server game: ${result.message}`);
    } catch (error) {
      game.close();
      throw error;
    }
  }
  return game;
}
