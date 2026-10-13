# O2 render/chunk/runtime increment

Implemented three independent pinned startup candidates: text.glyph_lookup,
render.buffer_growth and world.fixed_budget. All remain EXPERIMENTAL, OFF without
explicit flags, not staged into a managed game, and not promoted. The original
frustum candidate remains separate, with its previously reported regression.

## Font render/core lookup

Three real M5 glyph-alphabet indexOf anchors now use one private immutable lookup
table initialized from the target's existing constant. Color/format lookups and
pack/Unicode width arrays remain unchanged. 131,072 original/patched font-width
cases matched on actual Java 8: all BMP characters, both Unicode flags. The
verifier also hashes reconstructed unmodified methods and checks all three
public factory/pin/application/refusal routes.

Forty independent Java 8 processes: five per A/A-control, A/A-repeat, A/B-control
and A/B-treatment arm for ASCII and Unicode-miss workloads. Each has 10,485,760
warmup queries and ten samples of 1,048,576 queries, direct interface calls and
consumed identical checksums. Medians are medians of per-fork sample medians.

| Width-query workload | A/B median cost reduction | A/A median shift |
|---|---:|---:|
| ASCII | 90.37% | -1.78% |
| Unicode-miss | 96.54% | -0.07% |

These are isolated font-width kernel costs: roughly 15.66 -> 1.51 ns and
44.04 -> 1.53 ns respectively. They are not total text-render cost or FPS.
No statistical significance, confidence interval or whole-game acceptance is
claimed. The local point effects exceed the observed A/A median shifts. All raw
reports and variation are retained in font-microbench; no alternate JVM flags.

## Graphics buffer/core allocation

Execute original/patched real bfd.b(I)V and getBufferQuadSize bodies against owned
allocation/log/sprite/vertex-format substitutes, with native-order direct NIO
buffers. For an incremental 2 MiB initial -> 20 MiB used workload:

| Mechanism | Control | Treatment |
|---|---:|---:|
| Direct reallocations | 9 | 4 |
| Bytes copied by growth | 90 MiB | 30 MiB |
| Peak buffer capacity | 20 MiB | 32 MiB |

The memory tradeoff is **12 MiB extra peak capacity (+60%)** in this fixture.
32 MiB limits speculative headroom, not legitimate required allocations larger
than that. Preserved tested byte prefix, native order, Int/Short/Float positions
and sprite prefix/previous-array release are kernel evidence; no shader/visual
acceptance or live native-memory measurement is claimed.

## Chunk upload/workload cap

Execute original/patched real bho.a(J)Z drain bodies, replacing the Guava future
type with an owned timed task. Sixty-four FIFO tasks of 250 us nominal busy work:
control drained 64 in ~17.30 ms; capped treatment drained 8 in ~2.00 ms and
required 8 subsequent drain calls to finish. No task disappeared or reordered.
Zero/expired deadline still drains one task; empty queues return false; a 3 ms
single task is not preempted and may exceed the 2 ms cap while leaving the next
task queued. These task times are one synthetic-work kernel observation, not GPU
upload times, game frames, a latency confidence interval or FPS.

This deliberate completion-latency cost prevents default promotion without real
chunk-flight data. No compile queue, chunk generation/cancellation, ticks,
input/network, draw order or graphics quality is changed by this patch.
The cap is per drain call: it does not constrain an entire frame making multiple
calls. Drain count/time variation remains expected, including in fresh verification.

## Scope and remaining acceptance

All three exact raw pins apply through the public candidate -> planner ->
registration path. Unknown input remains unchanged with per-instance observable
refusal. Verification uses real method bodies with owned substitutes and owned
constructors/table init; it does not load the whole original game class hierarchy,
prove full FontRenderer clinit/GL execution or establish installed-provider use.
The host routing-alias factory additionally applies the same pinned font bytes
through the mapped FontRenderer routing name and reports APPLIED; no actual
managed adapter invocation is claimed. Emitted target/helper hashes stay equal
to the recorded timing revision after that factory boundary addition.

Managed Loader approval/helper visibility, observer wiring, staging, real
lobby/chunk controls, pack/shader visual parity, frame p95/p99, allocation and
instrumentation overhead remain required. No default or FPS claim is made.
See kernel-verification.json and font-microbench/summary.json for raw scope and
hashes; final verification and review receipts are saved alongside this report.
