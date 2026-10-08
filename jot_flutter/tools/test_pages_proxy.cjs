#!/usr/bin/env node
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const filename = path.resolve(__dirname, '../web/_worker.js');
// Evaluate the authored module in a controlled realm, preserving source lines
// for V8 coverage. Only its ESM export syntax is adapted for this test realm.
const source = fs.readFileSync(filename, 'utf8').replace('export default', 'globalThis.proxy =');
const { proxy } = vm.runInNewContext(source + '\n;globalThis', { URL }, { filename });

test('Pages routes API requests to the multiplayer binding without rewriting credentials or body', async () => {
  for (const method of ['GET', 'POST', 'OPTIONS']) {
    const headers = { Authorization: 'Bearer secret', 'Content-Type': 'application/json' };
    const request = new Request('https://jot-game.pages.dev/api/rooms/123/actions?value=1', {
      method, headers, ...(method === 'POST' ? { body: '{"revision":7}' } : {}),
    });
    const response = new Response('private view', { status: 409 });
    let forwarded;
    const result = await proxy.fetch(request, {
      MULTIPLAYER: { fetch(value) { forwarded = value; return response; } },
      ASSETS: { fetch() { assert.fail('API must not reach assets'); } },
    });
    assert.equal(forwarded, request);
    assert.equal(result, response);
    assert.equal(result.status, 409);
  }
});

test('Pages routes every non-API path to its static asset binding', async () => {
  for (const pathname of ['/', '/main.dart.js', '/jot_core.wasm', '/api', '/api-other/test']) {
    const request = new Request('https://jot-game.pages.dev' + pathname);
    const response = new Response('asset');
    let forwarded;
    const result = await proxy.fetch(request, {
      MULTIPLAYER: { fetch() { assert.fail('Assets must not reach multiplayer'); } },
      ASSETS: { fetch(value) { forwarded = value; return response; } },
    });
    assert.equal(forwarded, request);
    assert.equal(result, response);
  }
});

test('Pages preserves binding rejections for the platform error handler', async () => {
  for (const pathname of ['/api/rooms', '/']) {
    const failure = new Error('binding unavailable');
    const rejecting = { fetch() { return Promise.reject(failure); } };
    await assert.rejects(proxy.fetch(new Request('https://example.test' + pathname), {
      MULTIPLAYER: rejecting, ASSETS: rejecting,
    }), (error) => error === failure);
  }
});
