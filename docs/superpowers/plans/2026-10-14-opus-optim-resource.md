# O2 Resource Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove repeated immutable resource-text allocation with an independently gated raw M5 patch.
**Architecture:** Per-resource lazy volatile field; exact-pin ASM patch keeps original miss body; per-candidate status handle. Real full raw class verification with existing commons-lang3 on Java8.
**Tech Stack:** Java8, ASM5 compile-only, JUnit5, existing platform module and harness.
**Spec:** docs/RESOURCE_CACHE_SPEC.md

## Global Constraints

- Exact opus-optim and byte-identical Opus LICENSE; no push or host changes.
- EXPERIMENTAL, independent flag and raw-target/ASM capability, OFF by default.
- Only exact jy SHA256 in spec; no vendor/Loader/ASM/commons classes bundled.
- Keep original cold content, constructors, equals/hashCode and subclass dispatch.
- Cache retains extra heap; no global table, single-flight or FPS claim.
- Independent review is currently unavailable due usage limit; record pending, never PASS.

## Review Focus

- Existing cold exceptions/normalization must remain identical (Task2).
- Warm cache is per instance; subclasses and concurrent readers preserve content (Task1/2).
- Extra branch/frame/field must verify as actual Java8 class bytecode (Task1/2).
- Cold allocation/retained heap costs and A/A variation remain visible (Task2).
- Absent/OFF/missing capability and unsupported bytes never pretend application (Task1/2).

### Task 1: Executable resource cache candidate

**Files:** platform-opus/.../mc189/ResourceOptimization.java, ResourcePatch.java and ResourcePatchTest.java.
**Interfaces:** ResourceOptimization.candidate():Candidate; descriptor(), binding(), applicationState().

- [x] Write cold/warm/subclass/instance/status/flag tests against an owned bytecode fixture; observe missing candidate.
- [x] Implement exact hash gate, volatile field, hit branch, original cold body and result retention with a valid frame.
- [x] Run full ./gradlew test check assemble and commit.

### Task 2: Real full class verification and fork measurements

**Files:** smoke ResourceVerify.java, Gradle resourceVerify task; scripts/resource-microbench.py; evidence/2026-10-14-resource and README/status/handoff.
**Interfaces:** Consumes Task1 factory, local MC/M5 inputs, actual Java8 and explicit existing commons-lang3 jar.

- [x] Execute complete original/patched class with real constructors/dependency; verify content/errors/equals/hashCode/subclass/concurrent readers and public state.
- [x] Measure five independent forks per AA/AB arm on cold/warm string workloads; preserve checksums/identity, throughput and heap-retention caveats.
- [x] Run clean full suite and all4 Java8 verifiers, fresh-clone JAR/license guards; commit evidence.
- [x] Preserve author review and independent-review failure without a verdict; keep review gate pending.
