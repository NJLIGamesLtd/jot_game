# Jot online game server

The `jot-multiplayer` Worker routes each invite-only two-player game to a SQLite-backed `GameRoom` Durable Object. The same C++ engine used locally runs as a statically imported WebAssembly module on the server. Flutter submits commands and receives a view containing its own rack. The server owns the authoritative board, both racks, tile bags, scores, turn and saved game.

## Build, verify and deploy

From this directory:

```sh
npm ci
npm run build:core
npm run types
npm run check
npm test
npm run deploy
```

The core build needs Emscripten. Generated engine modules and the dictionary are written to the ignored `generated` directory; build them on a fresh checkout and rebuild whenever C++ or dictionary sources change. `npm test` first exercises the production Wrangler bundle, then runs the unit/regression/integration coverage suite. Both run the actual C++ WebAssembly module inside Cloudflare's local `workerd` runtime with SQLite persistence. They cover both turns, private racks and drafts, hints/scoring, duplicate requests, concurrent revision checks, eviction/restart recovery, request limits and expiration.

`wrangler.jsonc` declares the Durable Object namespace and its first SQLite migration. Keep the class name and migration history stable when redeploying so existing rooms resume. The Pages app forwards `/api/*` through a service binding to `jot-multiplayer`; native Flutter builds use the same public Pages API origin.

## Coverage

```sh
npm run test:coverage
```

This command requires 100% lines, statements, functions and branches in every authored production file: all runtime sources under `src/` and `server_core.mjs`. It discovers source files rather than relying on an allowlist that could silently omit a newly added module. A missing file or a lower percentage fails the command. There are no coverage ignore directives.

Coverage is measured by Istanbul counters executing inside the real Worker and Durable Objects. The harness reads counters through the local workerd inspector; Node test-runner coverage is not used as a substitute. Reports are written to `../coverage/multiplayer/`: `lcov.info`, `coverage-summary.json`, `coverage-final.json`, `index.html` and `scope.json`. Every run clears previous counters before measuring.

The integration cases use the full real dictionary and C++ WASM engine. Additional unit cases use a test-only C ABI fault boundary for runtime initialization failure, malformed responses and save/restore failures, and prove failed commands do not change the committed SQL game. Nine race cases hold a real HTTP initialization barrier while changing SQL, then verify the server rechecks a missing/expired room, revoked player credentials or changed invitation before executing. Alarm tests prove active rooms are rescheduled and expired storage is removed. The test helpers and instrumentation are never part of the production Wrangler entry point.

Generated Emscripten glue, compiled WASM and the dictionary are build artifacts. They are exercised by the actual-runtime tests; C++ source coverage is measured by the separate core suite. Generated declaration files and type-only TypeScript definitions do not contain executable statements.

## HTTP contract

All JSON responses use `Cache-Control: no-store`. Send `Content-Type: application/json` for POSTs. `requestId` is a cryptographically generated UUIDv4, saved on the client before sending. Player credentials are random 256-bit hex tokens, sent only as `Authorization: Bearer <playerToken>`.

| Endpoint | Body / authentication | Successful response |
| --- | --- | --- |
| `POST /api/rooms` | `{requestId}` | HTTP 201; host session and invite |
| `POST /api/rooms/{roomId}/join` | `{requestId, inviteToken}` | HTTP 200; guest session |
| `GET /api/rooms/{roomId}` | Bearer player credential | HTTP 200; current viewer projection |
| `POST /api/rooms/{roomId}/actions` | Bearer credential; `{revision, requestId, command}` | HTTP 200; viewer projection and command result |

Snapshots contain `ok`, `roomId`, `yourPlayer` (0 or 1), `revision`, `joined`, `expiresAt` and `state`. Host snapshots also contain `inviteUrl` and `inviteToken`. Create/join responses contain `playerToken`. The invite is `https://jot-game.pages.dev/?room=<roomId>#invite=<inviteToken>`; the secret stays in the URL fragment. Guest snapshots do not include the invitation. Poll GET every two seconds to receive the other player's committed turn.

Commands are `fill`, `draw` (`bag: vowel|consonant`), `place` (`rack`, `row`, `col`, `letter`), `unplace` (`rack`), `recall`, `shuffle`, `hint` (optional `apply` boolean), `play` and `pass`. The server allows them only after both seats are taken and only for the current player. C++ performs game validation; unsuccessful game commands return HTTP 200 with `ok: false` and `message`. Infrastructure/authentication errors return HTTP 4xx with `ok: false`, `code` and `error`. HTTP 409 revision conflicts also include the current viewer snapshot.

Repeat a create or join with the identical saved request ID to recover credentials after a lost response. A different join request after the guest seat is claimed returns `room_full`. Repeat an action with the identical request ID and command to retrieve its original result and the current snapshot without applying it twice. A request ID reused for another command is rejected. The last 256 action receipts are kept; older retries remain protected by the revision check. Internal saved games, opponent racks, bag contents and opponent pending placements are never returned to clients.

## Lifetime and request bounds

Rooms expire after 30 days of inactivity. Authenticated reads extend activity at most once per hour; commands and joining extend it immediately. A Durable Object alarm removes expired storage. There are exactly two seats and no anonymous room discovery or spectator endpoint. Clearing the saved browser session loses that player's ability to resume, so players should reopen the same browser/device.

Requests are limited to 4 KiB and a strict command allowlist. Workers rate limit bindings bound room creation to 10 requests/minute per incoming client IP, ordinary API requests to 120/minute per player credential, and ingress to 600/minute per incoming client IP. These are Cloudflare location-based abuse limits, rather than transaction accounting. The Pages proxy preserves Cloudflare's client-IP header. Hint search uses the C++ engine's existing bounded search and computer commands are prohibited online.

The runtime and immutable dictionary may be reused within one isolate. Each operation creates a private game handle, restores its room's SQL save, executes synchronously after initialization, commits save/revision/receipt together, and destroys the handle. No mutable game state is shared between rooms. Unknown room reads do not create persistent game tables.

API references: [Durable Object SQLite storage](https://developers.cloudflare.com/durable-objects/api/sqlite-storage-api/), [alarms](https://developers.cloudflare.com/durable-objects/api/alarms/), [Workers rate limits](https://developers.cloudflare.com/workers/runtime-apis/bindings/rate-limit/), [Workers best practices](https://developers.cloudflare.com/workers/best-practices/workers-best-practices/).
