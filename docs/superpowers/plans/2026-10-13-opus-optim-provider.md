# O2 Provider Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Build the pre-discovery O2 provider bridge and bounded telemetry callback, with an honest Loader handoff for real baseline integration.

**Architecture:** A platform-only adapter compiles against a hash-checked Loader draft fixture, excluded from its JAR. Pure core plans trusted startup bindings; immutable registrations check actual order/helper visibility before install. A separate frame callback feeds bench without a new game transform.

**Tech Stack:** Java 8, Gradle, JUnit 5.13.4, test-only ASM 5.0.3.

**Spec:** `docs/PROVIDER_BRIDGE_SPEC.md`

## Global Constraints

- Exact branch `opus-optim`, byte-identical Opus LICENSE.
- Preserve Claude's primary Runtime checkout; no Loader/Launcher edits or staging contract changes.
- Fixture Loader types never enter the O2 platform JAR; pure modules retain boundary guards.
- OFF contributes zero registrations; no game/GL initialization at discovery.
- No game baseline, performance gain, promotion or installation claims from fixtures.
- No push or unpublished superproject gitlink update.

## Review Focus

- A helper resolving through a second loader must be rejected before target definition (Task 1).
- Loader priority/id sorting can contradict planner dependencies or baseline composition (Task 1).
- ServiceLoader can read OFF before installation; late install must fail (Task 1).
- A foreign-thread/invalid frame and a concurrent finish must not produce COMPLETE evidence (Task 2).
- Draft API classes or third-party ASM accidentally entering the JAR must fail normal check/assemble (Task 3).

### Task 1: Trusted startup bindings and sealed service provider

**Files:** `platform-opus/build.gradle.kts`, archived fixture/hash receipt,
`platform-opus/src/main/java/org/polydevs/opusmc/optim/platform/{TransformerBinding,ProviderPlan,ProviderState,OptimBootstrap,OptimTransformerProvider}.java`, SPI resource, provider tests; `settings.gradle.kts`.

**Interfaces:** Consumes existing planner/config/recovery/descriptors and pinned draft Loader SPI. Produces `ProviderPlan.prepare(OptimConfig,CapabilitySet,CrashMarker.Recovery,Collection<TransformerBinding>,Collection<TransformerBinding>,ClassLoader)`; `patchPlan()` and immutable `registrations()`; `OptimBootstrap.install(ProviderPlan)`.

- [x] Write tests for zero OFF/SAFE/recovery contribution, unsupported/missing claims, immutable exact targets, dependency/composition order failure, helper identity and initialization, sealed/double install, real SPI/draft-plan behavior and executed injected helper.
- [x] Run `./gradlew :platform-opus:test --write-locks`; observe missing bridge behavior before implementation.
- [x] Implement bindings/plan/state/provider, preserve exclusions and cache transformer metadata.
- [x] Run `./gradlew test check`; expect all tests pass.
- [x] Commit `feat: add sealed O2 Loader provider bridge`.

### Task 2: Safe raw telemetry callback

**Files:** `platform-opus/src/main/java/org/polydevs/opusmc/optim/platform/{FrameTimingBridge,CaptureSession}.java`, callback tests.

**Interfaces:** Consumes Task 1 helper visibility and bench FrameCapture/RunReports. Produces `FrameTimingBridge.start(RunIdentity,BenchmarkScope,int,int,ClassLoader): CaptureSession`, `frameCompleted(long)`, `CaptureSession.finish(Termination,Metrics): BenchmarkRun` and `failureReason(): Optional<String>`.

- [x] Write tests for no attached session, one active capture, warmup/overflow, nonpositive/foreign writer invalidation, finish races, immutable seal, helper duplication and JSONL roundtrip with SYNTHETIC origin.
- [x] Run `./gradlew :platform-opus:test`; observe missing callback behavior.
- [x] Implement synchronized lifecycle and INTERRUPTED downgrade; no new timer/transform/I/O in callback.
- [x] Run `./gradlew test check`; expect all tests pass.
- [x] Commit `feat: bridge frame telemetry into O2 capture sessions`.

### Task 3: Artifact verification and concrete Loader handoff

**Files:** platform artifact verification, `docs/LOADER_HANDOFF.md`, README, `evidence/2026-10-13-provider*.{json,md}`.

**Interfaces:** Consumes Task 1 SPI JAR, Task 2 callback. Produces a build-verifiable artifact boundary, provenance and exact host integration/baseline prerequisites.

- [x] Inject a throwaway Loader class into JAR inputs; run artifact verification and observe rejection; remove fixture.
- [x] Implement artifact boundary guard and wire check/assemble; no bundled Loader/ASM/game classes, exact SPI provider.
- [x] Write handoff for empty-provider fix, managed approval/classpath/predefinition install, telemetry callback and controlled OFF/A/A baseline protocol.
- [x] Run `./gradlew clean test check assemble`, Java 8 executable smoke, JAR/class-major/license checks, and fresh-clone build.
- [x] Commit verification/handoff, then one fresh whole-change review; fix Important findings with RED-to-GREEN regressions and full suite. Preserve findings/rulings in evidence.
