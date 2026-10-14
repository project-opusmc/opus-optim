# O2 render, chunk and runtime pipeline candidates

The owner requests the optimizer suite, not completion of one frustum candidate.
This increment implements three executable startup patches against current local
Minecraft 1.8.9 + OptiFine M5 raw bytecode. FPS/frame tails are outcomes to measure,
not a separate feature that can promise gains without a game run. The broader
suite remains in development. All candidates remain EXPERIMENTAL and individually
OFF without explicit flags; managed installation and FPS acceptance are separate.

## Actual source opportunities

M5 already caches EnumFacing arrays and deduplicates many GlStateManager calls.
Do not add duplicate optimizations there. Verified bytecode still performs three
linear glyph-alphabet searches in avn, drains chunk upload futures in bho.a(J)Z
against the supplied deadline, and grows/copies bfd vertex buffers in approximately
2 MiB increments. Preserve all unrelated rendering, simulation and network work.

## Text rendering and immutable core lookup

`text.glyph_lookup`: pinned avn input
bf11b41d4cfa907e6cd3b517497cb8a82fd3ef21ef375b558dc6427c76e98c3e.
Replace only its three glyph-alphabet String.indexOf(int) anchors with a private
static final short lookup table initialized once from the existing literal in
the target bytecode. No vendor alphabet is copied into source. Keep first-index
semantics for duplicate characters, missing/negative/supplementary inputs, Unicode
and the NUL entries. The table has at most 65,536 shorts. Format-code lookups,
font widths, style flags and pack-owned width arrays remain live and unchanged.
Claims: a(CZ)F, a(Ljava/lang/String;Z)V, getCharWidthFloat(C)F plus <clinit>()V
and the private synthetic o2$glyphIndex field. Reject unknown input by identity.

## Graphics vertex-buffer allocation

`render.buffer_growth`: pinned bfd input
5d231d8bc0b76de6072d24eab8f6f49d7bb84a6044ab6a33ae84aa1296c60a40.
At the one computed-capacity store in b(I)V, select max(original required growth,
min(2*old capacity, 32 MiB)); no speculative headroom beyond 32 MiB. Preserve
original invalid/overflow outcomes by forwarding non-growing/negative capacities.
No draw order, vertex contents, buffer-view restoration, shader/quad-sprite
bookkeeping or GL call changes. This trades bounded extra capacity for fewer
direct allocations/copies; peak memory and copy counts must be reported together.

## Chunk upload and workload control

`world.fixed_budget`: pinned bho input
381976963d303e09ea0fba4fe4c0b0ec6db24db0b4da10e88fce999904e372c7.
Clamp the positive remaining deadline on a(J)Z entry to 2,000,000 ns from the
current clock. Keep zero's single-drain sentinel and already-expired deadlines.
This is a per-drain-call cap, not a guarantee about a whole frame with multiple calls.
Signed nanoTime wrap follows the original subtraction semantics. The original
do/while still executes at least one task, leaves pending futures queued, retains
FIFO and synchronization, and naturally progresses on subsequent calls. It never
preempts an upload already running: one slow task may exceed the cap. Only this
deferrable upload drain is budgeted; no ticks/input/network or resource quality
changes. Queue latency may increase and must be measured before promotion.

## Startup pack and verification

PipelineOptimizations exposes a fresh per-candidate handle with descriptor,
binding and NOT_SEEN/APPLIED/UNSUPPORTED_INPUT state. Capabilities include the
exact raw target and loader.asm5; shared priority 400 for these independent
non-overlapping candidates, matching the planner's canonical id order. Each claim has independent
selection/recovery/exclusion through the existing ProviderPlan. No registration
or host approval happens automatically. Keep the retained handle after planning.
The host may supply exact routing aliases with candidate(kind, aliases); these
aliases describe the same pinned raw class, not remapped-bytecode support. This
is needed for the current standalone GUI adapter, which routes raw avn as its
mapped FontRenderer name. Claims and target filtering include both raw/declared
alias identities; the digest and bytecode owner remain mandatory. Default raw
factory bindings alone cannot prove this mapped host path is reached.

Pure GlyphIndex, BufferGrowth and DeadlineBudget live in core with no game/GL
dependencies. ASM and host calls remain in platform-opus, compile-only. No vendor,
GL, Guava or Loader classes are bundled. No new artifact roles or host changes.

Write behavioral tests first; test all flags/capabilities, unknown pins and actual
bytecode anchors. Execute extracted real methods on actual Java 8 in owned kernel
fixtures: font widths for every char and both Unicode settings, vertex contents
and NIO positions including quad-sprite resize, original FIFO upload drain and
multi-frame completion. Clearly label kernel verification: these are real method
bodies with owned dependency substitutes, not whole-game/session/visual proof.
Measure forked local font throughput with A/A and A/B, allocation/copy/peak-memory
mechanisms and upload task counts/latency. Preserve unfavorable evidence; no FPS
claim, promotion or automatic install. Keep exact opus-optim and Opus LICENSE;
preserve Claude's dirty Loader checkout and published superproject gitlink.
