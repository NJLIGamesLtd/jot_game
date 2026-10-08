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

## Beginner guidance — October 8, 2026

All targets now explain the next action with Draw / Place / Play steps. The rack
remains directly above the guidance on phones so tiles can still be dragged
under the pointer. First words have four outlined centre squares. The scrollable
How to play dialog explains drawing, across/down placement, connected words,
blanks, scoring, hints and online invitations; controls also have tooltips and
accessible live guidance. Phone/tablet/desktop and large-text layouts are tested. Wide desktop layouts
also fit the whole square board to the available height: all 16 rows remain
visible and reachable in a 1,280×720 window, with scrolling controls alongside it.
Tests cover last-row placement on the five native platform configurations and
in actual Chrome.

A valid word can still be blocked by Jot's full-rack rule. For the reported JAR
case, the guide explains that JAR is worth 19 points but the rack needs more
tiles, and offers **Return tiles & fill rack**. This recalls only the draft and
draws tiles after authoritative acceptance, preserving committed tiles/scores.
It does not automatically submit the word. Stale turns, rooms, revisions, seats,
lost replies, refusal and disposal prevent the recovery chain from continuing.
An integration test restores the actual C++ fixture, returns/refills/restages
JAR and commits its 19-point score. Multiplayer and depleted bags have their own
instructions rather than misleading local prompts.

## Performance implementation and measurements — October 8, 2026

The audit above was committed before optimization. The following groups preserve
the beginner UI, scoring, dictionary, AI budgets, saved-game format, authentication
and all six platform features:

1. **Idle work and worker lifetime.** Unchanged online reads no longer write
   preferences or notify the page. Hidden, paused, detached and completed games
   stop polling; resuming refreshes immediately without overlapping requests.
   Inactive-but-visible desktop windows continue to refresh. Accepted online
   entry releases the already-saved local engine, and returning restores it.
   Native C++'s bounded shared dictionary cache can remain in the process;
   terminating the browser worker releases its own WASM memory.
   Local actions save once unless a separate computer turn actually occurs.
   Durable pre-send mutation IDs, lost-response retries and storage repair remain.
2. **Dictionary storage and AI traversal.** The immutable trie uses 12-byte nodes
   and packed 4-byte edges, with byte-prefix ranks reusing padding. Construction
   uses compact parent scratch rather than a dense intermediate. AI traversal
   reads packed children through the dictionary accessor. Every dictionary
   transition and four exact hint moves match the baseline; rules and tie order
   are unchanged. The sorted membership microbenchmark is about 14 ns slower per
   word, a documented tradeoff for much lower memory.
3. **Server transfer and serialization.** Each WASM runtime owns one bounded
   UTF-8 dictionary allocation; room operations pass its stable numeric pointer
   rather than recopying 1.29 MB through the string bridge. Failure paths free
   partial allocations and permit retry; fresh views handle WASM memory growth.
   Plain typed RPC replies avoid an unnecessary JSON round trip. The ingress
   token hash is reused. Temporary engine handles, SQL persistence, expiry and
   private player views retain their existing lifecycle and checks.
4. **Board rendering.** A fixed 256-entry cache retains at most one visual face
   and cell per square, and a repaint boundary isolates the board from unrelated
   page updates. Current callbacks and drop eligibility are read from the current
   widget; staged payloads include current turn/revision/seat stamps. Current
   semantics and focus/pointer gates wrap cached visuals. Bonus colours, blank
   scores, ownership, text scaling, first-move markers, zoom and moving tile
   feedback remain unchanged. Extra feedback/inner-grid repaint boundaries were
   tested and removed because raster results did not improve consistently.
5. **Builds and measurement tooling.** Both WASM targets start at 16 MiB with
   memory growth enabled. Cupertino icons were tested for removal, but restored
   after the release compiler reported retained SDK font-family references;
   avoiding possible missing glyphs takes precedence over that size saving.
   `integration_test` is declared as a development dependency for the real profile
   harness. macOS has no native adapter and web tree-shakes the unused test code;
   Android release filters dev plugins, but Flutter 3.41.9 does not filter iOS
   native dev plugins, so its test adapter remains registered there. Serial native,
   WASM, compiler-variant and actual workerd benchmarks retain input/output
   fingerprints and sample metadata. Release speed flags remain `-O3` after
   measuring the size/latency tradeoff; native builds already had `-O3`.

