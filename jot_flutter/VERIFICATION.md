# Milestone verification

## Performance audit — October 8, 2026

This audit was completed before performance edits. The baseline is commit
`b0bbb60`, including the beginner guidance and touch/mouse drag controls. Active
targets are Android, iOS, macOS, Windows, Linux and web: Flutter 3.41.9 / Dart
3.11.5, C++17 through native FFI isolates or a browser WebAssembly worker, and
Cloudflare Workers / SQLite Durable Objects for multiplayer. Build systems are
Flutter platform tooling, native-assets CBuilder, CMake, Emscripten and Wrangler.
The original Android/iOS directories are historical implementations.

| Rank | Finding | Planned response |
| --- | --- | --- |
| High | Every unchanged online poll still persists session/history and notifies the whole page; polling continues while hidden or backgrounded. At a two-second cadence this schedules about 30 reads and 60 preference writes per minute. | Suspend background polling, refresh on resume, avoid unchanged writes and rebuilds, preserve durable mutation retries. |
| High | Entering online mode retains the unused local engine worker, including at least 32 MiB of browser WASM linear memory. | Release the already-saved local engine and verify returning to local play restores its save. |
| High memory | The dictionary has 293,227 dense trie nodes, each with 26 child integers and a terminal flag: approximately 30.2 MiB before words and other allocations. | Benchmark compact immutable links against dictionary lookup and AI search before choosing the representation. |
| High aggregate server CPU | Each room read copies the 1,293,136-byte dictionary through the WASM string bridge, restores a temporary engine and produces an identical player view at unchanged revisions. | Measure creation/read costs; remove repeated dictionary transfer and consider a strictly bounded revision cache while retaining authentication, expiry and persistence checks. |
| Medium | Local actions save twice even when no computer turn occurs; UI-isolate JSON conversion, repeated save parsing and full-page notifications add work. | Remove redundant saves first; measure conversion/frame costs before changing transport or widget boundaries. |
| Medium | Generation guards discard obsolete results but do not stop CPU work or underlying HTTP requests. Room and invitation histories have no size bound. | Test cancellable reads and worker teardown; preserve in-flight move IDs and valid saved seats. Document storage-policy changes requiring a separate migration. |
| Medium | AI candidate evaluation copies a board and allocates a set; server replies take a redundant JSON encode/decode round trip. WASM builds use `-O3` without LTO. | Benchmark small algorithm improvements and release optimization variants; reject size savings that harm responsiveness. |
| Low | A player token is hashed twice per request; Cupertino icon dependency appears unused. | Reuse existing work and verify imports before pruning dependencies. |

Existing strengths: one persistent worker per local engine, lazy board cells,
immutable shared dictionary caching with one entry, temporary-handle cleanup,
bounded server receipts, capped request bodies, and asynchronous networking.
No per-command raw-thread pattern or confirmed normal-close worker leak was
found. Native core builds already use `-O3`; backend npm packages are development
tools, so removing them would not shrink the deployed application.

Baseline artifacts: each C++ WASM binary is 297,831 bytes, browser glue 17,048
bytes, server glue 14,922 bytes, dictionary 1,293,136 bytes and universal macOS
core library 683,120 bytes. These are uncompressed filesystem sizes. Measurements
will distinguish engine benchmarks, production/profile frame samples and
deterministic regression counts. Tests cannot establish zero stutter on every
device; 16.67 ms and 8.33 ms are the 60 Hz and 120 Hz frame-budget reference points.

Checked on October 7, 2026, using Flutter 3.41.9 / Dart 3.11.5 on macOS.
Dependency versions are recorded in `pubspec.lock`.

| Check | Result |
| --- | --- |
| Release CMake / CTest | All 3 suites passed: rules, C JSON API, dictionary / AI |
| AddressSanitizer and UndefinedBehaviorSanitizer | All 3 C++ suites passed |
| Flutter analysis | No issues found |
| Flutter tests | All 6 passed: actual native isolate / FFI and responsive widgets |
| Actual WebAssembly engine | Full 143,722-word dictionary, game commands and save validation passed |
| Cross-runtime saves | WebAssembly → native → WebAssembly round trip passed |
| Browser worker lifecycle | All 6 Node tests passed |
| macOS debug app | Built `build/macos/Build/Products/Debug/Jot.app` |
| iOS simulator debug app | Built `build/ios/iphonesimulator/Runner.app` |
| Android debug APK | Built `build/app/outputs/flutter-apk/app-debug.apk` |
| Web release | Built `build/web` |
| Cloudflare Pages deployment | Published and played at `https://jot-game.pages.dev/` |

