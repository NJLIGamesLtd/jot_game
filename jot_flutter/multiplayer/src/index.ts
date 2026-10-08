import { DurableObject } from 'cloudflare:workers';
import { createGame, type CoreGame, type CoreResult } from './core';

type JsonObject = Record<string, unknown>;
type Reply = { status: number; body: JsonObject };
// The C++ JSON schema is dynamic. Keep the RPC body opaque in TypeScript,
// while its concrete plain object is still transmitted by value at runtime.
type RpcReply = { status: number; body: object };
type Meta = {
  roomId: string;
  createRequestId: string;
  inviteToken: string;
  hostToken: string;
  hostHash: string;
  guestToken?: string;
  guestHash?: string;
  joinRequestId?: string;
  revision: number;
  seed: number;
  touchedAt: number;
  expiresAt: number;
};
type RoomRow = { metadata: string; save: string };
type ReceiptRow = { request_json: string; response_json: string };
const ROOM_TTL = 30 * 24 * 60 * 60 * 1000;
const TOUCH_INTERVAL = 60 * 60 * 1000;
const MAX_BODY = 4096;
const MAX_RECEIPTS = 256;
const TOKEN = /^[a-f0-9]{64}$/;
const REQUEST_ID = /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i;

function failure(status: number, code: string, error: string, extra: JsonObject = {}): Reply {
  return { status, body: { ...extra, ok: false, code, error } };
}
function success(body: JsonObject): Reply { return { status: 200, body: { ok: true, ...body } }; }
function object(value: unknown): value is JsonObject {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}
function exactKeys(value: JsonObject, allowed: string[]): boolean {
  return Object.keys(value).every((key) => allowed.includes(key));
}
function integer(value: unknown, min: number, max: number): boolean {
  return typeof value === 'number' && Number.isInteger(value) && value >= min && value <= max;
}
function requestId(value: unknown): value is string {
  return typeof value === 'string' && REQUEST_ID.test(value);
}
function token(value: unknown): value is string {
  return typeof value === 'string' && TOKEN.test(value);
}
function command(value: unknown): value is JsonObject {
  if (!object(value) || typeof value.op !== 'string') return false;
  switch (value.op) {
    case 'fill': case 'recall': case 'shuffle': case 'play': case 'pass':
      return exactKeys(value, ['op']);
    case 'draw':
      return exactKeys(value, ['op', 'bag']) && (value.bag === 'vowel' || value.bag === 'consonant');
    case 'unplace':
      return exactKeys(value, ['op', 'rack']) && integer(value.rack, 0, 7);
    case 'place':
      return exactKeys(value, ['op', 'rack', 'row', 'col', 'letter']) && integer(value.rack, 0, 7)
        && integer(value.row, 0, 15) && integer(value.col, 0, 15)
        && typeof value.letter === 'string' && /^[A-Z]$/.test(value.letter);
    case 'hint':
      return exactKeys(value, ['op', 'apply']) && (value.apply === undefined || typeof value.apply === 'boolean');
    default: return false;
  }
}
function randomToken(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(32)), (n) => n.toString(16).padStart(2, '0')).join('');
}
async function hash(value: string): Promise<string> {
  return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))),
    (n) => n.toString(16).padStart(2, '0')).join('');
}
function secretEqual(first: string, second: string): boolean {
  const encoder = new TextEncoder();
  const a = encoder.encode(first); const b = encoder.encode(second);
  return a.length === b.length && crypto.subtle.timingSafeEqual(a, b);
}

/** One authoritative persisted C++ game per room. All changes after runtime
 * initialization execute synchronously, so an await cannot interleave a turn. */
