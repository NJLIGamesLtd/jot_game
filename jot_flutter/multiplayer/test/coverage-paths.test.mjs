import assert from 'node:assert/strict';
import { randomUUID, randomBytes, createHash } from 'node:crypto';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { createServer } from 'node:http';
import { runtime } from './runtime.mjs';

const origin = 'https://jot-game.pages.dev';
const covered = { skip: process.env.JOT_COVERAGE !== '1' };
const jsonHeaders = { 'Content-Type': 'application/json' };
const token = () => randomBytes(32).toString('hex');
const digest = (value) => createHash('sha256').update(value).digest('hex');
async function fixture() {
  const directory = await mkdtemp(join(tmpdir(), 'jot-coverage-test-'));
  const mf = runtime(directory);
  await mf.ready;
  const call = async (path, input, expected = 200, credential) => {
    const response = await mf.dispatchFetch(`${origin}${path}`, {
      method: input === undefined ? 'GET' : 'POST',
      headers: { ...jsonHeaders, ...(credential ? { Authorization: `Bearer ${credential}` } : {}) },
      ...(input === undefined ? {} : { body: JSON.stringify(input) }),
    });
    const value = await response.json();
    assert.equal(response.status, expected, JSON.stringify(value));
    return value;
  };
  const invoke = (input, expected) => call('/__test__/invoke', input, expected);
  const create = async () => {
    const requestId = randomUUID();
    const host = await call('/api/rooms', { requestId }, 201);
    return { ...host, requestId, path: `/api/rooms/${host.roomId}` };
  };
  const storage = (id) => mf.unsafeGetDurableObjectStorage('jot-multiplayer', 'GameRoom', { name: id });
  const rpc = (roomId, method, args) => call('/__test__/rpc', { roomId, method, args });
  return { mf, directory, call, invoke, create, storage, rpc,
    async close() { await mf.dispose(); await rm(directory, { recursive: true, force: true }); } };
}

test('server adapter recovers initialization failure, rejects invalid responses and closes every handle', covered, async () => {
  const f = await fixture();
  try {
    await f.call('/__test__/fault', { kind: 'factory' });
    const failed = await f.call('/__test__/core', {}, 500);
    assert.match(failed.error, /initialization failure/);
    assert.equal((await f.call('/__test__/core', { doubleClose: true })).ok, true);
    await f.call('/__test__/fault', { kind: 'create' });
    assert.match((await f.call('/__test__/core', {}, 500)).error, /could not initialize/);
    assert.match((await f.call('/__test__/core', { closeFirst: true }, 500)).error, /closed/);
    await f.call('/__test__/fault', { kind: 'malformed', op: 'state' });
    assert.match((await f.call('/__test__/core', {}, 500)).error, /invalid response/);
    for (const payload of ['null', '{"ok":1,"state":{}}', '{"ok":true,"state":null}', '{"ok":true,"state":2}', '{"ok":true,"state":[]}']) {
      await f.call('/__test__/fault', { kind: 'malformed', op: 'state', payload });
      assert.match((await f.call('/__test__/core', {}, 500)).error, /invalid response/);
    }
    const saved = await f.call('/__test__/core', { command: { op: 'save' } });
    const restored = await f.call('/__test__/core', { save: saved.save });
    assert.deepEqual(restored.state, saved.state);
    assert.match((await f.call('/__test__/core', { save: {} }, 500)).error, /Unable to restore/);
    await f.call('/__test__/fault', { kind: 'throw', op: 'restore' });
    assert.match((await f.call('/__test__/core', { save: saved.save }, 500)).error, /C ABI failure/);
    assert.equal((await f.call('/__test__/core', {})).ok, true);
  } finally { await f.close(); }
});

