# Cloudflare web deployment

Published on October 7, 2026.

- Public production URL: https://jot-game.pages.dev/
- Initial deployment URL: https://2c7c9971.jot-game.pages.dev/
- Multiplayer Pages deployment: https://15dc92e7.jot-game.pages.dev/
- Multiplayer API Worker: `jot-multiplayer`
- Worker deployment URL: https://jot-multiplayer.yellow-voice-21ca.workers.dev/
- Worker version: `87abc7d5-79be-45bd-9568-6f3a0f6e1087`
- Cloudflare Pages project: `jot-game`
- Production branch: `main`
- Uploaded directory: `build/web`
- Configuration: `wrangler.jsonc`

The Pages site serves the Flutter release and its local C++ WebAssembly engine.
It forwards `/api/*` to the `jot-multiplayer` Worker through a service binding.
Each online room uses a Durable Object with SQLite persistence and the same
C++ rules. Local saves and online player credentials remain on the player's
device; the private online game save remains on the server.

## Redeploy

From `jot_flutter`, with Flutter and Emscripten available:

```sh
flutter pub get
python3 tools/build_web_core.py
python3 tools/build_server_core.py
node --test packages/jot_core/tests/server_wasm_tests.mjs
cd multiplayer
npm ci
npm run check
npm test
npm run deploy
cd ..
flutter build web --release
multiplayer/node_modules/.bin/wrangler pages deploy build/web --project-name jot-game --branch main
node tools/test_multiplayer_live.mjs https://jot-game.pages.dev/
```

Wrangler uses the existing Cloudflare login. Run `wrangler whoami` to check the
account or `wrangler login` if authentication has expired. Credentials are kept
outside the project; no API token is included in the app or configuration.

Deploy the Worker first so that the Pages service binding has its target.
The backend pins its build dependencies in `multiplayer/package-lock.json`.
Its generated JavaScript, WebAssembly and dictionary files are rebuilt from
source by `tools/build_server_core.py`; they are not committed.

The initial local-game deployment used Wrangler 4.112.0 and uploaded 41 files. Cloudflare
serves the worker JavaScript and WebAssembly with their expected content types.
The production URL, worker script and WASM module returned HTTP 200; WASM used
`application/wasm`.

## Public-site verification

The deployed Flutter UI successfully loaded the dictionary and C++ engine,
filled a rack, returned and staged a legal hint, committed a 16-point human
turn, and completed a 134-point computer reply. Reloading restored the board,
both scores and the remaining rack. This check used the production URL above.

Online deployment and public two-client checks are recorded in `VERIFICATION.md`.
The HTTP smoke test creates a new room, joins both seats, plays a C++-validated
word, exercises retries and private views, then ends that test game by passing.

## Local multiplayer development

Build the server core and Flutter web release as above. In one terminal run:

```sh
cd multiplayer
npm run dev -- --var PUBLIC_ORIGIN:http://localhost:8088
```

In another terminal, from `jot_flutter`, run:

```sh
multiplayer/node_modules/.bin/wrangler pages dev build/web --port 8088
```

Open http://localhost:8088/. The Pages service binding reaches the local Worker;
both the browser API origin and generated invites use this local address.
Local Durable Object storage is separate from production storage.

Cloudflare's direct upload workflow is documented at
https://developers.cloudflare.com/pages/get-started/direct-upload/.

## Latest release and checks — October 8, 2026

The web release was rebuilt and published after the expanded test and coverage
checks. The October 7 deployment history above remains unchanged.

- Stable public URL: [jot-game.pages.dev](https://jot-game.pages.dev/)
- Latest Pages deployment: [7a21a5a5.jot-game.pages.dev](https://7a21a5a5.jot-game.pages.dev/)
- Multiplayer API Worker: `jot-multiplayer`
- Worker version: `5fad14f6-1ae9-452e-a2c5-fd9c9f92fc42`

All 120 Flutter tests passed, including 27 drag tests, and the analyzer found no
issues. Four C++ suites passed in Release and with ASan/UBSan. Coverage accounts
for all 23 executable first-party runtime files without coverage-ignore
exclusions: native C++ and Flutter reached 100% lines; browser and multiplayer
modules reached 100% lines, statements, functions and branches. Dart web coverage
reached 52/52 source-mapped lines, a compiled-JavaScript metric whose denominator
differs from native Dart and cannot prove coverage of compiler-eliminated or
folded code. Detailed counts and scope limits are recorded in `VERIFICATION.md`
and `TESTING.md`.

The public Flutter UI successfully dragged a rack tile to row 9, column 9. The
tile was returned afterward, leaving the original saved game and scores of 16
and 134 unchanged. A screenshot is recorded at
`coverage/screenshots/jot-drag-drop-ready.jpg`.

The final live API smoke test passed through the stable Pages URL after this
deployment. It covered create/join retries, two seats, authentication, turns,
revisions, private racks and drafts, C++ scoring, duplicate play, both turns,
resume reads and game completion.

The Android debug APK also rebuilt successfully (156 MB, October 8 at 11:21).
Physical Android gameplay and remote CI were not run; the public browser checks
and local automated test results do not claim either.
