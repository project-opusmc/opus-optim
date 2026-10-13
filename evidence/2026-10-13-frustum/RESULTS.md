# First O2 optimization candidate: frustum corner tests

This is an implemented bytecode optimization, still EXPERIMENTAL and OFF without
an explicit flag. It accepts only the pinned raw Minecraft 1.8.9 + OptiFine M5
ClippingHelper class. It has not been staged or run in a managed game session.

The patch preserves the original first-corner fast success, then uses the
supporting corner to settle ordered boxes instead of testing all eight corners.
Unordered endpoints retain corner enumeration. Float/NaN behavior, disabled
bypass and fully-contained predicate are preserved. The real local M5 class
passed 121,211 original-versus-patched cases on Java 8 with zero mismatch.

## Measured query cost

Each scope has 60 independent JVM forks: 5 per A/A-control, A/A-repeat,
A/B-control and A/B-treatment arm for each of three workloads. Each fork has
10,485,760 warmup queries and ten samples of 1,048,576 timed queries. Timed loops
use generated direct-call adapters and observable checksums, with no reflection.
All reports/initial unfavorable prototypes are retained. Medians below are
medians of per-fork sample medians, not game frame statistics.

| JVM scope | Workload | A/B cost reduction | A/A shift | Interpretation |
|---|---|---:|---:|---|
| ordinary Java8 | inside | 0.63% | 0.74% | within observed variation |
| ordinary Java8 | mixed chunks | 8.54% | -32.52% | inconclusive, large baseline variation |
| ordinary Java8 | outside | 9.84% | -5.35% | observed local query reduction |
| Java8 with `-Xbatch` | inside | -3.48% | 0.15% | regression |
| Java8 with `-Xbatch` | mixed chunks | 6.92% | 0.20% | observed local query reduction |
| Java8 with `-Xbatch` | outside | 10.09% | -0.04% | observed local query reduction |

`-Xbatch` controls compilation scheduling and is a separate experiment scope,
not a proposed managed launch flag. Original query medians are roughly 5–6 ns;
these tiny isolated costs do not establish that frustum math is a material frame
bottleneck. No confidence interval, authenticated session proof, game FPS gain,
physical visual parity or game instrumentation overhead is claimed here.

## Decision

**Promotion refused.** The inside regression and missing managed game evidence
prevent VALIDATED/DEFAULT. The explicit EXPERIMENTAL binding remains available
for controlled integration research; it must not be enabled automatically.
Both favorable and unfavorable findings matter. This is a real first candidate
with query-level measurement, not completion of game optimization acceptance.

See `receipt.json`, both microbench summaries and all raw fork JSON files for
scope identities, helper/source hashes, checksums and raw elapsed timings.
The [review and resolution](REVIEW.md) retain the independent review's finding,
single author fix, declined judgments and decision ledger. Final fresh-clone
verification is recorded separately in `verification.json`.