export class GameRoom extends DurableObject<Env> {
  private hasSchema = false;
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    this.hasSchema = this.ctx.storage.sql.exec<{ name: string }>(
      "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'room'").toArray().length > 0;
  }
  private ensureSchema(): void {
    if (this.hasSchema) return;
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS room (
        singleton INTEGER PRIMARY KEY CHECK(singleton = 1), metadata TEXT NOT NULL, save TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS receipts (
        sequence INTEGER PRIMARY KEY AUTOINCREMENT, player INTEGER NOT NULL,
        request_id TEXT NOT NULL, request_json TEXT NOT NULL, response_json TEXT NOT NULL,
        UNIQUE(player, request_id)
      );
    `);
    this.hasSchema = true;
  }
  private read(): { meta: Meta; save: JsonObject } | undefined {
    if (!this.hasSchema) return undefined;
    const rows = this.ctx.storage.sql.exec<RoomRow>('SELECT metadata, save FROM room WHERE singleton = 1').toArray();
    if (!rows.length) return undefined;
    return { meta: JSON.parse(rows[0].metadata) as Meta, save: JSON.parse(rows[0].save) as JsonObject };
  }
  private write(meta: Meta, save: unknown): void {
    this.ctx.storage.sql.exec('INSERT OR REPLACE INTO room(singleton, metadata, save) VALUES(1, ?, ?)',
      JSON.stringify(meta), JSON.stringify(save));
  }
  private touch(meta: Meta): void {
    const now = Date.now();
    if (now - meta.touchedAt >= TOUCH_INTERVAL) {
      meta.touchedAt = now;
      meta.expiresAt = now + ROOM_TTL;
      this.ctx.storage.sql.exec('UPDATE room SET metadata = ? WHERE singleton = 1', JSON.stringify(meta));
    }
  }
  private authorize(meta: Meta, bearerHash: string): 0 | 1 | undefined {
    if (secretEqual(meta.hostHash, bearerHash)) return 0;
    if (meta.guestHash && secretEqual(meta.guestHash, bearerHash)) return 1;
    return undefined;
  }
  private snapshot(meta: Meta, engine: CoreGame, player: 0 | 1): JsonObject {
    const view = engine.execute({ op: 'view', player });
    if (!view.ok) throw new Error('C++ viewer projection failed.');
    const state = { ...view.state };
    if (!meta.guestHash) {
      state.canDraw = false; state.canPlay = false; state.canPass = false;
      state.canHint = false; state.canShuffle = false; state.isMyTurn = false;
    }
    return {
      roomId: meta.roomId, yourPlayer: player, revision: meta.revision,
      joined: Boolean(meta.guestHash), expiresAt: meta.expiresAt, state,
      ...(player === 0 ? { inviteToken: meta.inviteToken,
        inviteUrl: `${this.env.PUBLIC_ORIGIN}/?room=${meta.roomId}#invite=${meta.inviteToken}` } : {}),
    };
  }
  private expiry(meta: Meta): Reply | undefined {
    return meta.expiresAt <= Date.now() ? failure(410, 'room_expired', 'This game has expired. Start a new online game.') : undefined;
  }
  // Replies contain only plain data, which RPC transmits by value. Encode JSON
  // once at the HTTP boundary rather than encoding and parsing it between Workers.
  async create(roomId: string, nonce: string, hostToken: string, hostHash: string, inviteToken: string, seed: number): Promise<RpcReply> {
    return this.createRoom(roomId, nonce, hostToken, hostHash, inviteToken, seed);
  }
  async join(inviteToken: string, nonce: string, guestToken: string, guestHash: string): Promise<RpcReply> {
    return this.joinRoom(inviteToken, nonce, guestToken, guestHash);
  }
  async get(bearerHash: string): Promise<RpcReply> {
    return this.getRoom(bearerHash);
  }
  async act(bearerHash: string, revision: number, nonce: string, actionJson: string): Promise<RpcReply> {
    return this.actRoom(bearerHash, revision, nonce, JSON.parse(actionJson) as JsonObject);
  }
  private async createRoom(roomId: string, nonce: string, hostToken: string, hostHash: string, inviteToken: string, seed: number): Promise<Reply> {
    const engine = await createGame(seed);
    try {
      const previous = this.read();
      if (previous) {
        const expired = this.expiry(previous.meta); if (expired) return expired;
        if (!secretEqual(previous.meta.createRequestId, nonce)) return failure(409, 'room_exists', 'This game already exists.');
        const restored = engine.execute({ op: 'restore', save: previous.save });
        if (!restored.ok) throw new Error('C++ saved game restore failed.');
        this.touch(previous.meta);
        return success({ ...this.snapshot(previous.meta, engine, 0), playerToken: previous.meta.hostToken });
      }
      const now = Date.now();
      const meta: Meta = { roomId, createRequestId: nonce, inviteToken, hostToken, hostHash, revision: 0,
        seed, touchedAt: now, expiresAt: now + ROOM_TTL };
      const saved = engine.execute({ op: 'save' });
      if (!saved.ok || !saved.save) throw new Error('C++ game save failed.');
      this.ensureSchema();
      this.write(meta, saved.save);
      const reply = success({ ...this.snapshot(meta, engine, 0), playerToken: hostToken });
      await this.ctx.storage.setAlarm(meta.expiresAt);
      return reply;
    } finally { engine.close(); }
  }
  private async joinRoom(inviteToken: string, nonce: string, guestToken: string, guestHash: string): Promise<Reply> {
    const initial = this.read();
    if (!initial) return failure(404, 'room_missing', 'This game does not exist.');
    const initiallyExpired = this.expiry(initial.meta); if (initiallyExpired) return initiallyExpired;
    if (!secretEqual(initial.meta.inviteToken, inviteToken)) return failure(403, 'invalid_invite', 'This invitation is not valid.');
    const engine = await createGame(0);
    try {
      const room = this.read();
      if (!room) return failure(404, 'room_missing', 'This game does not exist.');
      const { meta, save } = room;
      const expired = this.expiry(meta); if (expired) return expired;
      if (!secretEqual(meta.inviteToken, inviteToken)) return failure(403, 'invalid_invite', 'This invitation is not valid.');
      const restored = engine.execute({ op: 'restore', save });
      if (!restored.ok) throw new Error('C++ saved game restore failed.');
      if (meta.guestHash) {
        if (!meta.joinRequestId || !secretEqual(meta.joinRequestId, nonce)) {
          return failure(409, 'room_full', 'Both seats are taken. Reopen this game in the browser where you joined.');
        }
        this.touch(meta);
        return success({ ...this.snapshot(meta, engine, 1), playerToken: meta.guestToken });
      }
      meta.guestToken = guestToken; meta.guestHash = guestHash; meta.joinRequestId = nonce;
      meta.revision += 1; meta.touchedAt = Date.now(); meta.expiresAt = meta.touchedAt + ROOM_TTL;
      this.write(meta, save);
      return success({ ...this.snapshot(meta, engine, 1), playerToken: guestToken });
    } finally { engine.close(); }
  }
  private async getRoom(bearerHash: string): Promise<Reply> {
    const initial = this.read();
    if (!initial) return failure(404, 'room_missing', 'This game does not exist.');
    const initiallyExpired = this.expiry(initial.meta); if (initiallyExpired) return initiallyExpired;
    if (this.authorize(initial.meta, bearerHash) === undefined) return failure(401, 'invalid_player', 'This player session is not valid.');
    const engine = await createGame(0);
    try {
      const room = this.read();
      if (!room) return failure(404, 'room_missing', 'This game does not exist.');
      const expired = this.expiry(room.meta); if (expired) return expired;
      const player = this.authorize(room.meta, bearerHash);
      if (player === undefined) return failure(401, 'invalid_player', 'This player session is not valid.');
      const restored = engine.execute({ op: 'restore', save: room.save });
      if (!restored.ok) throw new Error('C++ saved game restore failed.');
      this.touch(room.meta);
      return success(this.snapshot(room.meta, engine, player));
    } finally { engine.close(); }
  }
  private async actRoom(bearerHash: string, revision: number, nonce: string, action: JsonObject): Promise<Reply> {
    const initial = this.read();
    if (!initial) return failure(404, 'room_missing', 'This game does not exist.');
    const initiallyExpired = this.expiry(initial.meta); if (initiallyExpired) return initiallyExpired;
    if (this.authorize(initial.meta, bearerHash) === undefined) return failure(401, 'invalid_player', 'This player session is not valid.');
    const engine = await createGame(0);
    try {
      const room = this.read();
      if (!room) return failure(404, 'room_missing', 'This game does not exist.');
      const { meta, save } = room;
      const expired = this.expiry(meta); if (expired) return expired;
      const player = this.authorize(meta, bearerHash);
      if (player === undefined) return failure(401, 'invalid_player', 'This player session is not valid.');
      const restored = engine.execute({ op: 'restore', save });
      if (!restored.ok) throw new Error('C++ saved game restore failed.');
      const requestJson = JSON.stringify({ revision, command: action });
      const receipts = this.ctx.storage.sql.exec<ReceiptRow>(
        'SELECT request_json, response_json FROM receipts WHERE player = ? AND request_id = ?', player, nonce).toArray();
      if (receipts.length) {
        if (receipts[0].request_json !== requestJson) return failure(409, 'request_reused', 'An action identifier cannot be reused for another action.', this.snapshot(meta, engine, player));
        const receipt = JSON.parse(receipts[0].response_json) as JsonObject;
        return success({ ...this.snapshot(meta, engine, player), ok: receipt.ok, message: receipt.message,
          ...(receipt.revision === meta.revision && receipt.hint ? { hint: receipt.hint } : {}) });
      }
      if (revision !== meta.revision) return failure(409, 'stale_revision', 'The game changed. Your view has been updated.', this.snapshot(meta, engine, player));
      if (!meta.guestHash) return failure(409, 'waiting_for_player', 'Waiting for your friend to join.', this.snapshot(meta, engine, player));
      const view = engine.execute({ op: 'view', player });
      if (view.state.currentPlayer !== player) return failure(403, 'not_your_turn', 'Wait for the other player to finish their turn.', this.snapshot(meta, engine, player));
      if (view.state.over === true) return failure(409, 'game_over', 'This game has ended.', this.snapshot(meta, engine, player));
      const result: CoreResult = engine.execute(action);
      const saved = engine.execute({ op: 'save' });
      if (!saved.ok || !saved.save) throw new Error('C++ game save failed.');
      if (result.ok) meta.revision += 1;
      meta.touchedAt = Date.now(); meta.expiresAt = meta.touchedAt + ROOM_TTL;
      const reply = success({ ...this.snapshot(meta, engine, player), ok: result.ok, message: result.message,
        ...(result.hint ? { hint: result.hint } : {}) });
      this.ctx.storage.transactionSync(() => {
        this.write(meta, saved.save);
        this.ctx.storage.sql.exec('INSERT INTO receipts(player, request_id, request_json, response_json) VALUES(?, ?, ?, ?)',
          player, nonce, requestJson, JSON.stringify({ ok: result.ok, message: result.message, hint: result.hint, revision: meta.revision }));
        this.ctx.storage.sql.exec('DELETE FROM receipts WHERE sequence <= (SELECT COALESCE(MAX(sequence), 0) - ? FROM receipts)', MAX_RECEIPTS);
      });
      return reply;
    } finally { engine.close(); }
  }
  async alarm(): Promise<void> {
    const room = this.read();
    if (!room || room.meta.expiresAt <= Date.now()) {
      await this.ctx.storage.deleteAll();
      this.hasSchema = false;
    } else { await this.ctx.storage.setAlarm(room.meta.expiresAt); }
  }
}

