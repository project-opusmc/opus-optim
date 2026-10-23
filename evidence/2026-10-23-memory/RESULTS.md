# O2 memory increment — October 23, 2026

O2 now has a compact glyph table and an independent experimental
`world.chunk_buffer_capacity` startup candidate. The latter changes only four
initial allocation sizes in the exact-pinned `bfg` constructor. Each layer
starts at 8192 integers (32 KiB), so a four-layer builder initially owns 128 KiB
instead of 10 MiB. Actual mesh requirements still use the existing grow path.
There is no limit on mesh size and no change to worker count, builder count,
upload ordering, graphics settings or saved user preferences.

## Measurements and limits

Four automated native runs used A–B–B–A order, the same copied saved world and
options, the same probe, and the production Launcher/OpusLoader path. A uses the
previous five O2 patches and renderer. B also selects the new chunk-buffer
candidate and the current compact glyph helper. All eight staged JARs and each
manifest were checked against the selected bundle. Each run completed terrain,
reload, VBO OFF/ON, unload and world reopen with 96 created / 96 deleted / 0 live
regions and GL error 0. The disposable saved world/options were restored after
the experiment. This is automated workload evidence, not physical GUI acceptance.

At the terrain observation, both arms had 61 idle builders, eight workers and
16 region arenas. Twelve arenas were empty, but the upstream library already
allocates zero storage for empty arenas; their count alone is not wasted VRAM.

| Terrain observation | A, previous implementation | B, candidate |
|---|---:|---:|
| JMX direct-buffer capacity | 657.65–657.76 MiB | 86.42–87.28 MiB |
| Direct capacity owned by the idle builder pool | 610 MiB | 47.625 MiB |
| Glyph array payload | 19,266 B | 4,608 B |
| Glyph array shallow size, Instrumentation | 19,288 B | 4,624 B |
| Process RSS | 480.69–861.75 MiB | 724.35–878.61 MiB |

The direct-capacity reduction is repeatable within this fixture. It is not a
whole-process RAM reduction: **RSS did not establish a benefit and was higher
in both candidate final-unload samples** (827.36–922.45 MiB versus
718.46–744.25 MiB for A). Heap usage and GC observations are retained without
forcing a collection or calling them retained-live-heap measurements. macOS
resident/compressed memory, heap residency, and allocation timing need further
investigation before any whole-process memory recommendation.

Direct capacity also rises during repeated rebuilds: at final unload it is
184.58–190.58 MiB for B, versus 656.83 MiB for A. The pool deliberately retains
buffers for reuse; smaller initial buffers do not prove a bounded long-session
footprint. Cold allocations, copy cost, chunk-completion latency, frame tails,
shader/resource-pack/dimension parity and non-macOS behavior remain unmeasured.
The new candidate remains OFF without its explicit flag; there is no default
promotion or FPS claim.

## Glyph representation and CPU tradeoff

ASCII/Latin-1 uses a direct table read. Higher Unicode blocks use unsigned
offsets to populated 256-character pages; sparse gaps allocate no data pages.
The short-array ABI is retained as opaque storage consumed by `lookup`.
First-duplicate semantics, missing/negative/supplementary inputs and surrogate
rejection remain intact. Dense-page tests cover unsigned offsets past 32767.
The target's array payload is 76.08% smaller, with constant-time lookup.

The glyph-only benchmark ran before the chunk candidate, using two frozen core
JARs whose only differing entry was GlyphIndex.class. It retained 48 independent
Java 8 forks, with three A/A and five A/B pairs per workload. Paired median CPU
ratios were 0.995 for ASCII, 1.072 for Unicode misses and 1.051 for mixed valid
glyphs. Controls and treatment runs are noisy; **no CPU speedup is claimed**.
The representative absolute width-query medians were about 1.52–1.82 ns.
This is a RAM/lookup-cost tradeoff in a real method kernel with owned substitutes,
not a font-render, whole-game or FPS measurement.

## Verification

All 155 active O2 tests pass (api 4 / core 36 / bench 36 / platform 48 / renderer
31). The sparse-table resource-budget test first failed against the flat table;
the new constructor test first failed before the candidate existed. The Launcher
selection test first rejected the unrecognized ID, then passed with the explicit
allowlist entry. Actual Java 8 executed 131,072 font-width parity cases without
mismatch and the pinned builder constructor with allocation substitutes.
Actual M5 buffer-growth methods preserve data, NIO views and sprite prefixes
when growing from 32 KiB through a 20 MiB workload, both with and without the
separate geometric-growth patch. The actual-bootstrap bridge smoke is labeled
SYNTHETIC. Unknown bytecode stays unchanged; four constructor anchors are required.

Launcher workspace format/tests pass: 126 passed, zero failed, one existing
network test ignored. Independent frozen review found no required correctness
fixes; see [review](REVIEW.md). The superproject check is **not complete**:
after fixing Rust formatting and restoring the verified pinned legacy build
toolchain, its CEF guard refused execution because the macOS console was locked
(exit 78). No password was requested and no guard was bypassed. The four native
game runs above completed before that lock. Final CEF/full-check acceptance
remains pending unlock; it must not be reported as PASS.

[Native observations](native-cohort.json), [kernel checks](kernel-verification.json),
[all glyph forks](glyph-kernels.json), [source/check receipt](verification.json)
and [candidate terrain frame](managed-terrain.png) preserve the evidence.
No installed Launcher, remote ref, workflow, own license or third-party license
was changed. This is an incremental memory candidate; full FastMath and a full
Sodium renderer remain separate unfinished work.
