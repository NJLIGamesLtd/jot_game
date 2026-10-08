# Performance observations — October 8, 2026

The ranked audit, changes, results, tradeoffs and deferred recommendations are
in [VERIFICATION.md](../VERIFICATION.md). Commands for the retained benchmark
harnesses are in [TESTING.md](../TESTING.md).

| Files | Comparison |
| --- | --- |
| `baseline_matched_macos.json`, `after_macos.json` | Primary final Mac profile pair: beginner baseline versus final implementation, same viewport/DPR, retained Cupertino font dependency and actual C++ worker |
| `baseline_macos.json`, `cached_ui_macos.json` | Earlier board-cache selection with the dense core held constant; viewport was not recorded numerically |
| `baseline_pre_font_restore_macos.json`, `after_pre_font_restore_macos.json` | Earlier matched-viewport pair with the Cupertino font dependency omitted; not the final production configuration |
| `server_before.json`, `server_after.json` | Warm adapter and authenticated local workerd reads, stable scoring fingerprint |
| `server_rpc_string_comparison.json` | Control using the final numeric dictionary adapter and legacy string RPC serialization |
| `compiler_variants.json` | Serial final compact-core `-O3`, `-O3 -flto`, `-Oz -flto` size and hint comparisons |
| `release_sizes_before.json`, `release_sizes_after.json` | Production beginner baseline and final bundles, with fonts retained in both |

These are observations on one shared Intel Mac, not worst-case limits. Profile
frames include synthetic gestures and the live test binding's pointer overlay;
preferences are mocked in memory. Node RSS includes tooling/runtime instances
and is not deployed Worker RSS. Web directory bytes are not the initial network
download. Native dictionary allocation measurements and exact AI parity results
are separately retained in `packages/jot_core/tests/PERFORMANCE.md` and its JSON
files. No report contains player credentials.
