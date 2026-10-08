#!/usr/bin/env node
'use strict';

// Exercise the production browser transport with controlled worker events. This
// covers failures that are hard to reproduce reliably with a running engine.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../web/jot_bridge.js'), 'utf8');

function environment() {
  const workers = [];
  class FakeWorker {
    constructor(url) {
      this.url = String(url);
      this.messages = [];
      this.terminations = 0;
      this.postFailure = null;
      workers.push(this);
    }
    postMessage(message) {
      if (this.postFailure) throw this.postFailure;
      // Copy as a real Worker does, and make cross-realm assertions ordinary.
      this.messages.push(JSON.parse(JSON.stringify(message)));
    }
    terminate() { this.terminations += 1; }
    reply(id, result = null, error) {
      this.onmessage({ data: { id, result, ...(error ? { error } : {}) } });
    }
    fatal(message) { this.onmessage({ data: { fatal: true, error: message } }); }
    error(message) { this.onerror({ message }); }
    unreadable() { this.onmessageerror({}); }
  }
  const window = {};
  const context = vm.createContext({
    window, document: { baseURI: 'https://example.test/games/jot/' },
    URL, Worker: FakeWorker,
  });
  vm.runInContext(source, context, { filename: path.resolve(__dirname, '../web/jot_bridge.js') });
  return { bridge: window.jotBridge, workers };
}

async function started() {
  const session = environment();
  const opening = session.bridge.create('AT\nCAT\n', 123, 1);
  const worker = session.workers[0];
  assert.equal(worker.url, 'https://example.test/games/jot/jot_worker.js');
  assert.deepEqual(worker.messages[0], {
    id: 1, op: 'create', dictionary: 'AT\nCAT\n', seed: 123, robot: 1,
  });
  worker.reply(1);
  const handle = await opening;
  return { ...session, worker, handle };
}

async function closeNormally(session) {
  const closing = session.bridge.close(session.handle);
  const last = session.worker.messages.at(-1);
  assert.equal(last.op, 'close');
  session.worker.reply(last.id);
  await closing;
}

test('initialization errors reject creation and terminate the worker', async () => {
  for (const failure of ['error event', 'error response', 'fatal abort']) {
    const { bridge, workers } = environment();
    const opening = bridge.create('AT\n', 1, 1);
    const rejected = assert.rejects(opening, /missing engine/);
    const worker = workers[0];
    if (failure === 'error event') worker.error('missing engine');
    else if (failure === 'error response') worker.reply(1, null, 'missing engine');
    else worker.fatal('missing engine');
    await rejected;
    assert.equal(worker.terminations, 1, failure);
  }
});

test('fatal errors reject pending and future requests without posting to a dead worker', async () => {
  for (const failure of ['error event', 'fatal abort', 'unreadable message']) {
    const session = await started();
    const { bridge, worker, handle } = session;
    const first = bridge.execute(handle, '{"op":"state"}');
    const second = bridge.execute(handle, '{"op":"fill"}');
    const message = failure === 'unreadable message' ? /unreadable data/ : /worker crashed/;
    const pendingRejected = Promise.all([
      assert.rejects(first, message), assert.rejects(second, message),
    ]);
    if (failure === 'error event') worker.error('worker crashed');
    else if (failure === 'fatal abort') worker.fatal('worker crashed');
    else worker.unreadable();
    await pendingRejected;
    const messageCount = worker.messages.length;
    await assert.rejects(bridge.execute(handle, '{"op":"state"}'), message);
    assert.equal(worker.messages.length, messageCount);
    await assert.rejects(bridge.close(handle), message);
    assert.equal(worker.messages.length, messageCount);
    assert.ok(worker.terminations >= 1);
    await bridge.close(handle); // The failed engine has been removed.
    await assert.rejects(bridge.execute(handle, '{}'), /Engine is closed/);
  }
});

