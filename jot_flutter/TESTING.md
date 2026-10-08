# Tests and coverage

The rewrite has unit, regression, integration and UI/widget tests. Run the entire
verification sequence from `jot_flutter` with the pinned Flutter 3.41.9 SDK,
Clang/LLVM, Emscripten, Node 24 or newer and Google Chrome available:

```sh
python3 tools/run_tests.py --flutter /path/to/flutter/bin/flutter
```

The command analyzes Dart, runs every suite including the actual Chrome drag
tests, rebuilds both WebAssembly targets, collects coverage and rejects incomplete
reports. Flutter discovers Chrome using its normal platform defaults. If Chrome
is installed elsewhere, set `CHROME_EXECUTABLE` to its executable path before
running the command, for example:

```sh
CHROME_EXECUTABLE=/path/to/google-chrome python3 tools/run_tests.py --flutter /path/to/flutter/bin/flutter
```

The verification command does not contact production
or deploy anything. CI runs this sequence and uploads the reports alongside the
separate web and five-platform native build workflows.

## Suites

| Kind | Tests | What they verify |
| --- | --- | --- |
| Unit | C++ `core_tests.cpp`, `ai_tests.cpp`, `api_tests.cpp` | Dictionary/trie, legal moves, scoring, bags, blanks, turn transitions, AI budgets and JSON/C ABI validation |
| Unit | Dart `multiplayer_api_test.dart`, `native_protocol_test.dart` | HTTP credentials, URL parsing, timeouts, native-worker protocol, safe allocation cleanup, crashes and close ordering |
| Regression | Dart controller/local/multiplayer tests | Lost responses, exact request-ID retries, stale revisions, lifecycle races, terminal errors, persistence failures and seat recovery |
| Regression | Browser bridge/worker and Pages proxy tests | FIFO requests, copied replies, initialization failures, fatal workers, independent handles and unchanged proxy credentials |
| Integration | `test/integration/local_game_integration_test.dart` | Actual Flutter UI → native isolate → C ABI → C++ game and computer opponent → saved game → reload |
| Integration | Workerd room and coverage-path suites | Actual Workers/Durable Objects → SQL → C++ WASM, two seats, private views, duplicate/concurrent commands, eviction/restart, expiration and initialization races |
| Integration | Browser and server WASM tests | Complete 143,722-word dictionary, native/WASM save interoperability and bounded shared dictionary memory |
| UI | Widget, presentation and drag/drop suites | Phone/tablet/desktop layouts, dialogs, controls, accessibility actions, clipboard fallback, touch dragging, moving feedback, highlights, blanks, zoom and rejected drops |
| Unit / UI | Turn guidance and beginner presentation suites | Draw/place/play instructions, valid-word refill explanations, first-move centre markers, depleted bags, inactive turns, recovery guards and large-text layouts |
| Integration / Regression | Beginner refill and action-confirmation suites | Actual C++ JAR recovery and 19-point play, preserving committed tiles; accepted/rejected commands, stale online snapshots, lost replies, storage failures and disposal |
| Regression | `performance_regression_test.dart` | Unchanged polling does no persistence or notification work; background/resume races, completed rooms, storage repair, local-engine release and one-save local actions |
| UI / Regression | Board cache interaction, rendering and layout suites | Current pointer, keyboard and semantics callbacks, disable/re-enable, live drop rules, refreshed move stamps, tile/bonus colours, blank scores, inherited text scaling and all sixteen rows reachable in short desktop windows |

UI tests run in Flutter's test renderer with real pointer gestures. They are
automated widget tests, and the local-game integration test uses the actual C++
engine. They do not substitute for testing an APK on physical Android hardware.

### Platform drag regression gate

`test/platform_drag_test.dart` exercises drag placement and rejection across
Android, iOS, Windows, macOS and Linux platform settings and phone, tablet and
desktop layouts. Continuous Integration requires this file in the full native
widget suite. Every native Release build also runs it with the native C++
transport tests. The web workflow additionally compiles and runs this
suite, beginner guidance, board cache and height-fit UI suites in actual Google Chrome, using the browser installed on the
GitHub runner. `tools/run_tests.py` also runs these Chrome suites after collecting
native Flutter coverage. These browser tests exercise the web renderer; native platform
settings in widget tests do not establish physical Android or iOS input behavior.

The root `.github/workflows/continuous-integration.yml` owns test and coverage
checks. `build-web.yml` and `build-production.yml` own the release builds and
downloadable artifacts. The former combined `jot_flutter.yml` workflow is removed
so those jobs do not also run through a duplicate workflow.

Run the focused checks with the pinned Flutter 3.41.9 SDK:

```sh
flutter test test/platform_drag_test.dart
CHROME_EXECUTABLE=/path/to/google-chrome flutter test --platform chrome test/platform_drag_test.dart test/beginner_guidance_test.dart test/board_rendering_regression_test.dart test/board_cache_interaction_test.dart test/board_layout_test.dart
```

