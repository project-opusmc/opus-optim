# O2 immutable resource-string candidate

`runtime.resource_string_cache` is implemented as an independent exact-pin
startup candidate. It is EXPERIMENTAL and OFF without its flag/capabilities.
Unknown or modified raw bytes are unchanged with observable per-candidate
refusal. No managed install, default promotion, FPS gain or suite completion.

The pinned full M5 ResourceLocation class has final domain/path Strings; every
original toString() creates its result again. The patch adds a private volatile
per-instance cache, preserving the original cold body and constructor behavior.
Warm identity reuse is deliberate. Cold concurrent races can compute duplicates
with identical content; this is not single-flight or a global intern table.

On actual Java8, the complete original/patched raw classes load with the actual
commons-lang3 3.3.2 dependency. 20,043 content/constructor cases and 80,000
concurrent reads passed; equals/hashCode, normalization, subclass dispatch,
warm reuse, public application/refusal states and unmodified method hashes were
verified. This is a full class test, not a running game/resource-pack session.

## Forty independent JVM forks

Five per A/A-control, A/A-repeat, A/B-control and A/B-treatment arm for warm/cold
workloads. 4,096 deterministic resource ids. Each fork has 2,621,440 warmup queries
and eight samples of 262,144 queries. Generated factory adapter calls the real
constructor directly; there is no reflection in timed loops. Query consumption
uses length/first/last chars with independently checked fixture checksums.

Warm: construct and prime resources outside timing, then repeat toString().
Cold: construct a fresh resource and call toString() once inside every query.
These are distinct workloads; cold includes constructor cost.

| Workload | Control median ns/query | Treatment | A/B reduction | A/A shift |
|---|---:|---:|---:|---:|
| Warm | 14.37 | 3.59 | 74.99% | 5.43% |
| Cold | 17.34 | 15.32 | 11.61% | -6.57% |

Medians are medians of per-fork sample medians. These observed local point
effects are not statistical-significance or game conclusions; all variation is
retained in microbench. No confidence intervals or registry promotion.

Thread allocation counters measure a different tradeoff:

| Workload | Control B/query | Treatment B/query | Difference |
|---|---:|---:|---:|
| Warm | 83.24 | 0.00018 | repeated allocation nearly removed |
| Cold | 82.49 | 106.49 | **+24 B/query (+29.09%)** |

The actual Java8 ThreadMXBean allocation counter is enabled only in these
temporary JVMs. Two untimed reflective counter reads add a small fixed per-sample
allocation cost, explicitly included in the raw numbers. No live-heap footprint
or precise per-object size is measured. Each class instance has an added reference
field and queried instances retain a String until the resource object is collected.
Therefore lower repeated allocation is not a claim of lower memory usage.

## Decision and outstanding work

Keep OFF/EXPERIMENTAL. Cold allocation and retained live heap need actual resource
hit-rate/lifetime/reload measurements before recommendation. Zip scan, decode,
upload, pack invalidation and runtime leaks are still separate engineering work.
Independent review is pending: the requested reviewer hit a usage limit without
findings/verdict. Author checks are not labeled an independent PASS.

See full-class-verification.json, all 40 raw fork files and microbench/summary.json.
