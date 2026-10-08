# C++ dictionary benchmark

The benchmark is separate from unit tests: elapsed-time assertions would be
unreliable on shared CI runners. From `jot_flutter`:

```sh
clang++ -std=c++17 -O3 -DNDEBUG -Ipackages/jot_core/src \
  packages/jot_core/tests/performance_benchmark.cpp \
  packages/jot_core/src/dictionary.cpp packages/jot_core/src/engine.cpp \
  packages/jot_core/src/ai.cpp -o /tmp/jot-core-benchmark
/tmp/jot-core-benchmark assets/dictionary/words.txt > /tmp/jot-core-result.json
```

Use the same compiler, release flags, machine and quiet measurement window for
both revisions. `JOT_BENCHMARK_DENSE_TRIE` lets the current harness compile against
the original dense dictionary headers and sources, such as commit `b0bbb60`;
it only changes benchmark introspection. There is no architecture-specific CPU
flag. The constructor is measured five times with a fresh dictionary, membership
seven times, and four fixed hint states five times each. Their complete public
saves and resulting words, scores, placements and rack indices are included in
the JSON so native and WASM runtimes can use identical inputs.

The checked-in `performance_baseline.json` and `performance_compact.json` are
observations on macOS x86_64 with Apple Clang 21 and `-O3` on October 8, 2026:

| Measurement | Dense baseline | Compact byte-rank trie |
| --- | ---: | ---: |
| Original words / nodes | 143,722 / 293,227 | 143,722 / 293,227 |
| Allocated trie node/edge storage | 31,668,516 bytes | 4,691,628 bytes (-85.19%) |
| Fresh construction, median | 20.09 ms | 20.72 ms |
| Sorted positive membership, median per word | 34.85 ns | 48.78 ns |
| First-construction process peak RSS | 38,776,832 bytes | 13,004,800 bytes |
| Opening `CATSLINE` hint | 23.10 ms | 24.17 ms |
| Opening `C@SEINRT` hint | 199.52 ms | 193.61 ms |
| Horizontal `AT` with `CATSLINE` | 12.61 ms | 12.78 ms |
| Vertical `AT` with `C@SEINRT` | 109.89 ms | 110.65 ms |

Peak RSS includes the benchmark process, input text and constructor scratch; it
is neither live heap usage nor Flutter application RSS. Later repeated-build
peaks also reflect allocator retention and are reported separately. The compact
result adds a deterministic shuffled membership sample; rerunning the current
harness against both revisions produces that metric for both. Sorted membership
regressed by about 14 ns per word, while actual hint medians stayed within ordinary
run-to-run scatter. These figures describe this host rather than every platform.

The selected layout uses 12-byte nodes and packed 4-byte edges. Three byte-prefix
counts reuse node padding; a small immutable lookup table replaces per-edge
software population count. Construction retains only one 4-byte parent index
per child (1,172,904 bytes for this asset), plus a short prefix path, and never
constructs a dense temporary trie. Full transition fingerprint
`669227770809285337` and all four returned moves match the dense baseline exactly.
The unit suite pins that fingerprint, compares a generated vocabulary against an
independent dense reference and verifies the four exact hint results. No words,
game rules, AI budgets or tie-breaking order changed.
