'use strict';
// The compiler output is evaluated in Node, with the same JS promise boundary
// used by the browser. c8 remaps the emitted source map to the authored Dart.
globalThis.testBridgeMode = 'normal';
globalThis.jotBridge = {
  create(dictionary, seed, robot) {
    if (dictionary !== 'AT') throw new Error('unexpected dictionary');
    if (!Number.isInteger(seed) || !Number.isInteger(robot)) throw new Error('invalid numeric ABI');
    return Promise.resolve(globalThis.testBridgeMode === 'zero' ? 0 : 7);
  },
  execute(handle, command) {
    if (handle !== 7) throw new Error('wrong handle');
    return Promise.resolve('response:' + command);
  },
  close(handle) {
    if (handle !== 7) throw new Error('wrong handle');
    return globalThis.testBridgeMode === 'close failure' ?
      Promise.reject(new Error('close failed')) : Promise.resolve(null);
  },
};
// dart2js uses these standard host hooks when executing an async main in Node.
globalThis.self = globalThis;
require('../build/test-web-bridge/bridge_test.js');