The Android APK contains `libjot_core.so` for arm64-v8a, armeabi-v7a and x86_64.
The iOS simulator bundle contains `jot_core.framework` with all three exported
ABI functions. The native transport tests exercised the compiled C++ library,
including initialization, commands, computer moves, persistence, input errors
and concurrent closure.

Browser checks exercised the Flutter UI with its real WebAssembly worker:
fill rack, hint, stage, scoring preview, removal of one staged tile, rejection of
the resulting invalid word, replacement hint, committed human turn, computer
reply, and local save/resume. A restored game retained its scores and board, and
computer replies cleared obsolete hints.

The C++ tests also cover copy isolation, crossing words, blanks, bonus reuse,
bingo, consecutive passes, terminal scoring, malformed restore atomicity,
complete dictionary lookup, exhaustive small-dictionary AI comparison and a
complete computer game.

## Reproduce

Run the commands in `README.md` from `jot_flutter`. For sanitizer checks with a
supported Clang or GCC toolchain:

```sh
cmake -S packages/jot_core -B build/core-sanitize \
  -DCMAKE_BUILD_TYPE=Debug \
  -DCMAKE_CXX_FLAGS="-fsanitize=address,undefined -fno-omit-frame-pointer" \
  -DCMAKE_EXE_LINKER_FLAGS="-fsanitize=address,undefined" \
  -DCMAKE_SHARED_LINKER_FLAGS="-fsanitize=address,undefined"
cmake --build build/core-sanitize --parallel
ctest --test-dir build/core-sanitize --output-on-failure
```

## Not yet verified

Full Windows and Linux builds have not run on this Mac. Their runners and the
parent project's CI build matrix are provided; no remote CI run was performed.
Windows export declarations were separately cross-compiled and linked into a
small DLL test, which checks ABI declarations rather than the full application.

iOS verification is a simulator build. Physical-device gameplay and signed store
releases were not performed. Native release signing remains part of each
platform's distribution setup. The web release is publicly deployed on
Cloudflare Pages; `DEPLOYMENT.md` records the deployment and public-site checks.
Account-based identity and recovery remain outside this milestone.

## Multiplayer verification

Added on October 7, 2026.

| Check | Result |
| --- | --- |
| C++ release and ASan/UBSan | All three suites pass, including private player views |
| Workers WebAssembly | Full dictionary, private views and 101 temporary handles pass with stable 38.44 MiB memory |
| TypeScript and Worker bundle | Type check and Wrangler dry deployment pass |
| Actual workerd integration | Four suites pass: both turns/privacy, concurrent actions, SQLite restart/recovery, and request bounds |
| Flutter client | Analysis passes; all 19 tests pass, including persisted retries, room recovery and guest controls |
| Deployed Worker API | Two independent HTTP clients complete a game using real C++ scoring |
| Public Pages API | The same two-client smoke test passes through the Pages service binding |
| Public Flutter browser | Host creates an invite, plays a 29-point word, receives a 50-point friend turn, and restores the shared board after reload; guest invite entry and seat resume also pass |
| Android and macOS debug builds | Rebuilt successfully with HTTP support and macOS network client entitlement |

The public API check exercises idempotent creation and joining, refusal of a
third seat, bearer authentication, wrong-turn and stale-revision rejection,
server-only saves, hidden opponent racks and staged tiles, a committed word,
a duplicate commit, both players' turns, reload-style reads and game completion.
The local workerd tests also evict a room and restart the entire runtime while
retaining its SQLite save. The Flutter tests cover a lost move response followed
by reload, a lost join response, reusing a pending invitation, clearing processed
invite URLs, saved room selection, reconnecting at the same revision and replies
arriving after controller disposal.

The browser check used separate authenticated HTTP clients as the opposing
players. The host UI received committed turns through live polling. A second
invitation opened the guest UI directly, removed the invite token from the
address bar, showed the guest's turn after the host's move, and restored the
guest seat on reload. The independent API smoke tests also exercised terminal
scoring and completion. The guest UI filled its own rack and passed, received
game completion from the opposing client, then returned to the original local
save. Reload kept local mode and preserved the online room history.
Native debug builds compile the same HTTP client; no
physical-device multiplayer check or Windows/Linux build was performed here.

## Testing and drag verification — October 8, 2026

The results below supersede the test counts above while preserving the October 7
milestone history. Flutter analysis found no issues, and all 120 Flutter tests
passed, including 27 drag tests. All four C++ suites passed in Release builds and
with AddressSanitizer and UndefinedBehaviorSanitizer.

