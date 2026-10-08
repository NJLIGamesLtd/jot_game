// Run after `python3 tools/build_server_core.py` with Node's built-in test runner.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';
import createJotModule from '../../../multiplayer/generated/jot_server_core.mjs';

const compiled = new WebAssembly.Module(fs.readFileSync(
  new URL('../../../multiplayer/generated/jot_server_core.wasm', import.meta.url)));
const dictionary = fs.readFileSync(
  new URL('../../../multiplayer/generated/jot_dictionary.txt', import.meta.url), 'utf8');

test('server Wasm uses a precompiled module, private views, and bounded shared lexicon memory', async () => {
  // Byte compilation is allowed only here in the Node build test. Once the
  // precompiled module exists, enforce the same restriction as Workers.
  const unavailable = ['Module', 'compile', 'compileStreaming', 'instantiate', 'instantiateStreaming'];
  const original = new Map(unavailable.map(name => [name, WebAssembly[name]]));
  for (const name of unavailable) WebAssembly[name] = () => {
    throw new Error(`Dynamic WebAssembly.${name} was called by the server runtime.`);
  };
  let memory;
  let module;
  try {
    module = await createJotModule({
      instantiateWasm(imports, receiveInstance) {
        const instance = new WebAssembly.Instance(compiled, imports);
        memory = Object.values(instance.exports).find(value => value instanceof WebAssembly.Memory);
        receiveInstance(instance, compiled);
        return instance.exports;
      },
    });
  } finally {
    for (const [name, value] of original) WebAssembly[name] = value;
  }
  const create = module.cwrap('jot_create', 'number', ['string', 'number', 'number']);
  const execute = module.cwrap('jot_execute', 'string', ['number', 'string']);
  const destroy = module.cwrap('jot_destroy', null, ['number']);
  const command = (handle, value) => JSON.parse(execute(handle, JSON.stringify(value)));
  let handle = create(dictionary, 123, -1);
  assert(handle);
  let save;
  try {
    assert.equal(command(handle, {op: 'view', player: 0}).state.dictionaryWords, 143722);
    assert(command(handle, {op: 'fill'}).ok);
    assert(command(handle, {op: 'hint', apply: true}).ok);
    assert.equal(command(handle, {op: 'view', player: 0}).state.preview.valid, true);
    const waiting = command(handle, {op: 'view', player: 1}).state;
    assert.equal(waiting.rack.length, 0);
    assert(waiting.board.every(tile => tile === null));
    assert.equal(waiting.preview.score, 0);
    assert.equal(waiting.isMyTurn, false);
    save = command(handle, {op: 'save'}).save;
    assert(command(handle, {op: 'play'}).ok);
    const ownRack = command(handle, {op: 'view', player: 0}).state.rack;
    assert(ownRack.length < 8);
    assert.equal(command(handle, {op: 'view', player: 1}).state.rack.length, 0);
    assert(command(handle, {op: 'fill'}).ok);
    assert.deepEqual(command(handle, {op: 'view', player: 0}).state.rack, ownRack);
  } finally {
    destroy(handle);
  }
  const allocated = memory.buffer.byteLength;
  for (let index = 0; index < 100; index++) {
    handle = create(dictionary, index + 1, -1);
    assert(handle);
    try {
      assert(command(handle, {op: 'restore', save}).ok);
      assert.equal(command(handle, {op: 'view', player: 0}).state.preview.valid, true);
      assert.equal(command(handle, {op: 'view', player: 1}).state.rack.length, 0);
    } finally {
      destroy(handle);
    }
  }
  assert.equal(memory.buffer.byteLength, allocated);
  assert(allocated < 64 * 1024 * 1024);
});

test('one immutable UTF8 dictionary pointer creates isolated games after memory growth', async () => {
  let memory;
  const module = await createJotModule({
    instantiateWasm(imports, receiveInstance) {
      const instance = new WebAssembly.Instance(compiled, imports);
      memory = Object.values(instance.exports).find(value => value instanceof WebAssembly.Memory);
      receiveInstance(instance, compiled);
      return instance.exports;
    },
  });
  const allocate = module.cwrap('malloc', 'number', ['number']);
  const release = module.cwrap('free', null, ['number']);
  const create = module.cwrap('jot_create', 'number', ['number', 'number', 'number']);
  const execute = module.cwrap('jot_execute', 'string', ['number', 'string']);
  const destroy = module.cwrap('jot_destroy', null, ['number']);
  const bytes = new TextEncoder().encode(dictionary);
  const pointer = allocate(bytes.length + 1);
  assert(pointer);
  new Uint8Array(memory.buffer).set(bytes, pointer);
  new Uint8Array(memory.buffer)[pointer + bytes.length] = 0;
  const handles = [];
  let largeAllocation;
  try {
    const first = create(pointer, 12345, -1);
    assert(first); handles.push(first);
    const command = (handle, value) => JSON.parse(execute(handle, JSON.stringify(value)));
    assert(command(first, { op: 'fill' }).ok);
    const saved = command(first, { op: 'save' }).save;
    const sizeBefore = memory.buffer.byteLength;
    largeAllocation = allocate(sizeBefore);
    assert(largeAllocation);
    assert(memory.buffer.byteLength > sizeBefore);
    assert.deepEqual(new Uint8Array(memory.buffer, pointer, bytes.length), bytes);
    const second = create(pointer, 98765, -1);
    assert(second); handles.push(second);
    assert.equal(command(second, { op: 'state' }).state.dictionaryWords, 143722);
    assert.deepEqual(command(second, { op: 'state' }).state.rack, []);
    assert(command(second, { op: 'restore', save: saved }).ok);
    assert.deepEqual(command(second, { op: 'save' }).save, saved);
    assert(command(second, { op: 'pass' }).ok);
    assert.deepEqual(command(first, { op: 'save' }).save, saved);
  } finally {
    for (const handle of handles) destroy(handle);
    if (largeAllocation) release(largeAllocation);
    release(pointer);
  }
});
