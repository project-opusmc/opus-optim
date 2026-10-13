# O2 Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement and verify concrete text/render, graphics buffer and chunk/workload candidates, continuing the wider O2 suite.
**Architecture:** Three pure core kernels feed pinned platform ASM patches and per-candidate status handles. Actual local method bodies execute in Java8 kernel fixtures and independent query forks.
**Tech Stack:** Java 8, ASM 5.0.3 compile-only, JUnit5, existing four modules.
**Spec:** docs/PIPELINE_OPTIM_SPEC.md

## Global Constraints

- Exact branch opus-optim and byte-identical Opus LICENSE. Local commits only.
- EXPERIMENTAL independent flags, OFF without enable, no managed/FPS/promotion claim.
- Exact avn/bfd/bho M5 pins, no bundled vendor/GL/Loader/Guava classes.
- Preserve ticks/input/network, draw ordering, FIFO/queue ownership and pending work.
- 32 MiB speculative buffer ceiling, 2,000,000 ns upload cap, original zero/past deadlines.
- Keep host-owned ASM, artifact roles, Claude work and published gitlink.

## Review Focus

- Font first-duplicate/NUL/Unicode/pack-width semantics; no stale metrics cache (Task1/3).
- Modified/unknown target must remain unchanged with visible per-instance refusal (Task2).
- Buffer growth must retain bytes, view positions and quad-sprite bookkeeping while reporting memory cost (Task3).
- Upload zero/past/wrap deadlines, FIFO, slow tasks and progress over subsequent frames (Task1/3).
- Kernel/A/A results cannot imply whole-game integration, visual parity or FPS gains (Task3).

### Task 1: Pure lookup, allocation and workload kernels

**Files:** core/.../render/GlyphIndex.java, BufferGrowth.java; core/.../workload/DeadlineBudget.java and corresponding tests.
**Interfaces:** GlyphIndex.create(String):short[], lookup(short[],int):int; BufferGrowth.capacity(int required,int previous):int; DeadlineBudget.cap(long deadline,long now,long budget):long.

- [ ] Add first-index/missing/invalid char and exhaustive BMP tests against owned alphabets; bounded memory and surrogate refusal.
- [ ] Add literal allocation-capacity/overflow and deadline zero/past/wrap tests; observe missing kernels.
- [ ] Implement those three kernels without game/Loader/GL references.
- [ ] Run ./gradlew test check and commit passing deliverable.

### Task 2: Pinned executable candidates

**Files:** platform-opus/.../mc189/PipelineOptimizations.java, PipelinePatch.java and PipelinePatchTest.java.
**Interfaces:** PipelineOptimizations.candidates():List<Candidate>; Candidate.descriptor(), binding(), applicationState(); consumes Task1 kernels.

- [ ] Add real owned bytecode tests for transformed behavior plus exact public refusal, per-instance status and independent flags/capabilities; observe missing candidates.
- [ ] Implement exact pins, four glyph replacements/table init, one bfd capacity-store modification and bho method-entry cap.
- [ ] Verify counts/claims, fail closed on unknown input, no automatic installation. Run full suite/check/assemble and commit.

### Task 3: Real Java8 kernels and measurements

**Files:** platform-opus smoke PipelineVerify.java, Java8 task; scripts/pipeline-microbench.py; README and evidence/2026-10-14-pipeline.
**Interfaces:** consumes real local game/M5 jars, actual JVM8 and Task2 public factory. Verifies transformed real method bodies in owned kernel fixtures, no vendor output committed.

- [ ] Run original/patched real font width for all 65,536 chars and both Unicode states; assert unchanged format and unmodified methods.
- [ ] Verify buffer bytes/views/sprite prefix and measure grows/copied bytes/peak capacity across incremental workload.
- [ ] Verify real upload FIFO/sentinel/progress/slow-task behavior with an owned future substitute and preserve raw task counts/durations.
- [ ] Record independent warm font A/A and A/B forks/checksums, no game claims. Run clean full suite, both existing Java8 smokes and new verifier.
- [ ] Commit code/evidence, one fresh whole-range review, one test-backed Important fix pass, fresh-clone artifact/license checks; retain all limits/rulings.
