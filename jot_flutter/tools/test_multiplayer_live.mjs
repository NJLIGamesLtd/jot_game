// Exercise two independent HTTP clients against a local or deployed API.
// Credentials stay in memory; output contains only verification results.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const base = process.argv[2];
if (!base) throw new Error('Usage: node tools/test_multiplayer_live.mjs API_ORIGIN');
async function call(path, body, token, expected = 200) {
  const response = await fetch(new URL(path, base), {
    method: body ? 'POST' : 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  const data = await response.json();
  assert.equal(response.status, expected, data.error ?? 'Unexpected API status');
  assert.equal(response.headers.get('cache-control')?.includes('no-store'), true);
  assert.equal(JSON.stringify(data).includes('fullState'), false, 'Private save leaked');
  return data;
}

const createId = randomUUID();
const host = await call('/api/rooms', { requestId: createId }, null, 201);
const retriedHost = await call('/api/rooms', { requestId: createId }, null, 201);
assert.equal(retriedHost.roomId, host.roomId);
assert.equal(retriedHost.playerToken, host.playerToken);
assert.equal(host.yourPlayer, 0);
const path = `/api/rooms/${host.roomId}`;
const joinId = randomUUID();
const guest = await call(`${path}/join`, {
  inviteToken: host.inviteToken, requestId: joinId,
});
const retriedGuest = await call(`${path}/join`, {
  inviteToken: host.inviteToken, requestId: joinId,
});
assert.equal(guest.yourPlayer, 1);
assert.equal(retriedGuest.playerToken, guest.playerToken);
await call(`${path}/join`, { inviteToken: host.inviteToken, requestId: randomUUID() }, null, 409);
await call(path, null, null, 401);
let view = await call(path, null, host.playerToken);
assert.equal(view.joined, true);
await call(`${path}/actions`, {
  revision: view.revision, requestId: randomUUID(), command: { op: 'fill' },
}, guest.playerToken, 403);
await call(`${path}/actions`, {
  revision: view.revision, requestId: randomUUID(), command: { op: 'restore', save: {} },
}, host.playerToken, 400);
async function act(command, token = host.playerToken) {
  const payload = { revision: view.revision, requestId: randomUUID(), command };
  view = await call(`${path}/actions`, payload, token);
  return payload;
}
await act({ op: 'fill' });
assert.equal(view.state.rack.length, 8);
let other = await call(path, null, guest.playerToken);
assert.equal(other.state.rack.length, 0, 'Opponent rack leaked');
assert.equal(other.state.canPlay, false);
await call(`${path}/actions`, {
  revision: 0, requestId: randomUUID(), command: { op: 'pass' },
}, host.playerToken, 409);
await act({ op: 'hint', apply: true });
assert.equal(view.state.canPlay, true, 'Expected a legal first word');
assert.equal(view.state.board.some(tile => tile?.pending === true), true);
other = await call(path, null, guest.playerToken);
assert.equal(other.state.board.some(tile => tile?.pending === true), false, 'Draft leaked');
const playRequest = await act({ op: 'play' });
const committedRevision = view.revision;
const score = view.state.scores[0];
assert.equal(view.state.currentPlayer, 1);
const duplicate = await call(`${path}/actions`, playRequest, host.playerToken);
assert.equal(duplicate.revision, committedRevision);
assert.equal(duplicate.state.scores[0], score);
view = await call(path, null, guest.playerToken);
assert.equal(view.state.currentPlayer, 1);
assert.equal(view.state.board.some(tile => tile != null), true);
assert.equal(view.state.rack.length, 0);
await act({ op: 'fill' }, guest.playerToken);
assert.equal(view.state.rack.length, 8);
await act({ op: 'pass' }, guest.playerToken);
view = await call(path, null, host.playerToken);
await act({ op: 'pass' });
assert.equal(view.state.over, true);
other = await call(path, null, guest.playerToken);
assert.deepEqual(other.state.scores, view.state.scores);
assert.equal(other.state.over, true);
console.log('PASS: create/join retries, two seats, auth, turn checks, revisions, private racks/drafts, C++ scoring, duplicate play, both turns, resume reads and game completion.');