test('server dictionary allocation is bounded, survives room changes and recovers initialization failures', covered, async () => {
  for (const kind of ['allocation', 'dictionary_write']) {
    const f = await fixture();
    try {
      await f.call('/__test__/fault', { kind });
      const failure = await f.call('/__test__/core', {}, 500);
      assert.match(failure.error, kind === 'allocation' ? /could not be allocated/ : /initialization failure/);
      const failedStats = await f.call('/__test__/allocations', {});
      assert.equal(failedStats.releases, kind === 'allocation' ? 0 : 1);
      const host = await f.create();
      const guest = await f.call(`${host.path}/join`, { requestId: randomUUID(), inviteToken: host.inviteToken });
      const ready = await f.call('/__test__/allocations', {});
      assert.equal(ready.allocations - failedStats.allocations, 1);
      for (let i = 0; i < 10; i++) {
        const view = await f.call(host.path, undefined, 200, i % 2 ? guest.playerToken : host.playerToken);
        assert.equal(view.state.viewerPlayer, i % 2);
      }
      const another = await f.create();
      await f.call(another.path, undefined, 200, another.playerToken);
      const warmed = await f.call('/__test__/allocations', {});
      assert.equal(warmed.allocations, ready.allocations);
      assert.ok(warmed.games > ready.games);
    } finally { await f.close(); }
  }
});

test('request parser bounds streams, validates JSON and returns explicit route and outage errors', covered, async () => {
  const f = await fixture();
  try {
    await f.invoke({ body: '{}' }, 415);
    await f.invoke({ headers: { ...jsonHeaders, 'Content-Length': 'invalid' }, body: '{}' }, 413);
    await f.invoke({ headers: { ...jsonHeaders, 'Content-Length': '4097' }, body: '{}' }, 413);
    await f.invoke({ headers: jsonHeaders }, 400);
    for (const body of ['null', '[]', '{']) await f.invoke({ headers: jsonHeaders, body }, 400);
    await f.invoke({ headers: jsonHeaders, chunks: [[255]] }, 400);
    await f.invoke({ headers: jsonHeaders, chunks: [[123]], streamError: true }, 400);
    await f.invoke({ headers: jsonHeaders, chunks: [Array(4097).fill(32)] }, 413);
    assert.equal((await f.invoke({ method: 'OPTIONS' }, 200)).ok, true);
    assert.equal((await f.invoke({ method: 'GET', url: `${origin}/api/health` }, 200)).service, 'jot-multiplayer');
    await f.invoke({ method: 'GET', url: `${origin}/unknown` }, 404);
    await f.invoke({ rejectIngress: true, headers: jsonHeaders, body: '{}' }, 429);
    await f.invoke({ rejectApi: true, method: 'GET', url: `${origin}/api/rooms/${'f'.repeat(32)}` }, 429);
    await f.invoke({ faultIngress: 'primitive error', headers: jsonHeaders, body: '{}' }, 500);
    await f.invoke({ faultError: true, headers: jsonHeaders, body: '{}' }, 500);
    const host = await f.create();
    await f.call(host.path, undefined, 401, token());
    await f.invoke({ method: 'POST', url: `${origin}${host.path}`, body: '{}',
      headers: { ...jsonHeaders, Authorization: `Bearer ${host.playerToken}` } }, 405);
    await f.invoke({ method: 'GET', url: `${origin}${host.path}/join`,
      headers: { Authorization: `Bearer ${host.playerToken}` } }, 405);
    await f.call(`${host.path}/join`, {}, 400);
    await f.call(`${host.path}/join`, { inviteToken: host.inviteToken, requestId: 'bad' }, 400);
    await f.call(`${host.path}/join`, { inviteToken: 1, requestId: randomUUID() }, 400);
    await f.call(`${host.path}/join`, { inviteToken: 'bad', requestId: randomUUID() }, 400);
    await f.call(`${host.path}/join`, { inviteToken: token(), requestId: randomUUID() }, 403);
    await f.invoke({ method: 'POST', url: `${origin}${host.path}/join`, headers: jsonHeaders }, 400);
    await f.invoke({ method: 'POST', url: `${origin}${host.path}/actions`,
      headers: { ...jsonHeaders, Authorization: `Bearer ${host.playerToken}` } }, 400);
    await f.call(`${host.path}/actions`, { revision: -1, requestId: randomUUID(), command: { op: 'fill' } }, 400, host.playerToken);
    await f.call(`${host.path}/actions`, { revision: host.revision, requestId: randomUUID(), command: { op: 'fill' } }, 401, token());
    await f.call(`${host.path}/actions`, { revision: 0, requestId: 'bad', command: { op: 'fill' } }, 400, host.playerToken);
  } finally { await f.close(); }
});

