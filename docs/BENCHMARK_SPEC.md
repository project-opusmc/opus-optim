# O2 benchmark engine v1

This implements the host-neutral measurement subset of the existing Opus Optim
benchmark design. Loader provider integration and controlled game scenarios are
separate work. The owner requested continued O2 engineering on `opus-optim`.

## Input and capture

`:bench` depends on the pure API and Gson, targets Java 8, and runs the same
forbidden game/Loader/GL boundary guard. No new game artifact is staged.
`FrameCapture` preallocates a bounded primitive array, discards a declared number
of warmup frames, and counts overflow as dropped samples rather than overwriting
old observations. Durations are positive nanoseconds of wall frame duration;
they cannot be presented as CPU or GPU cost. Finish seals the single-writer
capture and returns an immutable run. Interrupted/incomplete runs remain visible.

Scope requires scenario, fixture, Minecraft, loader, client artifact, Java,
OS, CPU, GPU, GL, resource-pack, settings, instrumentation and sampling identities.
All scope entries are compared; sampling is `frame.wall.nanos.v1`. Variant identity
records id, source revision, Optim artifact SHA-256 (or explicit `absent`), and
config SHA-256. Run and process-session ids identify independent replicates;
these are declarations, not proof of independence. Origin distinguishes recorded
data from synthetic fixtures. No account/server/input contents belong in reports.

Limits: 1,000,000 samples/run, 32 scope fields with values up to 256 characters,
64 counters, 64 MiB/report and 16,384 characters/JSONL record. A comparison is
bounded to 1,000 runs/arm and 20,000,000 total samples. Reports are local.
JSONL v1 has one run header, frame records, then one end record with sample,
observed/dropped counts, termination and supplied measured counters. A missing
footer, duplicate JSON keys, unsupported version, wrong types, inconsistent count
or trailing records is invalid. Writing uses CREATE_NEW and never overwrites.

## Exact distributions

Percentiles use nearest rank over the entire raw run, not window summaries.
1% low FPS = 1e9 / mean nanoseconds of the slowest ceil(N/100) frames, requiring
at least 100 frames. 0.1% low uses ceil(N/1000), requiring 1,000 frames. Unsupported
lows are absent. Positive long durations are averaged without integer sum overflow.

## Independent-run comparison

Four arms: A/A control and repeat, A/B control and treatment. Each needs at least
five distinct process-session replicates and at least 1,000 raw frames/run, completed
warmup, COMPLETE termination and zero dropped samples. No session or run id may be
reused across arms. A/A variants and A/B control must have identical variant identity;
treatment identity must be internally consistent and distinct. Scope and warmup
policy must match across all arms.

Run means have equal weight regardless of run length. Also compare independent-run
p95 and p99 values, explicitly labelled as run-level estimates rather than pooled
percentiles. Difference intervals use unequal-variance standard errors and a
conservative Student t critical value with min(nA-1,nB-1) degrees of freedom capped
at 30. Values are rounded upward from the NIST 0.975 table. These are nominal
95% intervals for independent approximately normal run aggregates; they are not
distribution-free or simultaneous guarantees across all gates.

A/A noise floor = (abs(mean difference) + uncertainty margin) / A/A control mean.
A/B effect and interval are normalized by the observed A/B control mean; this is
an interval for the absolute mean difference in control-mean units, not an exact
confidence interval for a ratio. Mean improvement's lower bound must exceed A/A
noise. Tail regressions beyond A/A noise reject; uncertain tails stay inconclusive.

Evidence references bind scope, control and treatment fingerprints and declare
parity, measured mechanism and compatibility PASS/FAIL/MISSING with SHA-256 receipts.
The engine checks binding and completeness, not receipt authenticity or contents.
Synthetic data cannot yield PERFORMANCE_SIGNAL. Verdicts are INVALID, REJECTED,
INCONCLUSIVE, PERFORMANCE_SIGNAL. None changes OptimizationStatus or enables a
feature; actual promotion still needs reviewed game and lifecycle evidence.

## CLI

`stats RUN.jsonl` prints exact run statistics. `compare AA_CONTROL_DIR AA_REPEAT_DIR
AB_CONTROL_DIR AB_TREATMENT_DIR EVIDENCE.json` loads all `.jsonl` files in sorted
order and prints a comparison. Exit codes: 0 stats/signal, 2 invalid input,
3 inconclusive, 4 rejected. It writes no output file or external data.

References: [NIST difference-of-means intervals](https://itl.nist.gov/div898/software/dataplot/refman1/auxillar/diffmean.htm),
[NIST Student t table](https://www.itl.nist.gov/div898/handbook/eda/section3/eda3672.htm).
