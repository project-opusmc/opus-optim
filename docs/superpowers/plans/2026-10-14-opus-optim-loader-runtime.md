# O2 Loader Runtime Implementation Plan

> Required skill: superpowers:executing-plans, inline continuous execution as
> explicitly requested by the user; one fresh whole-change review at the end.

Goal: run O2 through completed Opus Loader using real managed SPI.
Spec: docs/superpowers/specs/2026-10-14-opus-optim-loader-runtime-design.md
Tech: Java8, ASM5, LaunchWrapper1.12, Gradle, Rust, Celeritas Common.

## Global constraints
Exact opus-optim; preserve primary sources and five Runtime roles; own Opus
license; external LGPL JAR/source; OFF absent; no push/install/workflows.

## Review focus
Actual game-loader helper identity; raw provider/mapped built-in routing;
renderer before lifecycle transforms on pinned ave; rejected/missing selected
providers; selection vs actual patch/GL lifecycle evidence.

### Task1: completed Loader snapshot and managed Runtime boundary
Files: isolated Runtime bootstrap ClassTransformer/Provider/Chain/Plan plus
ManagedTransformProviders; standalone OpusTweaker/adapter; bootstrap and adapter
tests. Snapshot changed/nonignored primary sources and hash them, commit baseline.
Interfaces: ClassTransformer default transform(raw,mapped,bytes); provider
initialize(ClassLoader); TransformerPlan.discover(providerLoader,gameLoader,
approvals,builtIns); ManagedTransformProviders approved ids/exclusions.
- [ ] Snapshot/hash Loader; run existing bootstrap/standalone tests.
- [ ] RED tests: OFF/invalid selection; missing approved provider; initialize
before registrations; providers receive raw while built-ins receive GUI mapping.
- [ ] Implement Runtime policy, routing and classloader preparation GREEN.
- [ ] Run full Runtime check and verify built artifacts, commit tested changes.

### Task2: real O2 managed providers and bundle
Files: platform OptimTransformerProvider/managed preparation, renderer provider,
compile-only loader inputs, bundle task and service descriptors; tests/smokes.
Interfaces: opcode early renderer adapter priority0 with seven exact targets;
platform provider id opus.optim prepares explicit comma-separated patch ids;
real bootstrap JAR property opusBootstrapJar; schema2 providers and8 JARs.
- [ ] RED tests for provider routing/selection/invalid ids/helper identity.
- [ ] Implement providers; preserve sealed startup/OFF, remove active O2 tweak.
- [ ] Verify real Java8 bootstrap linkage and early composition, artifact/JAR
boundaries and8-JAR bundle freshness; full O2 tests GREEN; commit.

### Task3: checked Launcher managed selection
Files: engine optim_renderer/launch options/tests, CLI, desktop, developer probe.
Interfaces: existing optim_renderer_bundle; new optim_patches list; JVM managed
providers property only after validation; no O2 tweak args; runtime SPI marker.
- [ ] RED tests schema2/old schema rejection, missing SPI, patch ids and OFF.
- [ ] Implement8-JAR staging/provider validation and explicit patch selection.
- [ ] Engine/CLI/example tests, clippy, desktop check GREEN; commit.

### Task4: native acceptance and final review
- [ ] Build/check actual Runtime; stage all5 current roles privately.
- [ ] Run native O2-absent base control and O2-selected full lifecycle workload;
require clean logs, actual providers/patched classes, GL0 and balanced regions.
- [ ] Fresh whole-change review. Re-grade; Important fixes one RED/GREEN pass
with green suite, no repeat reviewer. Record every ruling and declined boundary.
- [ ] Commit receipts/docs; retain opus-optim/worktree; clean only plan scratch.