The host was an Intel Core i9-9880H Mac (x86_64), Flutter 3.41.9/Dart 3.11.5,
Apple Clang 21, Emscripten 6.0.3-git and Node 26.5.0. CI pins its own compiler
versions (Node 24 and Emscripten 4.0.13), so local byte sizes need not match CI.
Measurements are observations on this shared desktop, not a statistical claim
about every device. Native constructor/hint runs use five samples, membership
seven, server warm reads forty, and compiler hint comparisons twelve. All
compared game output fingerprints match.

| Measurement | Before | After | Scope |
| --- | ---: | ---: | --- |
| Trie node/edge storage | 31,668,516 B | 4,691,628 B (−85.19%) | Allocated representation, excludes strings/scratch |
| First-construction peak RSS | 38,776,832 B | 13,004,800 B | Benchmark process, includes input/scratch; not Flutter RSS |
| Dictionary construction median | 20.09 ms | 20.72 ms | Native release benchmark |
| Sorted membership per word | 34.85 ns | 48.78 ns | Native microbenchmark; slower by about 14 ns |
| Four fixed hint medians | 23.10 / 199.52 / 12.61 / 109.89 ms | 24.17 / 193.61 / 12.78 / 110.65 ms | Native release, exact same moves |
| Adapter create/restore/view/destroy median / p95 | 17.779 / 20.894 ms | 1.969 / 2.629 ms | Warm Node + actual server WASM |
| Same final binary through the legacy string adapter | — | 15.752 ms median | Control isolates repeated dictionary transfer cost |
| Authenticated room read median / p95 | 17.003 / 21.079 ms | 9.006 / 11.637 ms | Actual local workerd HTTP; excludes Internet latency |
| WASM memory after repeated handles | 40,304,640 B (38.44 MiB) | 16,777,216 B (16 MiB) | Tested full-dictionary scenario; memory can grow |
| Visible unchanged minute: GET / preference writes / notifications | 30 / 60 / 30 | 30 / 0 / 0 | Deterministic controller regression, not device power measurement |
| Hidden minute: GET / writes / notifications | 30 / 60 / 30 | 0 / 0 / 0 | Deterministic background regression |
| Save commands in fixed local profile scenario | 59 | 34 | Real worker; preferences mocked in memory |
| Final matched profile build p95 | 29.251 ms | 10.261 ms (−64.9%) | Same 2,036×1,542 physical viewport, DPR 2 and gesture/turn script |
| Final matched build frames over 16.67 ms / 8.33 ms | 22/378 / 23/378 | 1/374 / 23/374 | Synthetic profile frame samples; neither budget is met in every frame |
| Final matched build p99 / maximum | 40.402 / 48.734 ms | 15.060 / 18.826 ms | Instrumented Mac profile |
| Final matched raster p95 / maximum | 26.607 / 35.297 ms | 23.789 / 34.558 ms | Raster still exceeds both budgets; repeated observations do not establish a GPU improvement |
| Final matched total-span p95 / p99 | 42.811 / 59.088 ms | 26.097 / 40.022 ms | Pipeline elapsed span on shared desktop |
| Profile startup observation | 404.069 ms | 285.610 ms | One warm-host launch per run; no general startup improvement established |

The profile harness uses six continuous touch drags and four human/computer
turns through the actual C++ worker and full dictionary. It excludes startup
frames from its warm frame sample. Startup observations are single warm-host
process launches, not cold installed-device startup measurements. It uses
in-memory preferences to avoid overwriting a player's save, so it does not measure
physical preference disk I/O. The live test binding also draws its fading pointer
overlay; these are instrumented, synthetically pumped gesture frames, not direct
production-device FPS or dropped-frame counts. Final matched UI JSON decode
calibration was 0.412 / 0.299 ms p95 before/after (200 samples each, largest reply
9,431 B; maxima 0.797 / 4.620 ms); worker round trips are
asynchronous elapsed time, not UI-thread occupancy. JSON conversion was retained
for these small responses to preserve the common string transport contract;
moving parsing to the existing workers requires a typed-message transport and
cross-runtime copy/latency comparison. This does not satisfy a literal guarantee
that every JSON conversion occurs off the UI thread. Larger history payloads
need a separate benchmark and policy.

