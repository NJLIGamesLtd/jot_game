# Jot: C++ engine and Flutter application

The existing Android and iOS trees remain the reference implementations. The
Flutter rewrite supports a computer opponent, two players sharing one device,
and private online games for two players on separate devices.

Play the web release at [jot-game.pages.dev](https://jot-game.pages.dev/).
See [DEPLOYMENT.md](DEPLOYMENT.md) for the Cloudflare project and redeploy command.
See [MULTIPLAYER.md](MULTIPLAYER.md) for inviting a friend and the online protocol.

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the original coupling, the new ownership
boundary and the remaining migration work. [VERIFICATION.md](VERIFICATION.md)
records the checks run for this milestone.

```text
Flutter widgets -> GameController -> CoreTransport
  mobile/desktop: Dart isolate -> dart:ffi -> C ABI
  web: browser worker -> WebAssembly -> same C ABI
  online: HTTPS -> Cloudflare room -> WebAssembly -> same C ABI
                                         |
                                  C++ Game / Dictionary / AI
```

`packages/jot_core/src` owns the game rules, tile inventory, placement validation,
scores, turn/end conditions, dictionary lookup, computer search, deterministic
randomness, and save-state validation. It has no Flutter, Android, Firebase, or
network dependencies. Board values are copied independently; evaluation and AI
search cannot modify a live game. A game handle has one worker owner.

Flutter owns layout, interaction, animation, accessible labels, worker transport,
and storage of the opaque C++ save document. Native and web builds exchange identical
commands and immutable snapshots. The C ABI catches C++ exceptions and copies
UTF-8 responses before reuse. Save format version 1 includes inventories and RNG
state, and invalid restores leave the live game unchanged.

The original 143,722-word dictionary is bundled as a text asset. It is converted
into a trie by C++ at startup; the original dictionary rights are preserved. See
`assets/dictionary/README.md` for provenance and the reproducible export command.
The JSON dependency is pinned and vendored with its upstream MIT license.

## Run

This milestone is verified with Flutter 3.41.9 and Dart 3.11.5; CI pins that
Flutter version. Use each target's normal build toolchain. The lockfile records
the dependency versions used for verification.

Run the following commands from this `jot_flutter` directory.

```sh
flutter pub get
flutter run -d macos
# Or an Android/iOS device, Windows, or Linux target:
flutter devices
flutter run -d DEVICE_ID
```

The native build hook compiles C++17 and bundles the library automatically;
there is no separate native-library copy step. Windows uses MSVC C++17 flags.

For web, activate the official Emscripten SDK first:

```sh
python3 tools/build_web_core.py
flutter run -d chrome
# Production browser bundle:
flutter build web --release
```

Rebuild the WebAssembly module whenever C++ source changes. `web/jot_core.js` and
`web/jot_core.wasm` are generated together; Flutter copies them into its web build.
Workers and WebAssembly require an HTTP origin. Subdirectory hosting is supported
through Flutter's `--base-href` and relative worker paths.

## Play

Fill your eight-tile rack or choose individual vowels and consonants. Touch a
tile, drag it onto the board, and release to drop it. The tile follows your finger
and the destination square highlights. You can also select a rack tile and tap
a board square. Blanks prompt for a letter. Drag staged tiles to move them. Tapping a staged
tile removes it; Recall removes the whole draft. Hint shows a legal candidate;
Place hint stages it, and Play commits it. Pinch or scroll to zoom the board.

The same drag-and-drop widgets ship on Android, iOS, Windows, macOS, Linux and
web. Mouse and stylus dragging preserve the same grab point and drop behavior
as touch input. Platform gesture regression checks are described in `TESTING.md`.

The first word crosses one of four centre squares. All newly formed words must
be valid, and later words must connect to existing tiles. Bonuses affect new
tiles, blanks score zero, and using eight tiles earns 100 bonus points. A rack
must be filled before playing unless its bags are depleted. Two consecutive
passes or a player using their final tile ends the match, with leftover scoring.

Computer search has a work budget and reports truncation. If a bounded search
finds no move, the computer performs a complete search before deciding to pass.
New game explicitly replaces only this app's current local save.

## Verify

For the complete unit, regression, integration and UI suites with enforced 100%
runtime line coverage, see [TESTING.md](TESTING.md). Run everything with:

```sh
python3 tools/run_tests.py --flutter /path/to/flutter/bin/flutter
```

```sh
cmake -S packages/jot_core -B build/core -DCMAKE_BUILD_TYPE=Release
cmake --build build/core --parallel
ctest --test-dir build/core --output-on-failure
flutter analyze
flutter test
node tools/test_web_core.cjs
node tools/test_web_bridge.cjs
python3 tools/build_server_core.py
node --test packages/jot_core/tests/server_wasm_tests.mjs
cd multiplayer
npm ci
npm run check
npm test
cd ..
```

For cross-runtime save checks, pass the native shared library to the Node test:

```sh
node tools/test_web_core.cjs build/core/libjot_core.dylib
```

The C++ tests cover bonuses, crossings, blanks, bingo, illegal move atomicity,
copy isolation, passes/endgame, restore corruption, dictionary completeness,
exhaustive small-dictionary AI comparisons, and complete computer self-play.
Flutter tests exercise the actual native isolate/FFI transport and responsive UI.
The [CI workflow](../.github/workflows/jot_flutter.yml) is located at the parent
project root. It builds the core on three operating systems and has Flutter build
jobs for Android, iOS, macOS, Windows, Linux, and web.

## Scope

Accounts, rankings, messaging, advertisements, and
legacy saved-game import are subsequent migration work. This version uses the
original board, bags, scores, and dictionary, while correcting the audited
placement, shared-copy, and consecutive-pass defects. It provides a clean engine
boundary shared by the local app and the server that owns online games.

The new code does not change the licensing of the original Jot source or assets.
