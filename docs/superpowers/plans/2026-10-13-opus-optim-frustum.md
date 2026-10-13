# O2 Frustum Candidate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement, bind and measure a real OptiFine M5 frustum optimization.
**Architecture:** Pure float math helper chooses one corner with exact slow fallback; platform ASM transformer accepts one pinned raw class and an experimental descriptor. Local real-bytecode/JVM8 harness measures query throughput without claiming game FPS.
**Tech Stack:** Java 8, existing platform-opus, compile-only ASM 5.0.3, JUnit 5.
**Spec:** `docs/FRUSTUM_CANDIDATE_SPEC.md`

## Global Constraints

- Branch `opus-optim`; exact Opus LICENSE; preserve Claude/primary files.
- No Minecraft/OptiFine/ASM/Loader classes bundled, no new artifact role or push.
- Only exact M5 raw Notch pin; preserve float comparison/disabled/full predicate.
- EXPERIMENTAL, explicit flag/capabilities, OFF by default; no game gain claim.

## Review Focus

- Float cancellation/rounding near zero must match original corner results (Task 1).
- NaN/infinity/overflow, reversed bounds and malformed matrices need exact fallback behavior (Task 1).
- Unknown/remapped/already modified targets must remain unchanged and report unsupported (Task 2).
- Disabled bypass and fully-contained predicate must remain intact (Task 2/3).
- JIT dead-code elimination, arm asymmetry and visible-workload regressions must not masquerade as an improvement (Task 3).

### Task 1: One-corner math with parity fallback

**Files:** `platform-opus/src/main/java/org/polydevs/opusmc/optim/platform/mc189/FrustumMath.java`, independent oracle/math tests.
**Interfaces:** `FrustumMath.visible(float[][],double,double,double,double,double,double): boolean`; no game/GL/ASM types.

- [x] Write tests for touching plane, coefficient signs, rounding, random normalized/bit-pattern planes, nonfinite/large/reversed bounds and malformed matrices against an independent eight-corner oracle.
- [x] Run `./gradlew :platform-opus:test`; observe missing math implementation.
- [x] Implement positive-vertex math and exact float fallback; final measured revision uses adaptive first/supporting tests and ordered rejection per the spec/ruling.
- [x] Run `./gradlew test check`; expect parity and full suite pass.
- [x] Commit `feat: optimize OptiFine frustum corner evaluation`.

### Task 2: Pinned transformer and opt-in binding

**Files:** `mc189/{FrustumPatch,FrustumOptimization}.java`, candidate tests, platform Gradle compile-only ASM and guard allowance.
**Interfaces:** `FrustumPatch.matchesTarget(byte[]): boolean`, ClassTransformer implementation with application state; `FrustumOptimization.binding(): TransformerBinding` consumes Task 1 helper.

- [x] Write negative pin/non-target tests, synthetic bytecode execution test, flag/capability/OFF/recovery tests and unchanged disabled/full-predicate tests.
- [x] Run platform tests; observe missing transformer/binding before implementation.
- [x] Implement exact hash gate, one-method rewrite, observable unsupported input, EXPERIMENTAL descriptor and helper binding.
- [x] Run `./gradlew test check assemble --write-locks`; expect all tests and packaging guards pass.
- [x] Commit `feat: bind pinned frustum patch behind explicit O2 flag`.

### Task 3: Real bytecode verification and controlled microbenchmark

**Files:** smoke `FrustumVerify.java`, direct adapter/harness, Gradle task, README, evidence receipt and raw fork reports.
**Interfaces:** consumes Task 2 public transformer, local game/M5 JAR inputs, Java8 home. No vendor bytes committed.

- [x] Build Java8 harness to extract exact local M5 target, execute original/patched classes and verify at least 100,000 seeded cases plus edge/disabled/full cases; do not reflect inside timed loops.
- [x] Run parity task with actual local JARs and JVM8; require zero mismatch and pin rejection.
- [x] Run warmup and independent A/A, interleaved A/B forks for inside/mixed/outside; preserve checksums, raw timings, scope, version/hash identity and regressions.
- [x] Run full clean test/check/assemble, existing Java8 bridge smoke, candidate Java8 verification, license/class/JAR checks; commit code and evidence.
- [x] One fresh whole-change review; fix Important findings through RED-to-GREEN and full suite; preserve all findings/rulings and limits, no promotion.