Raw UI/server/compiler and release-size observations live in `performance/`;
native dictionary inputs, outputs and allocation results are in
`packages/jot_core/tests/performance_{baseline,compact}.json` and `PERFORMANCE.md`.
The string-RPC control measured 9.010 ms median / 11.585 ms p95 compared with
9.006 / 11.637 for typed replies: no meaningful RPC speedup is claimed. Node
process RSS includes multiple benchmark runtimes/tooling and is not deployed
Worker RSS; it is not used as an app memory comparison.
The matched final frame reports are `baseline_matched_macos.json` and
`after_macos.json`. Earlier selection observations (`baseline_macos.json`,
`cached_ui_macos.json`) held the dense C++ core constant: build p95 fell from
22.261 to 7.878 ms, while raster p95 was 22.819 versus 22.524 ms. Their original
viewport was not recorded numerically, so the matched final pair above is the
primary comparison. External desktop load was not controlled; neither pair
establishes a worst-case frame limit or a causal explanation for GPU outliers.
Both packages in the final matched pair retain the Cupertino font dependency,
matching production. The earlier `baseline_pre_font_restore_macos.json` and
`after_pre_font_restore_macos.json` used the same recorded viewport but omitted
that font: build p95 was 19.684/7.947 ms, raster p95 20.351/21.466 ms, with a
128.416 ms raster maximum after caching. They are retained as earlier
observations, not the primary final-source comparison. That variability is why
no worst-case frame limit or consistent GPU speedup is claimed.

### Size experiments and decisions

With the final compact core and 16 MiB heap, `-O3` produced 300,533 B WASM plus
17,044 B glue. `-O3 -flto` produced 299,097 B plus 18,062 B, saving only 418 B
combined (0.13%), while observed opening/midgame hint medians increased from
1.923/2.100 ms to 2.286/2.716 ms. `-Oz -flto` produced 178,509 B plus 17,815 B,
but hints increased to 3.903/4.402 ms and the single cold-create observation was
276.749 ms versus 70.174 ms. Neither variant was retained. These results justify
speed-oriented flags on this host, rather than predicting compiler performance
on all architectures. Dead-code stripping and Flutter's release icon tree
shaking remain enabled by the existing production toolchains.

Final production releases retain the Cupertino font dependency. The table below
compares the beginner baseline with the complete final implementation using the
same local pinned Flutter SDK. Sizes are sums of regular-file byte lengths,
ignoring symlinks, except the explicitly gzipped Dart bundle. Web directory size
includes renderer alternatives and assets; it is not the initial network download.
macOS app size is an unsigned local bundle, not a compressed store download.

| Release output | Before | After | Difference |
| --- | ---: | ---: | ---: |
| macOS app | 44,462,403 B | 44,479,221 B | +16,818 B (+0.038%) |
| Web directory | 38,497,987 B | 38,517,998 B | +20,011 B (+0.052%) |
| Universal macOS C++ library | 683,120 B | 683,280 B | +160 B |
| Browser WASM | 297,831 B | 300,533 B | +2,702 B |
| Compiled Dart JavaScript | 2,571,265 B | 2,573,952 B | +2,687 B |
| Gzipped Dart JavaScript | 756,010 B | 756,721 B | +711 B |

The final binaries grew slightly. The compact runtime representation and lookup
code prioritize lower live memory and preserved AI throughput; no overall binary
size reduction is claimed. The production web icon font is tree-shaken to 1,472 B.
The macOS profile and release builds and Flutter web release build succeeded.

### Remaining recommendations and reasons

