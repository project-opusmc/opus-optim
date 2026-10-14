# Pipeline/resource review status and author checks

Independent review was requested once for immutable range
6c0d580..f1f66574b3f1f2581421104a5b86e60fa4e4b26b. The reviewer terminated at
its usage limit before returning findings or a verdict. Status: **PENDING,
NO INDEPENDENT VERDICT**. No independent zero-finding, ready-to-merge or PASS
assessment is inferred. The later resource increment was not independently reviewed.

Author verification continued under the owner's explicit instruction to continue.
It found an Important defect in the earlier font patch: replacing all glyph
alphabet LDC loads also changed String.length/charAt receivers in obfuscated
text to short[]. The complete raw M5 font class failed Java8 verification.
The original isolated width kernel did not exercise that body. Those v1 raw
timing reports remain historical width-query data, not renderer acceptance.

Fix: preserve the String literal/type, remove that receiver only at its exact
indexOf call, then call the table lookup. The owned draw-method regression
observed VerifyError before the fix and passed after it. The complete raw font
class definition/clinit now also passes, retaining 256 original Unicode page
slots and initializing a 19,266-byte lookup-table payload. All 40 timing forks
were rerun as v2 because emitted target bytes changed. See probes and v2 reports.

Author full tests and actual Java8 verifiers are recorded separately. They do
not replace the missing independent review. No minor review findings were
received or silently deferred; severity counts for independent review are
unavailable, not zero. No default promotion, host installation, push or merge.

## Decisions retained

1. Continue concrete pipeline patches under standing engineering authorization,
   rather than request the same authorization again. Cost if scope differs:
   reversible, locally committed opt-in candidates.
2. Accept host-supplied exact routing aliases for the same raw pin because
   standalone maps raw avn to the FontRenderer routing identity. Cost if the
   host supplies an irrelevant alias: an extra attempted routing; the exact
   digest/bytecode-owner gate still refuses other bytes. No remapped support.
3. Continue immutable resource caching while independent review is unavailable,
   as the owner explicitly requested. Cost if live hit-rate is low: extra cold
   allocation/retained heap. Keep its flag OFF, disclose that measured cost and
   retain the publication/promotion gate.
4. Withdraw v1 renderer validity, fix the overbroad alphabet substitution and
   rerun timing. Kernel correctness alone did not establish whole-class validity.
   Cost: additional verification/forks; v1 evidence stays preserved and labeled.

## Limits retained by author assessment

- Full font construction and actual GL/style/pack/shader rendering remain
  unverified; raw definition/clinit and owned method kernels are narrower.
- Resource full-class tests are actual code with real commons-lang3, but not
  pack/reload lifecycle, resource hit rate or retained-heap measurements.
- Buffer and upload kernel dependencies are owned substitutes; native GPU
  upload cost, game queue latency, memory footprint and visual parity remain open.
- The upload cap is per call and cannot preempt a long task or bound a whole
  frame making multiple calls. The buffer cap bounds speculative headroom,
  not required allocations or aggregate live memory across all renderers.
- Unknown/remapped/other vendors and unsafe mutation are outside pinned
  contracts. Public refusal/status, OFF/flag/capability behavior are verified.
- Local Java8 x86_64 timing point estimates, even with A/A, are not other JVM/
  hardware conclusions, statistical significance, game FPS or default acceptance.
- Loader approval/observer/staging and the broader suite's batching/compile/
  resource pipeline remain incomplete. Keep source-backed boundaries separate.
