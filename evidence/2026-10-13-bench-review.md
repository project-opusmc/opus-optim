# O2 benchmark review and fix receipt

One independent read-only review covered `ddae124..373432f`. It found two Important
issues (malformed Unicode fingerprint collisions; unbounded evidence stream reads)
and two initially Minor numerical issues (permutation-dependent means; decimal
percentile boundary rounding). The executor re-graded the numerical findings because
the public statistics and screening verdict are core correctness contracts.

Five new regressions failed against the original behavior, then passed after fixes:
- Direct scope/source-revision Unicode and escaped JSONL surrogate rejection.
- Actual evidence-stream byte limit, including exact-limit acceptance.
- Identical frame multisets produce identical means and no performance signal.
- Decimal 7/14/28-percent nearest ranks over 100 samples.

Final `./gradlew clean test check assemble`: 69 tests, 0 failures/errors/skips.
Fresh local clone repeated the complete build; all three JAR hashes match the receipt.
All production classes remain major 52. JDK `1.8.0_502` artifact smoke passed raw
capture, JSONL read/write, stats, bound evidence parsing, synthetic comparison,
Unicode rejection and decimal nearest rank. Fixtures are synthetic, not game evidence.

All four findings were fixed in one pass; no independent re-review was performed.
No findings are deferred. Loader provider work is still uncommitted in the primary
Runtime checkout; this module does not consume those draft types or stage a game artifact.

## Rulings made during implementation/review

Ruling: execute the loader-independent measurement subset of the existing O2 benchmark roadmap now, under the owner's request to continue engineering — the Loader provider contract is still uncommitted, so depending on it would not produce a reproducible O2 build — cost if wrong: managed integration remains pending while the benchmark engine is built.
Task 3: Ruling: additionally bound the whole comparison to 1,000 runs/arm and 20,000,000 raw samples — per-report limits alone allow unbounded aggregate memory during CLI import — cost if wrong: unusually large experiments must be split or the reviewed limit raised.
Final: Ruling: re-grade summation-order false PERFORMANCE_SIGNAL from Minor to Important — permutation-invariant statistics are central to this engine's evidence contract even for extreme durations — cost if wrong: an additional numerical regression is maintained.
Final: Ruling: re-grade nearest-rank decimal boundary error from Minor to Important — the public percentile API promises exact nearest-rank statistics for valid inputs, not only the three CLI percentiles — cost if wrong: decimal rank calculation has additional offline computation cost.
Final: Ruling: retain the specified relative A/A noise model and explicitly document its stationarity/relative-noise-transfer assumption — changing the statistical model without actual benchmark protocol data would be a separate design change — cost if wrong: an unstable baseline can produce a misleading screening signal; a controlled runner must establish comparability.
Final: Ruling: source-revision-only changes remain distinct declared variants — source revision is part of the chosen identity model, while actual executed-code change belongs to mechanism/receipt review — cost if wrong: stale or mislabeled source identity can support a misleading signal without authentic external evidence.
Final: Ruling: origins, receipt contents and session independence remain supplied declarations — the engine verifies binding and declared-id uniqueness rather than authenticating game sessions — cost if wrong: forged metadata can mislead screening and cannot be used as reviewed game evidence.
Final: Ruling: retain error exit for stats on zero-frame interrupted reports — no distribution exists, while RunReports.read preserves the run and its lifecycle metadata — cost if wrong: users need the API to inspect a zero-frame run until a richer CLI inspection command exists.
Final: Ruling: game/Loader integration, real game gains, promotions and fresh manual acceptance remain pending — no such evidence was collected or claimed by this host-neutral engine — cost if wrong: the next integration/measurement milestone is still required.
