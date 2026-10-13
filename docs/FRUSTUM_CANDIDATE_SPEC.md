# O2 positive-vertex frustum candidate

Goal: ship the first actual optimization implementation. Replace eight-corner
frustum rejection with an adaptive first/supporting-corner test on each
plane, preserving OptiFine M5's float arithmetic and edge behavior. The candidate
is EXPERIMENTAL, explicitly enabled, and not installed in the managed game yet.

## Priced scope

Minecraft 1.8.9 + imported OptiFine 1.8.9 HD U M5, raw Notch `bid.class`, field
`a:[[F`, method `b(DDDDDD)Z`, and `disabled:Z`. Exact input SHA-256:
`85b5a3893dfa826880a9e6e5a1221ac4443c1d778e38c384ef715911ababa7c4`.
Vanilla, remapped Forge, shaders' helper subclass overrides and other patched
bytecode are not silently accepted. Read local vendor bytes for verification;
never commit or bundle Minecraft/OptiFine classes.

Baseline bytecode pricing on 4,096 boxes per controlled workload, Java 8:
inside: 6.000 dot evaluations/query; mixed chunks: 9.875; outside: 9.000.
One-corner mechanism lower bounds from actual entered planes: 6.000, 3.934,
2.000 respectively. This shows an opportunity, not measured FPS or global gain.

## Algorithm and behavior

Cast the six double coordinates to float once, exactly as M5. Try its original
first corner first, preserving the cheapest inside path. If it fails, choose the
high endpoint for nonnegative coefficients and low for negative coefficients,
and evaluate `(a*x + b*y) + c*z + d` in float. A success is always sound: this is
one of the eight original corners. For ordered endpoints, this is the supporting
corner; when both tests fail, all corners fail. Unordered/NaN endpoints retain
the original remaining seven-corner enumeration. No epsilon, FMA,
distance/visibility heuristic or allocation is added.

Preserve M5's NaN-accepting comparison (`!(dot <= 0)`) on BOTH tests: testing only
the supporting corner could hide a NaN success at the original first corner.
Positive infinities/NaNs from products, sums or overflow are already success
cases in M5; an ordered supporting result <=0 cannot hide a larger finite/positive
corner. Reversed/NaN endpoint orders are explicitly checked before rejection.
Biased IEEE testing covers this rule; unit/reference and real-bytecode parity
remain acceptance requirements, not game visual proof. Preserve touching plane
semantics, sequential access/early return for
malformed arrays, `disabled` bypass even with null matrix, and unchanged
`isBoxInFrustumFully`. Require a stable matrix reference and plane rows on the render thread, as in host
culling; concurrent mutation is outside this candidate's contract.

`FrustumPatch` verifies the exact input digest before rewriting the one method.
Unknown input is returned by identity and exposed as UNSUPPORTED_INPUT; it cannot
be reported as an applied optimization. Non-target classes are unchanged.
`FrustumOptimization.candidate()` supplies a per-instance handle for id `render.frustum.positive-vertex`,
EXPERIMENTAL descriptor, exact behavior claim, priority 400, helper identity and
required capabilities `mc189.frustum.optifine-m5.notch` and `loader.asm5`.
OFF, absent flag, missing capabilities and experimental recovery contribute no
registrations through the existing planner. The host must attest the actual
target resource and compatible ASM before installation, then report application
state; no managed Loader/Launcher changes or artifact-role changes in this task.
Retain the candidate, supply its `binding()` to the planner, and query its
`applicationState()` after the registered transformer runs. The handle observes
NOT_SEEN, APPLIED and UNSUPPORTED_INPUT without a global/shared status. A selected
plan is immutable startup intent; digest refusal does not rewrite that plan.
APPLIED means bytes were rewritten, not that the class was defined or used.

ASM 5.0.3 is compile-only for this platform transformer, already host-owned.
Allow references to ASM in platform classes, still reject bundling its classes
or any Loader/game/GL classes. Pure api/core/bench guards remain unchanged.

## Measured design adjustment

The first bounded one-corner version reduced interpreted dot counts but was
slower on all three preliminary workloads because range checks outweighed the
work saved. An adaptive version retaining those range checks also regressed on
mixed/outside queries. The final rule removes redundant magnitude guards after
preserving both actual-corner success predicates and checking endpoint order.
Three million biased IEEE one-plane probe cases matched exhaustive enumeration;
the permanent suite adds 300,000 such cases. Final repeated timings, rather than
operation-count guesses, determine the local result.

## Proof and limits

Use an independent eight-corner oracle for unit parity, IEEE edge values,
float-rounding boundaries and malformed matrices. Execute the transformed real
local OptiFine class on Java 8, compare raw results to its original on seeded
random cases, verify disabled/full-frustum behavior and pin rejection.

Measure original versus transformed bytecode with generated direct-call adapters
(no reflection in timed loops), warmup, checksums, repeated independent JVM forks,
A/A controls and interleaved A/B. Cover inside/mixed/outside workloads; record
per-query CPU-wall throughput, not game frame timings. Reject promotion for
workload regressions or missing game parity/overhead/compatibility evidence.
Even a positive local microbenchmark leaves status EXPERIMENTAL, game FPS gain
unclaimed and the feature OFF by default until managed integration is verified.

Keep exact branch `opus-optim`, common Opus license, Claude's primary checkout,
five managed roles and published gitlink. Keep commits local.