Flutter 3.41.9 supports the Chrome test backend but labels it deprecated in its
[test command source](https://github.com/flutter/flutter/blob/3.41.9/packages/flutter_tools/lib/src/commands/test.dart).
Reassess that backend when upgrading the pinned SDK.

## Coverage scope and gates

The 100% requirement is **exact 100% line coverage within the declared native and
source-mapped metrics, with every first-party runtime file accounted for**. No coverage-ignore directives remove
production error paths. Inventories reject missing files and empty reports;
source changes after report generation invalidate the final check.

- LLVM measures C++ rules, AI, dictionary, C ABI and private exception boundary,
  including executable header code. Reports: `coverage/cpp/html/index.html`.
- Dart VM coverage measures Flutter UI, controllers, HTTP and native transport.
  Report: `coverage/lcov.info`; branches are collected separately.
- The actual Dart web transport is compiled with dart2js and tested through JS
  promises. V8 coverage is mapped back to its Dart source. These mapped line and
  branch counts differ from Dart VM executable-line counts; they must not be
  presented as the same metric. Mapped counts include non-executable source lines
  and cannot prove coverage of compiler-eliminated code. The harness explicitly
  exercises all authored transport methods. Reports: `coverage/dart-web/`.
- V8 measures the authored browser worker, bridge and Pages API proxy. Each file
  must reach 100% lines, statements, functions and branches.
- Istanbul instrumentation executes **inside workerd**, including Durable
  Objects. Each multiplayer runtime file must reach 100% lines, statements,
  functions and branches. The Node harness is not counted as server coverage.

The combined inventory and line gate is `python3 tools/check_coverage.py`.
Its human-readable result is `coverage/summary.md`, with detailed JSON in
`coverage/summary.json`. C++ and Flutter branch coverage is reported honestly;
100% line coverage does not imply every branch has both outcomes covered.

Declaration-only interfaces, external FFI declarations and exports contain no
executable lines. Hashes make executable changes to those files require a scope
review. Flutter/Dart SDK code, npm dependencies and vendored `json.hpp` belong to
their dependencies. Generated Emscripten glue, WASM binaries, dictionary assets,
platform runners and build/deployment tooling are validated separately rather
than included in the runtime line percentage. The original Android/iOS trees
remain historical references; these measurements apply to the C++/Flutter rewrite.

Coverage reports are generated artifacts and are ignored by version control.
The test sources, repeatable commands and CI gates are committed project files.

## Focused commands

```sh
python3 tools/run_cpp_coverage.py
flutter test --coverage --branch-coverage --coverage-package jot_flutter --coverage-package jot_core
npm --prefix tools ci
npm --prefix tools run coverage
python3 tools/run_dart_web_coverage.py --dart /path/to/flutter/bin/dart
npm --prefix multiplayer ci
npm --prefix multiplayer run test:coverage
python3 tools/check_coverage.py
```

Rebuild the server WASM with `python3 tools/build_server_core.py` after modifying
C++ before running multiplayer tests. `npm test` in `multiplayer` additionally
checks the production Wrangler bundle; test-only instrumentation and fault
controls never appear in the deployed Worker.

## Optional performance measurements

Frame measurements require a real profile build rather than the widget test
renderer. The retained harness uses the actual C++ engine, complete dictionary,
persistent worker, six continuous touch drags and four human/computer turns:

```sh
flutter drive -d macos --profile \
  --target integration_test/performance_test.dart \
  --driver test_driver/performance_driver.dart
```

It writes `build/integration_response_data.json`: startup observation, frame
build/raster/total percentiles, command round trips and a separate JSON decode
calibration. Preferences are in memory to protect player saves; this does not
measure real preference disk latency. Frame samples are bounded at 10,000.
Compare the same SDK, host, viewport, seed and quiet measurement window. This
opt-in benchmark is separate from CI's deterministic correctness gates: elapsed
time assertions on shared runners would be unreliable. Physical devices and
other OS renderers require their own profile runs.

The native dictionary benchmark and matched outputs are described in
`packages/jot_core/tests/PERFORMANCE.md`. Server/WASM measurements can be rerun
after building the server core and the Wrangler dry-run bundle:

```sh
cd multiplayer
node test/performance-benchmark.mjs --count=40 --hints=12 \
  --output=../build/server-performance.json
cd ..
python3 tools/benchmark_wasm_flags.py --output-dir build/performance-flags
```

The server harness exercises actual local workerd, authenticated private reads
and a fixed JAR scoring fixture. Reports contain fingerprints rather than player
credentials. `benchmark_wasm_flags.py` builds `-O3`, `-O3 -flto` and `-Oz -flto`
serially and checks unchanged game/hint outputs. Neither command contacts
production or deploys. Results and measurement limits are in `VERIFICATION.md`
and `performance/`; they do not establish zero stutter on every device.