test('every allowed command reaches C++ and rejected moves remain retryable without changing revisions', covered, async () => {
  const f = await fixture();
  try {
    const host = await f.create();
    const guest = await f.call(`${host.path}/join`, { inviteToken: host.inviteToken, requestId: randomUUID() });
    let view = await f.call(host.path, undefined, 200, host.playerToken);
    const act = async (command, credential = host.playerToken, requestId = randomUUID(), expected = 200) => {
      view = await f.call(`${host.path}/actions`, { revision: view.revision, requestId, command }, expected, credential);
      return view;
    };
    const rejectedId = randomUUID();
    const before = view.revision;
    assert.equal((await act({ op: 'play' }, host.playerToken, rejectedId)).ok, false);
    assert.equal(view.revision, before);
    assert.equal((await act({ op: 'play' }, host.playerToken, rejectedId)).ok, false);
    await act({ op: 'draw', bag: 'vowel' });
    await act({ op: 'draw', bag: 'consonant' });
    await act({ op: 'fill' });
    const letter = view.state.rack[0].letter === '@' ? 'A' : view.state.rack[0].letter;
    await act({ op: 'place', rack: 0, row: 7, col: 7, letter });
    await act({ op: 'unplace', rack: 0 });
    await act({ op: 'place', rack: 0, row: 7, col: 7, letter });
    await act({ op: 'recall' });
    await act({ op: 'shuffle' });
    await act({ op: 'hint' });
    const hintId = randomUUID(); const hintRevision = view.revision;
    await act({ op: 'hint', apply: true }, host.playerToken, hintId);
    const duplicate = await f.call(`${host.path}/actions`, { revision: hintRevision, requestId: hintId, command: { op: 'hint', apply: true } }, 200, host.playerToken);
    assert.deepEqual(duplicate.hint, view.hint);
    await act({ op: 'play' });
    for (let i = 0; i < 6 && view.state.over !== true; i += 1) {
      const credential = view.state.currentPlayer === 0 ? host.playerToken : guest.playerToken;
      await act({ op: 'fill' }, credential);
      await act({ op: 'pass' }, credential);
    }
    assert.equal(view.state.over, true);
    const activeCredential = view.state.currentPlayer === 0 ? host.playerToken : guest.playerToken;
    await act({ op: 'fill' }, activeCredential, randomUUID(), 409);
    const invalidCommands = [null, [], {}, { op: 3 }, { op: 'fill', extra: true }, { op: 'draw', bag: 'invalid' },
      { op: 'unplace', rack: -1 }, { op: 'unplace', rack: 1.2 }, { op: 'place', rack: 0, row: 0, col: 0, letter: 'a' },
      { op: 'place', rack: 0, row: 0, col: 0, letter: 1 }, { op: 'hint', apply: 'yes' }, { op: 'save' }];
    for (const command of invalidCommands) {
      await f.call(`${host.path}/actions`, { revision: view.revision, requestId: randomUUID(), command }, 400, host.playerToken);
    }
  } finally { await f.close(); }
});