test('concurrent close calls share completion and reject new commands during shutdown', async () => {
  const session = await started();
  const { bridge, worker, handle } = session;
  const outstanding = bridge.execute(handle, '{"op":"state"}');
  const executingId = worker.messages.at(-1).id;
  const firstClose = bridge.close(handle);
  const secondClose = bridge.close(handle);
  assert.equal(firstClose, secondClose);
  const closeId = worker.messages.at(-1).id;
  assert.equal(worker.messages.filter((message) => message.op === 'close').length, 1);
  const messageCount = worker.messages.length;
  await assert.rejects(bridge.execute(handle, '{"op":"fill"}'), /Engine is closed/);
  assert.equal(worker.messages.length, messageCount);
  assert.equal(worker.terminations, 0);
  worker.reply(executingId, '{"ok":true,"message":"state"}');
  assert.equal(await outstanding, '{"ok":true,"message":"state"}');
  worker.reply(closeId);
  await Promise.all([firstClose, secondClose]);
  assert.equal(worker.terminations, 1);
  await bridge.close(handle);
  assert.equal(worker.terminations, 1);
  await assert.rejects(bridge.execute(handle, '{}'), /Engine is closed/);
});

test('requests dispatch in FIFO order and responses are matched to their request IDs', async () => {
  const session = await started();
  const { bridge, worker, handle } = session;
  const first = bridge.execute(handle, '{"op":"state"}');
  const second = bridge.execute(handle, '{"op":"fill"}');
  const third = bridge.execute(handle, '{"op":"hint"}');
  const posted = worker.messages.slice(1);
  assert.deepEqual(posted.map((message) => message.command), [
    '{"op":"state"}', '{"op":"fill"}', '{"op":"hint"}',
  ]);
  assert.deepEqual(posted.map((message) => message.id), [2, 3, 4]);
  // Deliver replies out of order to detect accidental use of a shared callback.
  // Unknown or duplicate IDs must be harmless.
  worker.reply(999, 'unrelated');
  worker.reply(posted[1].id, 'filled');
  worker.reply(posted[0].id, 'original state');
  worker.reply(posted[0].id, 'duplicate');
  worker.reply(posted[2].id, 'legal hint');
  assert.deepEqual(await Promise.all([first, second, third]), [
    'original state', 'filled', 'legal hint',
  ]);
  await closeNormally(session);
});

test('postMessage failures reject all requests and persist as terminal failures', async () => {
  const { bridge, worker, handle } = await started();
  const pending = bridge.execute(handle, '{"op":"state"}');
  const pendingRejected = assert.rejects(pending, /worker unavailable/);
  worker.postFailure = new Error('worker unavailable');
  await assert.rejects(bridge.execute(handle, '{"op":"fill"}'), /worker unavailable/);
  await pendingRejected;
  assert.equal(worker.terminations, 1);
  await assert.rejects(bridge.execute(handle, '{}'), /worker unavailable/);
  await assert.rejects(bridge.close(handle), /worker unavailable/);
});

test('a command-level error rejects only that command and leaves the engine usable', async () => {
  const session = await started();
  const { bridge, worker, handle } = session;
  const invalid = bridge.execute(handle, 'invalid command');
  const rejected = assert.rejects(invalid, /bad command/);
  worker.reply(worker.messages.at(-1).id, null, 'bad command');
  await rejected;
  const next = bridge.execute(handle, '{"op":"state"}');
  worker.reply(worker.messages.at(-1).id, '{"ok":true}');
  assert.equal(await next, '{"ok":true}');
  assert.equal(worker.terminations, 0);
  await closeNormally(session);
});

test('empty worker errors use readable defaults and repeated fatal events are harmless', async () => {
  for (const event of ['error', 'fatal']) {
    const { bridge, workers } = environment();
    const opening = bridge.create('AT\n', 1, 1);
    const rejected = assert.rejects(opening, event === 'error' ? /failed to start/ : /worker failed/);
    const worker = workers[0];
    if (event === 'error') worker.error('');
    else worker.fatal('');
    worker.fatal('second failure');
    await rejected;
    assert.equal(worker.terminations, 1);
  }
});

test('each created game gets an independent handle and worker', async () => {
  const { bridge, workers } = environment();
  const first = bridge.create('AT\n', 1, -1);
  const second = bridge.create('CAT\n', 2, 1);
  workers[0].reply(1);
  workers[1].reply(1);
  const handles = await Promise.all([first, second]);
  assert.notEqual(handles[0], handles[1]);
  const request = bridge.execute(handles[1], 'state');
  workers[1].reply(2, 'second game');
  assert.equal(await request, 'second game');
  assert.equal(workers[0].messages.length, 1);
  const closes = handles.map((handle) => bridge.close(handle));
  workers.forEach((worker) => worker.reply(worker.messages.at(-1).id));
  await Promise.all(closes);
});
