# O2 Benchmark Engine Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans inline. Steps use checkbox syntax.

**Goal:** Deliver a usable local raw-frame report and independent-run comparator while Loader's provider contract is still being developed.
**Architecture:** Add pure `:bench`; capture/identity/statistics, JSONL I/O, then evidence-bound comparison and CLI. Never promote or stage game features.
**Tech Stack:** Java 8, existing Gson 2.11.0 and JUnit 5.13.4, Gradle 9.7.0.
**Spec:** [Benchmark engine](../../BENCHMARK_SPEC.md), adapted from the already-established Opus Optim benchmark design.

## Global Constraints
- Branch remains `opus-optim`; existing Opus LICENSE stays byte-identical.
- Pure boundary guard applies to bench. No Loader/game/ASM/GL dependency.
- 1,000,000 raw samples/run; 64 MiB/file; 16,384 characters/record.
- Independent runs, not windows; no fabricated metrics or game performance claims.
- Commit locally; do not push or advance the superproject pin to an unpublished commit.

## Review Focus
- Truncated, duplicate-key, oversized or numerically invalid reports fail closed (Task 2).
- Warmup overflow or incomplete captures do not become usable complete benchmarks (Tasks 1/3).
- Correlated/reused sessions and variant/scope drift cannot produce a signal (Task 3).
- Unequal run lengths do not weight long sessions more heavily (Task 3).
- Synthetic/missing or incorrectly bound evidence cannot promote a feature (Task 3).

### Task 1: Capture and exact statistics
**Files:** settings/build boundary; bench/build.gradle.kts; bench/src/{main,test}/java/org/polydevs/opusmc/optim/bench/{BenchmarkScope,BenchmarkVariant,RunIdentity,BenchmarkRun,FrameCapture,FrameStatistics}.
**Interfaces:** FrameCapture(RunIdentity, BenchmarkScope, int warmupFrames, int capacity); record(long nanos); finish(BenchmarkRun.Termination, Metrics) -> BenchmarkRun. FrameStatistics.of(long[]) -> exact nearest-rank percentiles and OptionalDouble lows.
- [ ] Write tests for warmup, overflow, sealed/immutable captures, invalid durations, exact percentiles/lows and long-sum overflow; run `./gradlew :bench:test --write-locks`; Expected RED: missing classes.
- [ ] Implement the validated capture and identity/statistics contract, with no per-frame allocations.
- [ ] Run `./gradlew test :bench:check`; Expected BUILD SUCCESSFUL.
- [ ] Commit `feat(bench): capture bounded raw frame distributions`.

### Task 2: Strict local report I/O
**Files:** bench/.../{JsonSupport,RunReports,BenchmarkCli}.java; corresponding Report/CLI tests.
**Interfaces:** RunReports.writeNew(Path, BenchmarkRun); RunReports.read(Path) -> BenchmarkRun; BenchmarkCli.run(String[], PrintStream out, PrintStream err) -> exit code.
- [ ] Write roundtrip and invalid/truncated/duplicate/version/count/type/overwrite tests; run `./gradlew :bench:test`; Expected RED: missing report classes.
- [ ] Implement bounded strict JSONL and stats CLI, retaining missing metrics as absent.
- [ ] Run `./gradlew test :bench:check`; Expected BUILD SUCCESSFUL.
- [ ] Commit `feat(bench): persist strict local run reports`.

### Task 3: A/A–A/B comparison and handoff
**Files:** bench/.../{EvidenceReceipt,ExperimentEvidence,ComparisonEstimate,ComparisonResult,BenchmarkComparator}.java; comparator tests; CLI compare; README; docs.
**Interfaces:** BenchmarkComparator.compare(List<BenchmarkRun> aaControl, List<BenchmarkRun> aaRepeat, List<BenchmarkRun> control, List<BenchmarkRun> treatment, ExperimentEvidence) -> ComparisonResult.
- [ ] Write tests for true/noise-level effects, independent sessions, raw counts, scope/variant mismatch, evidence binding, synthetic input, tails, unequal lengths and uncertainty; run `./gradlew :bench:test`; Expected RED: missing comparator.
- [ ] Implement conservative independent-run estimates and fail-closed gates; add compare CLI and document exact assumptions/exit codes.
- [ ] Run `./gradlew clean test check assemble`; verify Java major 52 and exact LICENSE; Expected BUILD SUCCESSFUL.
- [ ] Commit `feat(bench): gate comparisons on independent run evidence` and request one fresh review of the whole change.
