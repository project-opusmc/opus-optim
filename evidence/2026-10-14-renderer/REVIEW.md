# Whole-change review and disposition

Fresh independent reviewer: gpt-6-astra, read-only, no checkout changes or game
launches. Frozen O2 range `038985c..cff5c0b`; Launcher `405d52c..5b031c7`.
Verdict: **With fixes**, no Critical finding. The reviewer inspected production
sources, tests, dependency integration, execution rulings, extracted host
bytecode and the earlier clean native session. This verdict does not represent
an approval of subsequent changes, installation or production promotion.

## Important finding and one fix pass

P2: `ServerTaskWait.prepare` ignored a failed `future.cancel(false)` after timed
get expired and the owner terminated. Completion with an exception, or an
external cancellation, between those events was replaced with successful null.
The original Guava invocation consequently could not propagate that outcome.

Two deterministic tests hold the owner until a real timed-get timeout, then
complete exceptionally or cancel the original future and join the owner before
the guard checks its state. Both failed against reviewed production code with
the wrong replacement-future assertion. Commit `df955a5` returns the original
future if cancellation loses the race. Both tests pass, as does the complete
138-test suite (renderer21), bundle freshness and seven actual Java8 class
verifications. No second reviewer was dispatched; covering RED/GREEN tests and
the green suite resolve the one Important finding.

Post-fix native session `1791962240351-41185` staged the four JARs from this exact
source and independently rehashed each. It completed terrain/reload/VBO OFF/ON,
world unload/reopen and final96 created/96 deleted/0 live, GL error0. The guard
executed on abandoned shutdown futures. Both output streams contain zero NPE or
probe failures; native host and strict probe verdict exited0.

## Minor finding

The frozen snapshot's documentation retained older source/test counts and a
blocked cleanup replay. Concurrent planned evidence finalization was already
refreshing these texts. This minor was recorded separately from the production
fix pass; current RESULTS/verification receipt identifies the tested source and
session. No additional production change is justified by this documentation
finding.

## Execution rulings, in order

1. Link separate published LGPL Celeritas Common instead of copying Argentum
   All Rights Reserved code. Cost: external compatibility, replaceability and
   source/license provenance must remain supported.
2. Select the actual downgraded-8/POM artifact despite richer Java17 Gradle
   metadata; suppress only optional upstream classfile annotation warnings.
   Cost: actual Java8 binary and class verification are required.
3. Enable the exact bqs GL2.1 capability path when O2 is ready and compatible,
   allowing Celeritas's real copy fallback. Cost: another pinned input and real
   compatibility evidence are required.
4. Track world/reload ownership and transform OptiFine -> O2 -> Opus. Null-world
   GPU cleanup runs after the host unload body. Cost: raw Minecraft pins and
   ordering contract, and storage release depends on successful host completion.
5. Preserve the production native-host activation guard; use a disposable,
   bounded test-only frame/task fixture after native accessibility failed.
   Dispatch/drain runs outside the producer monitor and retains pending tasks.
   Cost: automated evidence cannot substitute for physical GUI/input acceptance.
6. Add only the pinned terminated-owner integrated-server future guard after
   actual native thread dumps exposed abandoned work. Keep original Guava
   semantics and shutdown/pinger body, substitute only after successful cancel.
   Cost: two further exact inputs/owner-field contract and 50ms incomplete-wait
   polling; live/NEW/unknown owners retain the original waiting behavior.
7. Full Sodium meshing, entity instancing and Forge integration remain outside
   this renderer slice. Cost: narrower optimization coverage than a full
   renderer replacement.
8. Physical GUI/input, shader/AF/AA transitions, packs/dimensions and controlled
   FPS remain unverified. Cost: regressions in those paths remain possible until
   their own acceptance checks.
9. Retain exact local opus-optim branches and explicit opt-in. No installation,
   publishing or default enablement. Cost: users receive the candidate only
   through a separately authorized release step.