test('room TTL refresh, collision checks and cleanup alarms preserve active games and remove inactive games', covered, async () => {
  const f = await fixture();
  try {
    const host = await f.create();
    const collision = await f.rpc(host.roomId, 'create', [host.roomId, randomUUID(), token(), token(), token(), 1]);
    assert.equal(collision.status, 409);
    const noGuest = await f.call(`${host.path}/actions`, { revision: host.revision, requestId: randomUUID(), command: { op: 'fill' } }, 409, host.playerToken);
    assert.equal(noGuest.code, 'waiting_for_player');
    await f.rpc(host.roomId, 'testAlarm', []);
    assert.equal((await f.call(host.path, undefined, 200, host.playerToken)).joined, false);
    const storage = await f.storage(host.roomId);
    const rows = await storage.exec('SELECT metadata FROM room');
    const meta = JSON.parse(rows[0].metadata);
    meta.touchedAt = Date.now() - 2 * 60 * 60 * 1000;
    await storage.exec('UPDATE room SET metadata = ?', JSON.stringify(meta));
    const refreshed = await f.call(host.path, undefined, 200, host.playerToken);
    assert.ok(refreshed.expiresAt > meta.expiresAt);
    const latest = JSON.parse((await storage.exec('SELECT metadata FROM room'))[0].metadata);
    latest.expiresAt = Date.now() - 1;
    await storage.exec('UPDATE room SET metadata = ?', JSON.stringify(latest));
    await f.call(host.path, undefined, 410, host.playerToken);
    await f.call(`${host.path}/join`, { inviteToken: host.inviteToken, requestId: randomUUID() }, 410);
    await f.call(`${host.path}/actions`, { revision: host.revision, requestId: randomUUID(), command: { op: 'fill' } }, 410, host.playerToken);
    await f.call('/api/rooms', { requestId: host.requestId }, 410);
    await f.rpc(host.roomId, 'testAlarm', []);
    assert.deepEqual(await storage.exec("SELECT name FROM sqlite_master WHERE type='table' AND name IN('room','receipts')"), []);
    await f.call(host.path, undefined, 404, host.playerToken);
    await f.rpc(host.roomId, 'testAlarm', []);
    const missing = randomBytes(16).toString('hex');
    const missingJoin = await f.rpc(missing, 'join', [token(), randomUUID(), token(), token()]);
    assert.equal(missingJoin.status, 404);
    const missingAction = await f.rpc(missing, 'act', [digest(host.playerToken), 0, randomUUID(), '{"op":"fill"}']);
    assert.equal(missingAction.status, 404);
  } finally { await f.close(); }
});

test('C ABI failure paths keep committed SQL game state intact and expose no internal save', covered, async () => {
  const f = await fixture();
  try {
    const host = await f.create();
    const guest = await f.call(`${host.path}/join`, { inviteToken: host.inviteToken, requestId: randomUUID() });
    const before = await f.call(host.path, undefined, 200, host.playerToken);
    for (const op of ['view', 'restore']) {
      await f.call('/__test__/fault', { kind: 'reject', op });
      const failed = await f.call(host.path, undefined, 500, host.playerToken);
      assert.equal(failed.code, 'server_error');
      assert.equal(failed.save, undefined);
      assert.deepEqual((await f.call(host.path, undefined, 200, host.playerToken)).state, before.state);
    }
    await f.call('/__test__/fault', { kind: 'reject', op: 'restore' });
    await f.call('/api/rooms', { requestId: host.requestId }, 500);
    const separate = await f.create();
    await f.call('/__test__/fault', { kind: 'reject', op: 'restore' });
    await f.call(`${separate.path}/join`, { requestId: randomUUID(), inviteToken: separate.inviteToken }, 500);
    await f.call('/__test__/fault', { kind: 'reject', op: 'restore' });
    await f.call(`${host.path}/actions`, { revision: before.revision, requestId: randomUUID(), command: { op: 'fill' } }, 500, host.playerToken);
    await f.call('/__test__/fault', { kind: 'reject', op: 'save' });
    await f.call(`${host.path}/actions`, { revision: guest.revision, requestId: randomUUID(), command: { op: 'fill' } }, 500, host.playerToken);
    const after = await f.call(host.path, undefined, 200, host.playerToken);
    assert.equal(after.revision, before.revision);
    assert.deepEqual(after.state.rack, []);
    await f.call('/__test__/fault', { kind: 'reject', op: 'save' });
    const id = randomUUID();
    await f.call('/api/rooms', { requestId: id }, 500);
    await f.call(`/api/rooms/${digest(id).slice(0, 32)}`, undefined, 404, host.playerToken);
    await f.call('/__test__/fault', { kind: 'missing', op: 'save' });
    await f.call(`${host.path}/actions`, { revision: guest.revision, requestId: randomUUID(), command: { op: 'fill' } }, 500, host.playerToken);
    await f.call('/__test__/fault', { kind: 'missing', op: 'save' });
    await f.call('/api/rooms', { requestId: randomUUID() }, 500);
  } finally { await f.close(); }
});

