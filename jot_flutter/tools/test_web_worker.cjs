#!/usr/bin/env node
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const filename = path.resolve(__dirname, '../web/jot_worker.js');
const source = fs.readFileSync(filename, 'utf8');

function environment(options = {}) {
  const messages = [];
  const calls = [];
  let configuration;
  let postFailures = options.postFailures ?? 0;
  const module = {
    cwrap(name, result, arguments_) {
      calls.push([name, result, arguments_]);
      if (name === 'jot_create') {
        return (...args) => {
          calls.push(['create', ...args]);
          return options.handle ?? 42;
        };
      }
      return (handle, command) => {
        calls.push(['execute', handle, command]);
        if (options.executeFailure) throw options.executeFailure;
        return JSON.stringify({ ok: true, command });
      };
    },
    _jot_destroy(handle) {
      calls.push(['destroy', handle]);
      if (options.destroyFailure) throw options.destroyFailure;
    },
  };
  const self = {
    postMessage(value) {
      if (postFailures > 0) {
        postFailures -= 1;
        throw options.postFailure ?? new Error('message channel failed');
      }
      messages.push(JSON.parse(JSON.stringify(value)));
    },
  };
  const context = vm.createContext({
    self,
    importScripts(url) { assert.equal(url, 'jot_core.js'); },
    async createJotModule(config) {
      configuration = config;
      if (options.loading) await options.loading;
      if (options.createFailure) throw options.createFailure;
      return module;
    },
  });
  vm.runInContext(source, context, { filename });
  return {
    calls, messages,
    send(data) { self.onmessage({ data }); },
    settle() { return vm.runInContext('queue', context); },
    abort(reason) { configuration.onAbort(reason); },
  };
}

test('real worker protocol preserves FIFO through asynchronous WASM startup and closes once', async () => {
  let finishLoading;
  const loading = new Promise((resolve) => { finishLoading = resolve; });
  const env = environment({ loading });
  env.send({ id: 1, op: 'create', dictionary: 'AT\n', seed: 123, robot: -1 });
  env.send({ id: 2, op: 'execute', command: '{"op":"fill"}' });
  env.send({ id: 3, op: 'execute', command: '{"op":"hint"}' });
  env.send({ id: 4, op: 'close' });
  env.send({ id: 5, op: 'close' });
  await Promise.resolve();
  assert.deepEqual(env.messages, []);
  finishLoading();
  await env.settle();
  assert.deepEqual(env.messages.map((reply) => reply.id), [1, 2, 3, 4, 5]);
  assert.deepEqual(env.calls.filter((call) => call[0] === 'execute'), [
    ['execute', 42, '{"op":"fill"}'], ['execute', 42, '{"op":"hint"}'],
  ]);
  assert.deepEqual(env.calls.filter((call) => call[0] === 'destroy'), [['destroy', 42]]);
  assert.equal(JSON.parse(env.messages[1].result).command, '{"op":"fill"}');
  assert.equal(env.messages[4].result, null);
});

test('invalid operations and commands return scoped errors while later requests continue', async () => {
  const env = environment();
  env.send({ id: 1, op: 'execute', command: 'early' });
  env.send({ id: 2, op: 'invalid' });
  env.send({ id: 3, op: 'create', dictionary: 'AT\n', seed: 1, robot: 1 });
  env.send({ id: 4, op: 'create' });
  env.send({ id: 5, op: 'execute', command: 'state' });
  env.send({ id: 6, op: 'close' });
  env.send({ id: 7, op: 'execute', command: 'late' });
  await env.settle();
  assert.match(env.messages[0].error, /closed/);
  assert.match(env.messages[1].error, /Unknown worker/);
  assert.match(env.messages[3].error, /already created/);
  assert.equal(JSON.parse(env.messages[4].result).ok, true);
  assert.match(env.messages[6].error, /closed/);
});

test('failed WASM creation and command execution produce readable Error and non-Error replies', async () => {
  for (const options of [
    { handle: 0 },
    { createFailure: new Error('loading failed') },
    { createFailure: 'loading failed without Error' },
    { executeFailure: new Error('execution failed') },
    { executeFailure: 'execution failed without Error' },
    { destroyFailure: new Error('destroy failed') },
  ]) {
    const env = environment(options);
    env.send({ id: 1, op: 'create', dictionary: '', seed: 1, robot: 1 });
    env.send({ id: 2, op: 'execute', command: 'state' });
    env.send({ id: 3, op: 'close' });
    await env.settle();
    const failure = options.executeFailure ? env.messages[1] :
      options.destroyFailure ? env.messages[2] : env.messages[0];
    assert.equal(typeof failure.error, 'string');
    assert.match(failure.error, /failed|could not load/);
    assert.deepEqual(env.messages.map((reply) => reply.id), [1, 2, 3]);
  }
});

test('a WASM abort is terminal and prevents further native calls', async () => {
  const env = environment();
  env.send({ id: 1, op: 'create', dictionary: 'AT\n', seed: 1, robot: 1 });
  await env.settle();
  env.abort(123);
  assert.deepEqual(env.messages.at(-1), { fatal: true, error: '123' });
  env.send({ id: 2, op: 'execute', command: 'state' });
  await env.settle();
  assert.match(env.messages.at(-1).error, /unavailable/);
  assert.equal(env.calls.filter((call) => call[0] === 'execute').length, 0);
});

test('broken reply channels reject the queue with a terminal fatal message', async () => {
  for (const postFailure of [new Error('broken channel'), 'broken channel']) {
    const env = environment({ postFailures: 2, postFailure });
    env.send({ id: 1, op: 'close' });
    await env.settle();
    assert.deepEqual(env.messages, [{ fatal: true, error: 'broken channel' }]);
    env.send({ id: 2, op: 'execute', command: 'state' });
    await env.settle();
    assert.match(env.messages.at(-1).error, /unavailable/);
  }
});