async function readBody(request: Request): Promise<Reply> {
  if (!request.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) {
    return failure(415, 'json_required', 'Send application/json.');
  }
  const length = request.headers.get('Content-Length');
  if (length && (!/^\d+$/.test(length) || Number(length) > MAX_BODY)) {
    return failure(413, 'body_too_large', 'The request is too large.');
  }
  const reader = request.body?.getReader();
  if (!reader) return failure(400, 'invalid_json', 'A JSON object is required.');
  const chunks: Uint8Array[] = []; let size = 0;
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      size += value.length;
      if (size > MAX_BODY) { await reader.cancel(); return failure(413, 'body_too_large', 'The request is too large.'); }
      chunks.push(value);
    }
    const bytes = new Uint8Array(size); let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
    const value: unknown = JSON.parse(new TextDecoder('utf-8', { fatal: true, ignoreBOM: false }).decode(bytes));
    return object(value) ? { status: 200, body: value } : failure(400, 'invalid_json', 'A JSON object is required.');
  } catch { return failure(400, 'invalid_json', 'The request is not valid JSON.'); }
}
function bearer(request: Request): string | undefined {
  const match = /^Bearer ([a-f0-9]{64})$/.exec(request.headers.get('Authorization') ?? '');
  return match?.[1];
}
function response(reply: RpcReply, origin?: string): Response {
  return Response.json(reply.body, { status: reply.status, headers: {
    'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer', 'X-Content-Type-Options': 'nosniff',
    ...(origin ? { 'Access-Control-Allow-Origin': origin, 'Vary': 'Origin',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS', 'Access-Control-Allow-Headers': 'Authorization, Content-Type' } : {}),
    ...(reply.status === 429 ? { 'Retry-After': '60' } : {}),
  } });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const origin = request.headers.get('Origin');
    const allowedOrigin = origin === env.PUBLIC_ORIGIN ? origin : undefined;
    if (origin && !allowedOrigin) return response(failure(403, 'origin_forbidden', 'This origin is not allowed.'));
    if (request.method === 'OPTIONS') return response(success({}), allowedOrigin);
    try {
      const url = new URL(request.url);
      if (url.pathname === '/api/health' && request.method === 'GET') return response(success({ service: 'jot-multiplayer' }), allowedOrigin);
      const create = url.pathname === '/api/rooms' && request.method === 'POST';
      const match = /^\/api\/rooms\/([a-f0-9]{32})(?:\/(join|actions))?$/.exec(url.pathname);
      if (!create && !match) return response(failure(404, 'not_found', 'This endpoint does not exist.'), allowedOrigin);
      const playerToken = bearer(request);
      const ingressKey = request.headers.get('CF-Connecting-IP') ?? 'anonymous';
      if (!(await env.INGRESS_LIMITER.limit({ key: ingressKey })).success) return response(failure(429, 'rate_limited', 'Please wait a moment before trying again.'), allowedOrigin);
      const rateKey = !create && playerToken ? await hash(playerToken) : ingressKey;
      const limiter = create ? env.CREATE_LIMITER : env.API_LIMITER;
      if (!(await limiter.limit({ key: rateKey })).success) return response(failure(429, 'rate_limited', 'Please wait a moment before trying again.'), allowedOrigin);
      if (create) {
        const parsed = await readBody(request);
        if (parsed.status !== 200) return response(parsed, allowedOrigin);
        const body = parsed.body;
        if (!exactKeys(body, ['requestId']) || !requestId(body.requestId)) return response(failure(400, 'invalid_create', 'A random UUIDv4 requestId is required.'), allowedOrigin);
        const roomId = (await hash(body.requestId)).slice(0, 32);
        const hostToken = randomToken(); const inviteToken = randomToken();
        const seed = crypto.getRandomValues(new Uint32Array(1))[0];
        const reply = await env.ROOMS.getByName(roomId).create(roomId, body.requestId, hostToken, await hash(hostToken), inviteToken, seed);
        return response({ ...reply, status: reply.status === 200 ? 201 : reply.status }, allowedOrigin);
      }
      const roomId = match![1]; const operation = match![2];
      if (operation === 'join' && request.method === 'POST') {
        const parsed = await readBody(request);
        if (parsed.status !== 200) return response(parsed, allowedOrigin);
        const body = parsed.body;
        if (!exactKeys(body, ['inviteToken', 'requestId']) || !token(body.inviteToken) || !requestId(body.requestId)) return response(failure(400, 'invalid_join', 'The invitation and a random UUIDv4 requestId are required.'), allowedOrigin);
        const guestToken = randomToken();
        return response(await env.ROOMS.getByName(roomId).join(body.inviteToken, body.requestId, guestToken, await hash(guestToken)), allowedOrigin);
      }
      if (!playerToken) return response(failure(401, 'player_required', 'A player session is required.'), allowedOrigin);
      const bearerHash = rateKey;
      if (!operation && request.method === 'GET') return response(await env.ROOMS.getByName(roomId).get(bearerHash), allowedOrigin);
      if (operation === 'actions' && request.method === 'POST') {
        const parsed = await readBody(request);
        if (parsed.status !== 200) return response(parsed, allowedOrigin);
        const body = parsed.body;
        if (!exactKeys(body, ['revision', 'requestId', 'command']) || !integer(body.revision, 0, Number.MAX_SAFE_INTEGER)
          || !requestId(body.requestId) || !command(body.command)) return response(failure(400, 'invalid_action', 'This action is not valid.'), allowedOrigin);
        return response(await env.ROOMS.getByName(roomId).act(bearerHash, body.revision as number, body.requestId, JSON.stringify(body.command)), allowedOrigin);
      }
      return response(failure(405, 'method_not_allowed', 'This method is not allowed.'), allowedOrigin);
    } catch (error) {
      console.error(JSON.stringify({ event: 'jot_request_failed', message: error instanceof Error ? error.message : 'Unknown failure' }));
      return response(failure(500, 'server_error', 'The game server could not finish this request. Please retry.'), allowedOrigin);
    }
  },
} satisfies ExportedHandler<Env>;