| Priority | Recommendation | Why deferred / next evidence needed |
| --- | --- | --- |
| High | Profile GPU/raster work on physical Android/iOS and each desktop/web renderer; consider a board painter only if those traces support it. | Intel Mac raster p95 still exceeds 16.67 ms. Extra cache boundaries were inconsistent; a full renderer rewrite risks interaction, semantics and visual regressions and needs separate GPU/cache-memory evidence. Zero stutter under every circumstance cannot be guaranteed. |
| Medium | Replace foreground two-second polling with authenticated push notifications. | Background/unchanged work is fixed; push adds reconnect, missed-event and server lifetime behavior. Keep the tested read/retry protocol until that architecture has its own integration coverage. |
| High | Add cancellation to outstanding reads and cooperative AI search, including the robot's exhaustive fallback. | Generation/disposal guards reject stale results, and computation is off the UI thread. Ordinary hints have a visit budget, but an incomplete empty robot search can retry with `size_t::max()`; there is no practical wall-clock/cancellation bound and native close can queue behind it. Capping that fallback would change computer behavior. A cooperative C ABI/transport contract needs lifetime and move-parity tests while preserving durable mutation receipts. |
| Medium | Bound/migrate invitation and room history, and measure large-history JSON conversion. | Arbitrary eviction could discard a valid guest seat or unresolved retry. A storage migration and user-visible recovery policy are needed before choosing a cap. |
| Medium | Benchmark typed worker replies and move remaining JSON conversion off the UI thread where it helps. | The final tested 9.4 KB reply decodes in 0.299 ms p95 (4.620 ms maximum); an earlier baseline observation exceeded 8.33 ms. A new cross-runtime message contract needs parity, failure and copy-cost tests; no claim is made that all parsing is already off-thread. |
| Low | Measure remaining AI candidate allocation/copies before changing scratch lifetimes. | Packed dictionary traversal is verified, but candidate evaluation/result copies and the placement-validation set remain. Moving reusable search scratch incorrectly can change later candidates; exact-move regression tests are in place for a separate measured change. |
| Medium | Evaluate a bounded revision snapshot cache. | Removing dictionary transfer reduced warm adapter work to about 2 ms. Another cache would add seat/privacy, expiry and invalidation state without measured need. |
| Low | Revisit native LTO and architecture-specific packing with matched release benchmarks. | Native core already uses `-O3`; WASM LTO had negligible size benefit and slower hints. Cross-platform throughput and symbol/signing evidence is needed before changing native flags. |
| Low | Isolate the iOS performance harness into a separate runner/package or adopt SDK support for filtering native dev plugins. | The SDK's iOS registrant includes the `integration_test` native adapter despite its dev-dependency declaration. A custom registrant/pod workaround would be regenerated by builds; a dedicated harness needs asset/native-hook parity and distribution validation. No iOS size saving is claimed from the benchmark dependency. |
| Low | Prune Cupertino icons only after tracing retained SDK glyph references on every target. | The app has no direct Cupertino icon imports, but the release compiler still reports its font family. Removal produced a missing-font warning; preserving cross-platform visuals is more important than an unverified font saving. |
| Low | Change asset streaming/compression or add more worker pools. | The shared dictionary is already lazy, cached once and required in full for AI. Network/browser asset compression is separate from in-memory trie storage; extra workers would duplicate memory. No large image decode or per-task raw thread hotspot was found. |

The six platforms share the same UI and controller changes. Platform widget
overrides and Chrome tests cover interaction behavior, while physical-device
power/memory, 120 Hz rendering, signing and store distribution remain separate
validation steps. Complete line coverage does not establish those properties.

### Final automated verification

The final runtime source inventory accounts for all 24 authored executable
files, with no coverage-ignore exclusions. The combined gate passed after the
board, controller, dictionary and server changes:

| Check | Final result |
| --- | --- |
| Native Flutter unit, regression, integration and UI tests | 240 passed |
| Actual Chrome UI tests | 58 passed: platform dragging, beginner guidance, board rendering/cache, drop-only boards and short-window last-row placement |
| C++ Release / ASan+UBSan | Four suites passed in each configuration |
| C++ LLVM coverage | 1,143/1,143 lines, 79/79 functions, 835/854 branches (97.78%) |
| Flutter native runtime coverage | 1,319/1,319 lines, 437/438 branches (99.77%) |
| Authored browser/Pages modules | 138/138 lines, 100% statements/functions/branches |
| Dart web transport | 52/52 source-mapped lines; compiler mapping limits remain as documented in `TESTING.md` |
| Multiplayer Worker / Durable Objects / WASM adapter | 271/271 lines, 358/358 statements, 45/45 functions, 265/265 branches (100% each) |
| Actual full-dictionary browser and server WASM | Browser smoke/cross-runtime checks and both server suites passed |
| Actual local workerd | Four integration suites and eleven coverage-path cases passed |
| Flutter analysis / workflow lint | No issues / actionlint passed |
| Real Mac profile integration scenario | Baseline and final passed, every measured drag verified placed and recalled |

Native line coverage is executable-line coverage; Dart web maps compiled
JavaScript counters back to source and cannot prove compiler-eliminated paths.
C++ and Flutter branches remain below 100%. The remaining Flutter branch is the
defensive private null-session save guard; public callers establish/check the
session before entering it. Complete line coverage is not full branch coverage
or proof of race freedom, visual correctness on physical devices, or frame limits.

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
