# O2 implementation scope

Updated 2026-10-14. O2 is being engineered as a suite, not declared complete
because one frustum patch exists. These local commits remain on opus-optim.
Every current candidate is EXPERIMENTAL, independently enabled and OFF without
its flag. The user-directed Opus license is unchanged.

| Area | Executable code currently implemented | Work still required |
|---|---|---|
| Render | Celeritas-backed custom region arena, ordered multi-draw and linked pointer setup; actual terrain/reload/VBO cycles; frustum and font candidates | Broader geometry/transparent/pack parity, entity/HUD pricing |
| Chunk | Pinned existing upload-drain deadline capped at 2 ms per call | Compile/queue/upload stage pricing, priority/aging/generation cancellation and world lifecycle |
| Graphics | Separate Celeritas Common Java8/LWJGL2 backend and real GL2.1 pixel/lifetime harness; bounded geometric vertex-buffer growth | Actual GL counters, shader/pack acceptance, texture lifetime and supported backend work |
| Workload | Fixed deferrable upload cap; zero/past sentinel and FIFO/progress retained | Cross-stage budgets, queue-latency pricing and comparison before any adaptive governor |
| Core runtime | Immutable glyph index, allocation/copy reduction, immutable text cache and pinned terminated-server wait guard | Broader lifecycle/physical GUI acceptance, resource metadata invalidation/decode/cache, live hit-rate/heap pricing and additional allocation hotspots |
| FPS/frame stability | Raw-frame capture/comparison infrastructure and local kernel measurements | Managed observer/provider/staging integration; controlled lobby/chunk A/A/A/B and frame-tail/overhead evidence |

FPS is an outcome of these changes, not a separately proven optimization from
a class name. No managed FPS or graphics-quality gain is established. Font widths
and resource-pack data remain live; the current patches do not reduce quality or
defer gameplay/input/network/ticks. Fixed upload deferral deliberately trades
first-drain work for completion latency, and buffer growth trades bounded extra
capacity for fewer allocations. Both costs remain mandatory acceptance criteria.
Resource warm repeats remove most measured allocation, but cold queries add
24 B/query and queried instances retain their String. Independent review of the renderer package returned With fixes; its Important
future-completion race is now fixed with deterministic RED/GREEN tests,138-test
suite and fresh native replay. This does not establish production promotion. An
earlier review attempt reached its usage limit without a verdict.

Current platform support is one pinned raw Minecraft 1.8.9 + OptiFine M5 input
per candidate. Unknown/remapped/already modified bytecode is unchanged and reports
unsupported. The provider bridge exists; the current host defaults still approve
no external providers. The renderer has a separate, explicitly selected LaunchWrapper
tweaker and checked Launcher bundle route; it does not depend on that unpublished SPI. Claude's Loader work is read-only here. Whole-game
integration, auto-enable, Forge/vanilla support and a terrain rewrite are not
silently inferred from local kernel verification.
Host-supplied routing aliases are available because standalone routes raw avn
through its mapped FontRenderer name. Exact raw bytecode is still required; the
alias tests do not establish live adapter installation.

See [pipeline spec](PIPELINE_OPTIM_SPEC.md), [measured pipeline results](../evidence/2026-10-14-pipeline/RESULTS.md)
and [frustum results](../evidence/2026-10-13-frustum/RESULTS.md).
See [resource-cache results](../evidence/2026-10-14-resource/RESULTS.md) for the
additional core-runtime candidate and measured cold/warm tradeoff.

## Celeritas renderer integration

`renderer-celeritas` is executable Java8 code, dynamically linked to Celeritas
Common2.5.0-pre.1, with separate upstream LGPL JAR/source/notices. It replaces
OptiFine M5 region storage, uploads and ordered draw submission, and owns GPU
cleanup across world/reload boundaries. Minecraft/OptiFine still provides CPU
block meshing, culling, lighting, textures and transparent sorting. This is not
the complete Sodium/Celeritas mod or an entity-instancing/shader port.

Launcher source on `opus-optim` validates/stages an optional four-JAR bundle,
selects O2 after OptiFine/before Opus, and exposes a standalone CLI dry-run plus
desktop environment opt-in. The installed Launcher is unchanged. Current Standard native-host terrain/reload/VBO OFF/ON/world reopen workload
completed with96 regions created/deleted,0 live, GL0 and no probe/NPE failures.
The M5 terminated-server wait guard also executed on the actual abandoned future.
This is automated workload evidence; physical GUI/shader/pack/dimension and FPS
acceptance remain separate.
See [renderer status](RENDERER_INTEGRATION.md) for exact boundaries.
