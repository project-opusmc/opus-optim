# O2 implementation scope

Updated 2026-10-14. O2 is being engineered as a suite, not declared complete
because one frustum patch exists. These local commits remain on opus-optim.
Every current candidate is EXPERIMENTAL, independently enabled and OFF without
its flag. The user-directed Opus license is unchanged.

| Area | Executable code currently implemented | Work still required |
|---|---|---|
| Render | Frustum adaptive-corner candidate; three font glyph-table anchors | Draw submission/batching, entity/HUD pricing and real visual parity |
| Chunk | Pinned existing upload-drain deadline capped at 2 ms per call | Compile/queue/upload stage pricing, priority/aging/generation cancellation and world lifecycle |
| Graphics | Bounded geometric vertex-buffer growth, existing GL/shader/sprite path retained | Actual GL counters, shader/pack acceptance, texture lifetime and supported backend work |
| Workload | Fixed deferrable upload cap; zero/past sentinel and FIFO/progress retained | Cross-stage budgets, queue-latency pricing and comparison before any adaptive governor |
| Core runtime | Immutable glyph index, direct-buffer allocation/copy reduction and per-resource immutable text cache | Lifecycle leaks, resource metadata invalidation/decode/cache, live hit-rate/heap pricing and additional allocation hotspots |
| FPS/frame stability | Raw-frame capture/comparison infrastructure and local kernel measurements | Managed observer/provider/staging integration; controlled lobby/chunk A/A/A/B and frame-tail/overhead evidence |

FPS is an outcome of these changes, not a separately proven optimization from
a class name. No managed FPS or graphics-quality gain is established. Font widths
and resource-pack data remain live; the current patches do not reduce quality or
defer gameplay/input/network/ticks. Fixed upload deferral deliberately trades
first-drain work for completion latency, and buffer growth trades bounded extra
capacity for fewer allocations. Both costs remain mandatory acceptance criteria.
Resource warm repeats remove most measured allocation, but cold queries add
24 B/query and queried instances retain their String. Independent review is still
pending after its usage-limit failure; no verdict or merge readiness is claimed.

Current platform support is one pinned raw Minecraft 1.8.9 + OptiFine M5 input
per candidate. Unknown/remapped/already modified bytecode is unchanged and reports
unsupported. The provider bridge exists; the current host defaults still approve
no external providers. Claude's Loader work is read-only here. Whole-game
integration, auto-enable, Forge/vanilla support and a terrain rewrite are not
silently inferred from local kernel verification.
Host-supplied routing aliases are available because standalone routes raw avn
through its mapped FontRenderer name. Exact raw bytecode is still required; the
alias tests do not establish live adapter installation.

See [pipeline spec](PIPELINE_OPTIM_SPEC.md), [measured pipeline results](../evidence/2026-10-14-pipeline/RESULTS.md)
and [frustum results](../evidence/2026-10-13-frustum/RESULTS.md).
See [resource-cache results](../evidence/2026-10-14-resource/RESULTS.md) for the
additional core-runtime candidate and measured cold/warm tradeoff.