test('post-initialization checks reject a room removed, expired or reauthorized while the C++ runtime loads', covered, async () => {
  const cases = [
    ['get', 'missing', 404], ['get', 'expired', 410], ['get', 'credential', 401],
    ['act', 'missing', 404], ['act', 'expired', 410], ['act', 'credential', 401],
    ['join', 'missing', 404], ['join', 'expired', 410], ['join', 'invitation', 403],
  ];
  for (const [operation, change, expected] of cases) {
    const f = await fixture();
    let pausedRuntime;
    let release;
    let requestArrived;
    const arrived = new Promise((resolve) => { requestArrived = resolve; });
    const server = createServer((_request, response) => {
      release = () => { response.end('continue'); };
      requestArrived();
    });
    await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
    try {
      const host = await f.create();
      if (operation === 'act') await f.call(`${host.path}/join`, { inviteToken: host.inviteToken, requestId: randomUUID() });
      await f.mf.dispose();
      pausedRuntime = runtime(f.directory);
      await pausedRuntime.ready;
      const barrierUrl = `http://127.0.0.1:${server.address().port}/pause`;
      await pausedRuntime.dispatchFetch(`${origin}/__test__/fault`, {
        method: 'POST', headers: jsonHeaders, body: JSON.stringify({ kind: 'pause', url: barrierUrl }),
      });
      const args = operation === 'get' ? [digest(host.playerToken)]
        : operation === 'act' ? [digest(host.playerToken), 1, randomUUID(), '{"op":"fill"}']
        : [host.inviteToken, randomUUID(), token(), token()];
      const pending = pausedRuntime.dispatchFetch(`${origin}/__test__/rpc`, {
        method: 'POST', headers: jsonHeaders, body: JSON.stringify({ roomId: host.roomId, method: operation, args }),
      });
      await arrived;
      const storage = await pausedRuntime.unsafeGetDurableObjectStorage('jot-multiplayer', 'GameRoom', { name: host.roomId });
      if (change === 'missing') await storage.exec('DELETE FROM room');
      else {
        const meta = JSON.parse((await storage.exec('SELECT metadata FROM room'))[0].metadata);
        if (change === 'expired') meta.expiresAt = Date.now() - 1;
        if (change === 'credential') meta.hostHash = token();
        if (change === 'invitation') meta.inviteToken = token();
        await storage.exec('UPDATE room SET metadata = ?', JSON.stringify(meta));
      }
      release();
      const response = await pending;
      const result = await response.json();
      assert.equal(result.status, expected, `${operation}/${change}: ${JSON.stringify(result)}`);
      if (change === 'missing') {
        // Recovery from an empty table uses the existing schema without
        // recreating it, exercising the orphan-table recovery guard.
        const recovered = await pausedRuntime.dispatchFetch(`${origin}/api/rooms`, {
          method: 'POST', headers: jsonHeaders, body: JSON.stringify({ requestId: host.requestId }),
        });
        assert.equal(recovered.status, 201);
      }
    } finally {
      release?.();
      await pausedRuntime?.dispose();
      await f.close();
      await new Promise((resolve) => server.close(resolve));
    }
  }
});
