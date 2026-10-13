# O2 provider review and fix receipt

Reviewed range: `716caef..bdaad61`. The fresh independent reviewer found
0 Critical, 2 Important, 0 Minor; its pre-fix verdict was not ready to merge.
No second review was performed. The author verified one fix pass through four
observed failing regressions, followed by the complete green suite.

## Findings and fixes

1. A callback admitted through the bridge could block on the session monitor;
   finish overtook it and falsely returned COMPLETE. Callback admission,
   validation, recording and sealing now share one lifecycle monitor, without
   session-to-bridge lock inversion. Invalid-duration and foreign-thread racing
   regressions both failed with COMPLETE before the fix and pass with INTERRUPTED.
2. Provider preparation reduced baseline descriptors to claims and ignored
   explicit conflicts in either descriptor direction. The core resolver now
   accepts active baseline descriptors, retains CONFLICT reasons and propagates
   exclusions to dependents. Candidate-to-baseline and reverse-declaration
   regressions both failed before the fix and pass after it.

Fixed implementation: `9c62f397d5b44e648d319a97065c61eb69a3603b`.
Full clean test/check/assemble: **93 tests**, zero failures/errors/skips.
Actual Java 8 VM smoke: **1.8.0_502 PASS**, using built O2 JARs and separate draft
API fixture, SYNTHETIC origin. Fresh clone of the fixed implementation repeated
all checks and smoke with identical SHA-256 for all four JARs.
All 54 production classes are Java 8 major 52; common Opus LICENSE matches exactly.
See `2026-10-13-provider.json` for artifact hashes and integration status.

The reviewer independently exported the reviewed revision and ran its 89 tests;
all four original hashes matched that revision's receipt. It reported normal
check AND assemble rejecting actual Loader and ASM class bundling. Our own
normal test task also rejected the temporary foreign Loader-class probe, removed
before the final builds. The reviewer did not independently rerun the Java 8 VM
smoke; that smoke is the author's direct verification before and after fixes.

## Rulings made, including all review scope decisions

- Ruling: Use a hash-checked draft Loader fixture because published Runtime lacks the SPI — permits independent bridge work without touching Claude files — cost if draft changes: re-pin and rebuild before host use.
- Ruling: Native implementation proceeds under the user's repeated authorization to continue engineering — no redundant design gate — cost if scope differs: local reversible commits only.
- Final: Ruling: Actual adapter behavior, empty-provider repair, managed approval/classpath/staging and predefinition enforcement remain host integration — this slice never edits or installs Loader — cost if wrong: game integration remains unproven.
- Final: Ruling: Candidate bytecode/member linkage, mappings/anchors and receipts remain trusted host declarations — no production candidate is supplied — cost if wrong: an unsafe future candidate could fail until candidate-specific validation.
- Final: Ruling: Baseline-only composition and helper correctness remain host-owned — O2 preserves baseline and validates candidate interactions — cost if wrong: pre-existing baseline defects persist.
- Final: Ruling: Dependencies naming baseline-only ids fail closed as DEPENDENCY_MISSING — candidate-only registry semantics remain explicit — cost if wrong: such a candidate stays disabled until cross-baseline dependency semantics are defined.
- Final: Ruling: Real-game completeness, observer lifecycle, parity, overhead and gains are unclaimed — host wiring and controlled game runs are absent — cost if wrong: no trustworthy game-performance acceptance yet.
- Final: Ruling: Artifact guard covers ordinary accidental bundling, not disguised/relocated-code provenance authentication — class namespace/symbol/SPI checks are the declared guard — cost if wrong: provenance requires separate verification before distribution.
- Final: Ruling: Unchanged foundation behavior was not independently re-audited — 69 existing tests plus new regressions run in the full 93-test suite — cost if wrong: an undiscovered old semantic defect may remain.

## Deferred minors

None. Host wiring, live adapter/game sessions, baseline/overhead/parity and gain
remain unimplemented integration work, not a deferred minor or a claimed PASS.
The five existing managed artifact roles and published superproject gitlink are
unchanged; all new work remains local on `opus-optim`.

## Archived source formatting

The full-range git diff whitespace check reports original blank lines at EOF in
archived ClassTransformer.java and TransformerChain.java. Those published Loader
bytes are deliberately preserved under the existing hash-checked snapshot
ruling. The authored diff excluding this separate source archive passes
`git diff --check`; this is an archive formatting diagnostic, not a failed test
or a claim that the archive was reformatted.
