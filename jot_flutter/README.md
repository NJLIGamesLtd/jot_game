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

### Getting started

The turn guide walks you through **Draw → Place → Play** and explains what is
needed before you can finish a move. Fill your rack to eight tiles at the start
of each turn, then drag tiles onto the board and release to place them. The four
outlined centre squares show where your first word must cross. Later words join
letters already on the board. Check the preview, then press **Play word**.

A valid word preview can still need a rack refill. If you have already placed
tiles with fewer than eight in your rack, **Return tiles & fill rack** returns
only your unplayed tiles and fills your rack. Place the word again afterward;
committed board letters and scores stay saved. You can also choose your new
vowels and consonants individually after using **Recall**. When both of your
bags are empty, you can play the tiles you have.

**How to play** explains the three steps, controls, scoring and online invites.
Control tooltips explain Recall, Hint, Fill rack and Pass. This guidance and
recovery use the same Flutter widgets on mobile, desktop and web.

## GitHub Actions

The repository has three workflows under the root `.github/workflows/` directory:

| Workflow | Runs | Output |
| --- | --- | --- |
| Continuous Integration | Pushes, pull requests and manual runs | Unit, regression, integration, Chrome UI and coverage checks; C++ Release tests on Linux, Windows and macOS, plus Linux ASan/UBSan; coverage reports |
| Build Web | Pushes to `main`, pull requests targeting `main` and manual runs | Complete Flutter/C++ WebAssembly release in `jot-web-release.tar.gz`, with SHA256 checksum |
| Build Production | Pushes to `main`, `v*` tags, pull requests targeting `main` and manual runs | Separate Android, iOS, macOS, Windows and Linux Release artifacts |

Open the repository's **Actions** tab, choose a workflow and select **Run workflow**
for a manual build. Successful builds expose downloads under the run's **Artifacts**
section. The web workflow packages the existing Pages worker and routing files;
it builds the site and does not deploy it. Deployment instructions remain in
`DEPLOYMENT.md`.

Native build versions use `pubspec.yaml` for the version name and the production
workflow's run number for the build number. Artifacts include a commit/ref
manifest and SHA256 checksums. Windows includes its Visual C++ runtime DLLs;
Linux preserves executable permissions, and Apple archives preserve framework
symlinks. macOS builds support macOS 13 or later and include both Apple Silicon
and Intel code, matching the native-assets minimum in the pinned Flutter SDK.

Android builds are unsigned until a release key is configured. Set all four
repository Actions secrets to obtain signed APK and AAB artifacts on `main`,
tags or manual runs: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`. Pull requests always build
unsigned artifacts. The keystore is the base64 encoding of your existing upload
or release key; partial configuration fails rather than using a debug key.
Local signing uses the ignored `android/key.properties` file with `storeFile`,
`storePassword`, `keyAlias` and `keyPassword` properties. Unsigned APKs require
signing before installation.

iOS produces an **unsigned device `.xcarchive`**, not an installable IPA.
Apple signing/provisioning and IPA export remain necessary for distribution.
macOS does not perform Developer ID signing or notarization. These workflows
compile production Release builds; store publishing is a separate step. See
[Flutter's Android signing guide](https://docs.flutter.dev/deployment/android#sign-the-app)
and [iOS release guide](https://docs.flutter.dev/deployment/ios).

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
The [Continuous Integration workflow](../.github/workflows/continuous-integration.yml)
is located at the parent project root. Separate [web](../.github/workflows/build-web.yml)
and [production](../.github/workflows/build-production.yml) workflows build
Android, iOS, macOS, Windows, Linux, and web artifacts.

## Scope

Accounts, rankings, messaging, advertisements, and
legacy saved-game import are subsequent migration work. This version uses the
original board, bags, scores, and dictionary, while correcting the audited
placement, shared-copy, and consecutive-pass defects. It provides a clean engine
boundary shared by the local app and the server that owns online games.

The new code does not change the licensing of the original Jot source or assets.
