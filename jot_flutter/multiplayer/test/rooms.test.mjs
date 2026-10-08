import assert from 'node:assert/strict';
import { randomUUID, randomBytes } from 'node:crypto';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { runtime } from './runtime.mjs';

const origin = 'https://jot-game.pages.dev';
let address = 0;
async function fixture() {
  const directory = await mkdtemp(join(tmpdir(), 'jot-multiplayer-test-'));
  const mf = runtime(directory);
  try { await mf.ready; }
  catch (error) { await mf.dispose(); await rm(directory, { recursive: true, force: true }); throw error; }
  const ip = `192.0.2.${++address}`;
  const call = async (path, body, bearer, expected = 200, headers = {}) => {
    const res = await mf.dispatchFetch(`${origin}${path}`, {
      method: body === undefined ? 'GET' : 'POST',
      headers: { 'CF-Connecting-IP': ip, Origin: origin,
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}), ...headers },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    const data = await res.json();
    assert.equal(res.status, expected, JSON.stringify(data));
    assert.equal(res.headers.get('Cache-Control'), 'no-store');
    return data;
  };
  const createId = randomUUID();
  const host = await call('/api/rooms', { requestId: createId }, undefined, 201);
  const path = `/api/rooms/${host.roomId}`;
  const joinId = randomUUID();
  const guest = await call(`${path}/join`, { requestId: joinId, inviteToken: host.inviteToken });
  const act = (credential, revision, command, requestId = randomUUID(), expected = 200) =>
    call(`${path}/actions`, { revision, requestId, command }, credential, expected);
  return { mf, directory, host, guest, path, call, act, createId, joinId, ip,
    async close() { await mf.dispose(); await rm(directory, { recursive: true, force: true }); } };
}

test('C++ turns, privacy, hints, safe retries and revisions run inside workerd', async () => {
  const f = await fixture();
  try {
    const { host, guest, path } = f;
    assert.equal(guest.yourPlayer, 1);
    assert.equal(guest.inviteToken, undefined);
    assert.equal(guest.inviteUrl, undefined);
    assert.equal(guest.state.currentPlayer, 0);
    assert.deepEqual(guest.state.rack, []);
    assert.equal(host.state.canShuffle, false);
    const hostRetry = await f.call('/api/rooms', { requestId: f.createId }, undefined, 201);
    assert.equal(hostRetry.playerToken, host.playerToken);
    assert.equal(hostRetry.roomId, host.roomId);
    const guestRetry = await f.call(`${path}/join`, { requestId: f.joinId, inviteToken: host.inviteToken });
    assert.equal(guestRetry.playerToken, guest.playerToken);
    await f.call(`${path}/join`, { requestId: randomUUID(), inviteToken: host.inviteToken }, undefined, 409);
    await f.call(path, undefined, undefined, 401);
    await f.call(path, undefined, randomBytes(32).toString('hex'), 401);
    await f.act(guest.playerToken, guest.revision, { op: 'fill' }, randomUUID(), 403);
    await f.act(host.playerToken, guest.revision, { op: 'restore', save: {} }, randomUUID(), 400);
    let current = await f.act(host.playerToken, guest.revision, { op: 'fill' });
    assert.equal(current.state.rack.length, 8);
    const fillRevision = current.revision;
    const firstHint = await f.act(host.playerToken, current.revision, { op: 'hint', apply: true });
    assert.equal(firstHint.ok, true);
    assert.ok(firstHint.hint.placements.length > 0);
    assert.equal(firstHint.state.preview.valid, true);
    const waiting = await f.call(path, undefined, guest.playerToken);
    assert.deepEqual(waiting.state.rack, []);
    assert.equal(waiting.state.board.filter((tile) => tile?.pending).length, 0);
    assert.equal(waiting.state.preview.valid, false);
    assert.equal(waiting.state.canPlay, false);
    assert.equal(waiting.save, undefined);
    assert.equal(waiting.fullState, undefined);
    assert.equal(waiting.state.players, undefined);
    assert.equal(waiting.state.hint, undefined);
    await f.act(host.playerToken, fillRevision, { op: 'pass' }, randomUUID(), 409);
    const playId = randomUUID();
    const playBody = { revision: firstHint.revision, requestId: playId, command: { op: 'play' } };
    current = await f.call(`${path}/actions`, playBody, host.playerToken);
    assert.equal(current.state.currentPlayer, 1);
    assert.ok(current.state.scores[0] > 0);
    const committedBoard = current.state.board;
    const duplicate = await f.call(`${path}/actions`, playBody, host.playerToken);
    assert.equal(duplicate.revision, current.revision);
    assert.deepEqual(duplicate.state.board, committedBoard);
    let second = await f.call(path, undefined, guest.playerToken);
    second = await f.act(guest.playerToken, second.revision, { op: 'fill' });
    assert.equal(second.state.rack.length, 8);
    second = await f.act(guest.playerToken, second.revision, { op: 'pass' });
    assert.equal(second.state.currentPlayer, 0);
    const laterRetry = await f.call(`${path}/actions`, playBody, host.playerToken);
    assert.equal(laterRetry.revision, second.revision);
    assert.equal(laterRetry.state.currentPlayer, 0);
    assert.deepEqual(laterRetry.state.scores, second.state.scores);
    await f.call(`${path}/actions`, { ...playBody, command: { op: 'pass' } }, host.playerToken, 409);
    // A fresh object restores exclusively from SQL, including rack and board.
    await f.mf.unsafeEvictDurableObject('jot-multiplayer', 'GameRoom', { name: host.roomId });
    const resumed = await f.call(path, undefined, host.playerToken);
    assert.equal(resumed.revision, second.revision);
    assert.deepEqual(resumed.state.board, committedBoard);
    assert.deepEqual(resumed.state.rack, laterRetry.state.rack);
  } finally { await f.close(); }
});