| Coverage scope | Latest result |
| --- | --- |
| C++ rules, AI, dictionary and C ABI (LLVM) | 1,103/1,103 lines; 78/78 functions; 822/840 branches (97.86%) |
| Flutter UI, controllers, HTTP and native transport | 1,103/1,103 lines; 378/379 branches (99.74%) |
| Flutter `main.dart`, included in the Flutter total | 499/499 lines; 126/126 branches (100%) |
| Multiplayer Worker, Durable Objects and WASM adapter | 259/259 lines; 344/344 statements; 45/45 functions; 263/263 branches (100% for every metric) |
| Three authored browser/Pages modules | 138/138 lines; 100% statements, functions and branches; all 16 tests passed |
| Dart web transport, mapped from compiled JavaScript | 52/52 mapped lines (100%) |
| Coverage-gate regression tests | All five passed |

All 23 executable first-party runtime files are accounted for. No coverage-ignore
directives exclude their production paths. Audited declaration-only files,
dependencies, generated artifacts and platform/build tooling are documented
separately in `TESTING.md`. Native C++ and Dart measurements use executable-line
coverage; the Dart web result maps compiled JavaScript coverage back to Dart.
Its line denominator includes source-map artifacts and differs from Dart VM
coverage. It describes retained compiled paths and cannot establish coverage of
code the compiler eliminates or folds. The web harness explicitly exercises all
authored bridge methods. The C++ and Flutter branch counts above remain below
100%; complete line coverage does not imply complete branch coverage.

The release web application was rebuilt and published at
[the October 8 Pages deployment](https://7a21a5a5.jot-game.pages.dev/), with
[the stable public URL](https://jot-game.pages.dev/) serving that release. Its
multiplayer Worker version is `5fad14f6-1ae9-452e-a2c5-fd9c9f92fc42`.

An actual public Flutter UI drag moved a rack tile to row 9, column 9 successfully.
The test tile was then returned to the rack, preserving the saved local board and
scores of 16 and 134. The screenshot is
`coverage/screenshots/jot-drag-drop-ready.jpg`. Automated drag tests also cover
placement, moving staged tiles, removal, feedback and rejected drops.

The final two-client API smoke test passed through the deployed Pages service
binding after publication. It verified create/join retries, the two-seat limit,
authentication, turn checks, revision checks, private racks and drafts, actual
C++ scoring, duplicate play, both players' turns, resume reads and game completion.

The Android debug APK rebuilt successfully on October 8 at 11:21, producing
`build/app/outputs/flutter-apk/app-debug.apk` (156 MB). This verifies a build;
physical Android gameplay was not tested. No remote CI run was performed.

## Shared UX across all targets — October 8, 2026

All six runners use the same `GamePage`, `JotBoard` and rack drag handlers. No
platform-specific game screen or gesture implementation bypasses them.

The new `test/platform_drag_test.dart` passed all 15 widget cases on the Dart VM:
Android, iOS, Windows, macOS and Linux defaults at phone (360×780), tablet
(820×1180) and desktop (1440×1000) sizes. Each case exercises touch, mouse and
stylus feedback, preservation of the original off-centre grab point, exact cell
placement, staged-tile relocation, occupied/outside rejection and pointer
cancellation. The rack and drop destination remain visible at each size.

The same suite compiled and ran in actual Google Chrome: all 18 cases passed,
including three additional cases using the browser's default platform settings.
The full native Flutter regression and coverage suite now passes all 135 tests,
and the runtime coverage gate still passes at exactly 100% lines within its
documented measurement scope. C++ and Flutter branch results remain as above.

| Target | Build and gesture evidence |
| --- | --- |
| Android | Updated debug APK built; shared platform/widget input cases passed |
| iOS | Debug simulator app rebuilt successfully with the updated UI; shared input cases passed |
| Windows | Shared platform/widget input cases passed; full Windows build is configured in CI and was not run here |
| macOS | Debug app rebuilt successfully with the updated UI; shared input cases passed |
| Linux | Shared platform/widget input cases passed; full Linux build is configured in CI and was not run here |
| Web | Previously published release uses these shared widgets; all 18 Chrome cases passed and a public-site drag at 390×844 successfully placed a tile |

The public phone-size check returned the test tile to the rack afterward,
preserving the local board and scores, and restored the browser viewport. Its
placement screenshot is `coverage/screenshots/jot-phone-drag.jpg`.

Every build job now requires the platform test file in its full widget suite.
The web job additionally runs it in Chrome, and `tools/run_tests.py` includes
that browser run. No remote CI execution or physical-device input check was
performed here: platform overrides exercise Flutter defaults, not another OS's
native event loop. Those limits apply to the evidence above.
