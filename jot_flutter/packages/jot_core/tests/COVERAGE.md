# Portable C++ test coverage

Run from `jot_flutter` with Clang, CMake, `llvm-cov`, and `llvm-profdata` installed:

```sh
python3 tools/run_cpp_coverage.py
```

On macOS the runner resolves LLVM through `xcrun`. It builds a Debug instrumented
library, runs all four CTest suites, merges their profiles, and fails unless
every executable first-party C++ line is covered. The coverage mapping comes
from the shipped shared library, while profiles include direct engine/AI tests
and C ABI integration tests. This avoids duplicate discarded inline mappings
from the static test library. Reports are written to `coverage/cpp`:

- `summary.json`: complete source inventory, line/function/branch totals, and
  individual branch outcomes without a hit.
- `lcov.info`: line and branch data consumed by the repository coverage gate.
- `coverage.json`, `coverage.profdata`, and `html/index.html`: detailed LLVM data
  and annotated source.

The inventory includes every `.cpp`, `.hpp`, and `.h` below `packages/jot_core/src`.
Only the separately licensed vendored `third_party/json.hpp` dependency is outside
this first-party scope. `abi_boundary.hpp`, `ai.hpp`, and `jot_api.h` contain only
declarations and have no executable lines. The repository gate pins their hashes
so adding executable code requires reviewing their coverage status.

The verified native run covers **1143/1143 lines and 79/79 functions**. Raw branch
coverage is **835/854 outcomes (97.78%)**, and LLVM region coverage is 1064/1070
(99.44%). No coverage ignore directives, branch exclusions, or production-source
exclusions are used to raise those numbers.

## Test responsibilities

`core_rules` covers tile inventory conservation, seeded draws, every bonus,
opening and connection rules, gaps, crosses, duplicate placements, blanks,
pending-tile relocation, hints applied atomically, full-rack bonuses, both forms
of game completion, score limits, exhausted bags, corruption rejection without
mutation, and terminal saves for either player. It also checks the private ABI
boundary with successful, standard-exception, and nonstandard-exception callbacks.

`c_json_api` covers typed command/save parsing, borrowed-response ownership,
pending save/RNG round trips, invalid commands and corrupted saves, both private
player views, inactive and completed-game actions, the computer turn, complete
no-move searches, and a dense lexicon that exhausts the normal search budget
before finding a move. The latter verifies that the robot retries before passing
and preserves its rack.

`c_abi_allocation_failures` links the unchanged C ABI source and static engine into
one executable with a standard replacement allocator. Keeping them in one process
avoids platform DLL/sanitizer allocation interception bypassing the injected fault.
It exercises creation failures and the constant serialization-failure response,
and verifies that allocation failure preserves the game. Production allocation
is unchanged; `c_json_api` continues to test the shipped shared library.

`dictionary_ai` checks strict dictionary parsing, deduplication and prefix nodes,
an independent brute-force move oracle for crossing/repeated-letter/blank racks,
empty/finished/no-move/score-limit states, incomplete default-budget search, every
word in the original 143722-word asset, and complete self-play with that asset.
The compact trie is compared against an independently built dense trie for
every A-Z transition and terminal marker in a generated vocabulary. The original
asset's complete transition fingerprint, compact allocation bound, and four exact
opening/crossing/blank hint results pin node numbering and search tie behavior.

Additional build/runtime regressions:

```sh
cmake -S packages/jot_core -B /tmp/jot-core-release -DCMAKE_BUILD_TYPE=Release
cmake --build /tmp/jot-core-release -j 4
ctest --test-dir /tmp/jot-core-release --output-on-failure
python3 tools/build_web_core.py
node tools/test_web_core.cjs /tmp/jot-core-release/libjot_core.dylib
python3 tools/build_server_core.py
node --test packages/jot_core/tests/server_wasm_tests.mjs
```

Use `.so` in place of `.dylib` on Linux. The browser test exchanges a save with the
native library and restores it again in Wasm. The server test forbids dynamic
Wasm compilation, checks private views, and restores 100 independent handles
without growing lexicon memory. Native tests also pass with AddressSanitizer and
UndefinedBehaviorSanitizer enabled.

## Remaining branch outcomes

These outcomes remain in the raw report. They are defensive checks or internal
invariants on the measured native target and the shipped dictionary. The table
explains why ordinary valid inputs cannot exercise them; it does not alter the
coverage denominator.

| Source condition | Uncovered outcomes | Constraint |
| --- | ---: | --- |
| AI `visit`: already exhausted on entry | 1 | Every loop checks exhaustion before calling; the existing-square path has only one recursive call and returns immediately afterwards. |
| AI terminal, connected candidate with no placements | 1 | Connection becomes true only when a new rack placement is pushed. |
| AI committed/rack letter outside A–Z | 4 | Constructors and `restore` validate letters; committed blanks carry their chosen A–Z letter. |
| AI encounters a second unused blank | 1 | Each player starts with exactly one blank, and `restore` enforces its inventory. |
| Dictionary current word ends before its common prefix with the previous word | 2 | Sorting and deduplication ensure the current word cannot be a shorter or identical prefix of the previous word. Both the sizing pass and compact construction pass have this invariant. |
| A single turn's total exceeds 1000000 | 1 | At most 8 placements generate at most 16 words of at most 16 letters. Letter points are at most 10, letter multipliers at most 3, and any row/column's word-bonus product at most 16. The loose upper bound is 122980 including the rack bonus. `test_single_move_score_bound` checks these board/scoring constants. A high accumulated score can still exceed the limit and is tested. |
| C ABI dictionary is empty after construction | 1 | The dictionary constructor rejects empty vocabularies; the cache stores only immutable successful constructions. |
| Signed JSON integer above its positive maximum | 1 | The JSON parser represents nonnegative input numbers as unsigned; negative signed values can violate the lower bound but not these positive maxima. Both range errors are tested through the applicable signed/unsigned paths. |
| A validated hint's stage action fails | 1 | Validation and staging evaluate the same immutable game and placements; allocation errors throw to the ABI boundary instead of producing a failed action. |
| Robot retry returns an incomplete, empty move | 1 | The retry uses `size_t`'s maximum budget. The finite 16-cell/8-rack-tile search stays below the native 64-bit budget. With the shipped trie, even a loose bound of 512 starts × 9 blank assignments × 293227 nodes stays below the 32-bit Wasm budget. Custom lexicons on a smaller target may require reassessing this bound. |
| Robot validated evaluation/stage/play/commit fails | 4 | The robot works on a private game copy, fills or exhausts its bags first, and commits a move already validated against that copy. No command can interleave; allocation errors are exceptions, not failed actions. |
| Serialized legal hint has no scored words | 1 | An opening word has at least two contiguous tiles; later moves must adjoin a committed tile. Every valid evaluation therefore contains a scored word of at least two letters. |

The former `evaluate` empty-word error was removed because opening continuity and
later adjacency prove it unreachable. The tests still reject isolated opening
tiles and disconnected later moves. `jot_destroy` directly deletes a handle:
its `shared_ptr`, `unique_ptr`, and `string` members have nonthrowing destructors,
and conforming deallocation cannot throw. A catch around that delete could not
be exercised by valid C++ code. Public command exception handling was moved into
the private callback boundary without changing its response strings or public
C ABI, making the standard and foreign-exception paths directly testable.
