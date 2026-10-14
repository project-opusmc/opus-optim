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

Current measurements are the corrected v2 patch in `font-microbench-v2`.
The original `font-microbench` and first verification receipts are historical:
their isolated width kernel ran, but author full-class verification later found
a VerifyError in obfuscated text from replacing alphabet length/charAt loads.
That whole renderer candidate was invalid and its timings do not describe the
corrected patch. The retained v1 data is not current acceptance evidence.

The fix preserves every String literal and substitutes only the three indexOf
calls. The expanded owned draw-method regression failed with VerifyError first,
then passed. Actual complete raw font definition and clinit also pass on Java8,
preserving the original 256 page slots and adding 19,266 bytes of table payload
in an isolated unsigned test classloader. This verifies class definition/clinit,
not full font constructor, GL draw behavior or managed adapter installation.

Forty new independent Java 8 processes: five per A/A-control, A/A-repeat, A/B-control
and A/B-treatment arm for ASCII and Unicode-miss workloads. Each has 10,485,760
warmup queries and ten samples of 1,048,576 queries, direct interface calls and
consumed identical checksums. Medians are medians of per-fork sample medians.

| Width-query workload | A/B median cost reduction | A/A median shift |
|---|---:|---:|
| ASCII | 90.18% | -2.44% |
| Unicode-miss | 95.81% | -2.17% |

These are isolated font-width kernel costs: roughly 15.90 -> 1.56 ns and
44.37 -> 1.86 ns respectively. They are not total text-render cost or FPS.
No statistical significance, confidence interval or whole-game acceptance is
claimed. The local point effects exceed the observed A/A median shifts. All raw
reports and variation are retained; no alternate JVM flags.

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
prove full font construction/GL execution or establish installed-provider use.
The host routing-alias factory additionally applies the same pinned font bytes
through the mapped FontRenderer routing name and reports APPLIED; no actual
managed adapter invocation is claimed. Emitted target/helper hashes stay equal
to the recorded timing revision after that factory boundary addition.

Managed Loader approval/helper visibility, observer wiring, staging, real
lobby/chunk controls, pack/shader visual parity, frame p95/p99, allocation and
instrumentation overhead remain required. No default or FPS claim is made.
See corrected-kernel-verification.json, probes/full-font-init.log and
font-microbench-v2/summary.json for current raw scope and hashes. Independent
review failed at its usage limit without a verdict; that gate remains pending.
