# O2 region draw batching — October 15, 2026

The existing optional Celeritas region renderer now retains live arena segments
in its queue, avoiding the second identity-map lookup at finish. It reads each
segment's current offset and length at finish, so relocation is still respected.
Increasing adjacent complete GL_POINTS/LINES/TRIANGLES/QUADS are coalesced into
one draw range without sorting. Gaps, reverse order, repeated meshes, incomplete
primitives, strips/fans and unknown modes keep separate ranges. Combined signed
GL counts cannot overflow. Thread ownership, upload exclusion during a queued
batch, cleanup after a failed draw and the existing 65,536-entry bound remain.
The optimization uses the same explicit optional renderer selection, eight-JAR
bundle and managed Loader provider. No new flag or Runtime artifact role exists.

## Correctness and integration

Eight new regression tests cover merging, incomplete primitives, restart modes,
ordered gaps/repeats, relocation, queue growth, signed count overflow and draw
failure recovery. The original renderer produced three expected RED failures.
An older relocation fixture accidentally used adjacent 83 + 8 = 91 and asserted two
commands; it now uses a gap at 96 to preserve its ordering purpose, while the new
fixtures explicitly assert the merged command stream.

All 151 active O2 tests pass (api 4 / core 33 / bench 36 / platform 47 / renderer 31), with zero
failures/errors/skips. Java 8 smoke against the real bootstrap passes on its
explicitly synthetic target. Actual seven-class MC 1.8.9 / OptiFine M5 hook verification
also passes. These are distinct from the native game workload.

The real Java 8/LWJGL2/Celeritas GL 2.1 fixture on Apple M4 checks blended pixel
parity against separate vanilla draws, preserving overlap order after real arena
growth/relocation. Its safe adjacent case goes from 2 commands to 1 with identical
pixels. Reverse memory order stays at 2 commands. All 21,522 colored pixels match;
replacement, empty uploads and native/GPU cleanup pass with GL error 0.

Managed native session `1792032393139-41996` runs the new bundle through the existing
OpusLoader. It reaches the real early CEF frame and terrain at 2000x1300, executes
renderer reload, VBO OFF/ON, unload and world reopen, then exits 0. Final counters
are 96 created / 96 deleted / 0 live, GL 0, no NPE or probe failure. All 8 staged JARs and
the manifest are independently byte-identical to the build. The [terrain PNG](managed-terrain.png) was
visually inspected. This run does not measure the in-game command reduction
ratio; the measured 2-to-1 result belongs to the GL fixture.

## Local CPU queue/packing measurement

The driver compiles the old RegionRenderer from faf6303 and the exact candidate
source separately, then runs independent Java 8 JVMs with one shared driver. Each
batch queues 128 four-vertex meshes. There is no GL driver in this timing path.
Three A/A pairs and five alternating-order A/B pairs run per layout. The longer
confirmation uses 150,000 warmup batches and 500,000 measured batches per fork.

| Layout | Commands per batch before / after | Paired median treatment / control CPU time |
|---|---:|---:|
| Increasing contiguous ranges | 128 / 1 | 0.669 |
| Fragmented ranges | 128 / 128 | 0.750 |
| Reverse memory order | 128 / 128 | 0.737 |

This is about 25–33% lower paired median CPU time for this isolated queue/packing
workload. All measured warmed runs report 0 allocated bytes per batch. A/A ratios
still span 0.816–1.190 in the confirmation, so the exact magnitude is noisy and
must not be assigned to whole-frame time. The shorter first run is retained,
including one fragmented A/B ratio above 1.0; it is not silently discarded.

[Confirmation raw forks](packing-confirm.json), [initial raw forks](packing-first.json)
and [source/artifact/native receipt](verification.json) retain all measurements.
Reproduce on an otherwise idle machine with the desired Java 8 VM:

```sh
python3 scripts/region-batch-microbench.py \
  --java8-home /absolute/path/to/java8/home \
  --warmup-batches 150000 --batches 500000 \
  --output /new/path/packing.json
```

The microbenchmark does not measure GPU speed, a native command-reduction ratio,
frame tails, FPS, or a full chunk mesher. No default enablement or performance
promotion follows. Physical GUI/input, shader/resource-pack/dimension parity,
non-macOS coverage, installation and release remain separate. This work leaves
Runtime/Launcher sources, primary Loader files, saved user preferences, own
Opus license and third-party licensing unchanged; nothing is installed or pushed.

[Independent frozen-patch review](REVIEW.md) completed with no actionable
findings within the existing optional-backend scope. The superproject check
completed on 2026-10-19 with exit code 0, including both CEF oracles, production
migration and release-lock contract checks. It ran against this tested candidate
working tree; the O2 pin is synchronized to the resulting commit afterward.
