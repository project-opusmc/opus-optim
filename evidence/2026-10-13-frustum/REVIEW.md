# Frustum candidate review and resolution

Review date: 2026-10-14. Immutable range:
`87f406538d076961be8df1e27c2f6406886f118f..2e2a29cf2eb70127e95544450ef1bce2c52da58b`.
One fresh independent whole-change review; no second review after the author fix.
Original verdict: **not ready to merge until the application-state visibility
issue is fixed**. Critical: none. Important: one P2. Minor: none.

## Finding and author fix

The original `FrustumOptimization.binding()` discarded the only accessible
`FrustumPatch` handle. Through `ProviderPlan` the delegate is wrapped, so after
unknown `bid` input the caller could observe selected intent and unchanged bytes,
but could not query NOT_SEEN/APPLIED/UNSUPPORTED_INPUT. Bytecode rejection was
safe; the advertised application-reporting contract was incomplete.

Replaced this factory with `FrustumOptimization.candidate()`, a per-instance
handle retaining the patch and its immutable binding. The host passes
`candidate.binding()` to preparation and reads `candidate.applicationState()`
after registered transformation. There is no global status. Immutable selected
intent remains separate from the actual bytecode outcome. APPLIED proves only
rewritten bytes, not class definition, game use or performance acceptance.

RED: a factory-to-planner-to-registration regression failed compilation because
Candidate/candidate() did not exist. GREEN: the regression observes NOT_SEEN on
non-target input, UNSUPPORTED_INPUT on target refusal, and an independent second
candidate still NOT_SEEN. The actual Java 8 M5 harness additionally proves APPLIED
through that same factory/registration and then observable rejection of already
modified input. Full author tests/check/assemble and both Java 8 smokes passed.
Final fresh-clone counts/hashes are in `verification.json`.

This fix changes the status factory and verification setup; FrustumMath and
FrustumPatch bytecode are unchanged from the measured revision. The final fresh
verification checks their hashes against the frozen pre-fix build. No timing
forks were rerun after the status fix, and no new performance claim is made.

## Reviewer's verification and assessment

The reviewer independently reported:

- Fresh export passed 103 tests, full test/check/assemble, Java 8 bridge smoke
  and 121,211 real M5 parity cases with zero mismatch.
- Additional real M5 malformed-matrix/disabled probe: 24 cases passed.
- Systematic ordered-edge probe: 218,297,364 combinations, including 45,664,810
  rejection cases, without a hidden successful corner.
- All 156 stored raw fork reports recomputed consistently: checksums, identities,
  fork medians and summary percentages. Fresh helper hash matched measured code.
- Review package matched the Git range; diff whitespace check passed. Checkout,
  index, HEAD and branches were unchanged by the review.

The review accepted float operation order, first-corner and NaN success,
disabled/full behavior, ordered supporting rejection, exact-pin refusal,
equivalent direct-call benchmark adapters, unfavorable evidence retention,
separate compilation scopes and promotion refusal. It confirmed compile-only
ASM and absence of bundled ASM/Loader/game classes. These are reviewer-reported
checks; the numerical probe results are not game acceptance or FPS evidence.

The reviewer recommended fixing status exposure and retaining EXPERIMENTAL,
explicit enable and refusal to promote because of the inside regression.
The author applied that single fix pass and reran direct verification. The
original review verdict is retained above rather than rewritten as a fresh PASS.

## Declined judgments retained

1. Managed Loader/Launcher integration, installation, physical visual parity,
   game FPS and game instrumentation overhead: absent and unclaimed.
2. Optimization behavior for vanilla/remapped/other vendors and shader
   overrides: outside the pinned contract; refusal behavior was reviewed.
3. Concurrent matrix/reference/row mutation: excluded by the stable render-thread
   contract.
4. Other JVMs, architectures and gameplay workloads: only local Java 8 x86_64
   query evidence. Stored timing calculations were verified, not all timing
   forks freshly reproduced or statistical significance established.
5. Unrelated pre-existing foundation behavior: review covered its interactions
   with this candidate while rerunning the full suite.

These limits remain open integration/measurement work, not assumed approvals.

## Decision ledger preserved

1. Implement the priced raw M5 candidate under the existing engineering request
   after the user corrected infrastructure-only progress. Cost if scope differs:
   local reversible commits.
2. Start with an exact M5 pin, bounded float fast path and exact fallback to
   preserve profile/NaN behavior. Cost: other profiles remain unsupported.
   This initial math design was superseded by the measured adjustment below.
3. Use adaptive first/supporting corners, ordered rejection and exact unordered
   fallback after range-guard overhead erased savings. Cost if proof/scope is
   wrong: culling parity could fail; EXPERIMENTAL and game parity stay required.
4. Refuse promotion and keep explicit EXPERIMENTAL because local mixed/outside
   reductions accompany an inside regression and absent game evidence. Cost:
   this is not a recommended/default game optimization yet.
5. Replace the status-hiding convenience factory with a per-candidate public
   handle. Existing candidate consumers must use candidate().binding() and retain
   the handle. Cost: a local unreleased API call changes; no managed integration
   exists to migrate. Do not change immutable planner intent or measured math.

No minor findings were deferred. Exact branch `opus-optim`, shared license,
Claude's primary checkout, published superproject gitlink and local-only commits
remain the standing user-directed boundaries.