test('simultaneous actions serialize and a repeated action changes the game once', async () => {
  const f = await fixture();
  try {
    const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${f.host.playerToken}`, 'CF-Connecting-IP': f.ip };
    const bodies = [0, 1].map(() => ({ revision: f.guest.revision, requestId: randomUUID(), command: { op: 'fill' } }));
    const results = await Promise.all(bodies.map((body) => f.mf.dispatchFetch(`${origin}${f.path}/actions`, {
      method: 'POST', headers, body: JSON.stringify(body),
    })));
    assert.deepEqual(results.map((res) => res.status).sort(), [200, 409]);
    const current = await f.call(f.path, undefined, f.host.playerToken);
    assert.equal(current.state.rack.length, 8);
    const duplicate = { revision: current.revision, requestId: randomUUID(), command: { op: 'pass' } };
    const repeated = await Promise.all([0, 1].map(() => f.mf.dispatchFetch(`${origin}${f.path}/actions`, {
      method: 'POST', headers, body: JSON.stringify(duplicate),
    })));
    const views = await Promise.all(repeated.map((res) => res.json()));
    assert.deepEqual(repeated.map((res) => res.status), [200, 200]);
    assert.equal(views[0].revision, current.revision + 1);
    assert.equal(views[1].revision, current.revision + 1);
    assert.equal(views[0].state.currentPlayer, 1);
  } finally { await f.close(); }
});

test('sessions and game state survive an entire runtime restart; expired rooms reject play', async () => {
  const f = await fixture();
  let mf = f.mf;
  try {
    const current = await f.act(f.host.playerToken, f.guest.revision, { op: 'fill' });
    await mf.dispose();
    mf = runtime(f.directory);
    await mf.ready;
    const resumedResponse = await mf.dispatchFetch(`${origin}${f.path}`, {
      headers: { Authorization: `Bearer ${f.host.playerToken}` },
    });
    assert.equal(resumedResponse.status, 200);
    const resumed = await resumedResponse.json();
    assert.deepEqual(resumed.state.rack, current.state.rack);
    assert.equal(resumed.revision, current.revision);
    const storage = await mf.unsafeGetDurableObjectStorage('jot-multiplayer', 'GameRoom', { name: f.host.roomId });
    const rows = await storage.exec('SELECT metadata FROM room WHERE singleton = 1');
    const meta = JSON.parse(rows[0].metadata);
    meta.expiresAt = Date.now() - 1;
    await storage.exec('UPDATE room SET metadata = ? WHERE singleton = 1', JSON.stringify(meta));
    const expired = await mf.dispatchFetch(`${origin}${f.path}`, { headers: { Authorization: `Bearer ${f.host.playerToken}` } });
    assert.equal(expired.status, 410);
  } finally { await mf.dispose(); await rm(f.directory, { recursive: true, force: true }); }
});

test('rejects malformed input, oversize bodies and foreign origins; creation limits cannot be bypassed with random tokens', async () => {
  const f = await fixture();
  try {
    await f.call('/api/rooms', { requestId: 'not-random' }, undefined, 400);
    await f.call('/api/rooms', { requestId: randomUUID(), pad: 'x'.repeat(4096) }, undefined, 413);
    await f.call(f.path, undefined, f.host.playerToken, 403, { Origin: 'https://foreign.example' });
    await f.act(f.host.playerToken, f.guest.revision, { op: 'place', rack: 0, row: 999, col: 0, letter: 'A' }, randomUUID(), 400);
    for (let i = 0; i < 7; i += 1) await f.call('/api/rooms', { requestId: randomUUID() }, randomBytes(32).toString('hex'), 201);
    await f.call('/api/rooms', { requestId: randomUUID() }, randomBytes(32).toString('hex'), 429);
    const missingId = randomBytes(16).toString('hex');
    await f.call(`/api/rooms/${missingId}`, undefined, f.host.playerToken, 404);
    const storage = await f.mf.unsafeGetDurableObjectStorage('jot-multiplayer', 'GameRoom', { name: missingId });
    const tables = await storage.exec("SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('room', 'receipts')");
    assert.deepEqual(tables, []);
  } finally { await f.close(); }
});
