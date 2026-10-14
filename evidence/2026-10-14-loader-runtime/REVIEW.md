# Managed Loader integration review

Fresh read-only independent reviewer: gpt-6-astra. Frozen Runtime8de907d..bb0f377,
O2170c7f8..f45c7c4, Launcher5b031c7..de10cde. Verdict **With fixes**, no Critical.
Reviewer inspected source, actual pinned LaunchWrapper bytecode and supplied
native receipts, without modifying files or running a game/suites.

## Important and one fix pass
LaunchWrapper1.12 catches adapter-constructor Exceptions in registerTransformer.
Missing selected services, invalid patch selections and helper initialization
errors could therefore remove the entire required nine-built-in+O2 adapter while
injection continued. Direct discover/constructor tests missed that boundary.

Two regressions inject OpusTweaker through a real LaunchClassLoader, with a
missing selected renderer service or a provider whose initialization rejects.
After correcting missing Log4j fixture dependencies, both were RED: expected an
exception but nothing was thrown. e983989 adds count/identity verification after
each required registration, outside the swallowing boundary, before loaderStarted.
Both are GREEN; full Runtime146 tests/check/package pass. Logs use test-only
console configuration, and only necessary dependency lock changes are retained.
Launcher813783f re-pins the newly built Runtime through the verified five-role
contract. This is an author fix pass with covering tests, not a re-review verdict. Post-fix native base1791975963930-15258 and selected
1791976067478-16150 both completed successfully; the selected workload verified
actual5patches/7renderer pins/96regions released/GL0 and clean logs.

## Minor deferred
OptifineEventHookTest requires .opus-launcher caches; existing Loader acceptance
tests require the macOS Minecraft installation path. Configurable shared fixture
paths remain a follow-up. Cost: otherwise valid developer setups must supply the
current two locations to run these acceptance fixtures.

## Execution rulings in order
1. Snapshot completed primary Loader sources into existing isolated Runtime
   opus-optim, retaining original/history. Cost: local snapshot is not delivered
   to shared branches until separately published.
2. Re-pin existing Launcher Runtime/native fingerprints from a complete,
   independently checked five-role build; retain native ABI/marker checks.
   Cost: future Runtime builds require explicit contract sync and Launcher rebuild.
3. Admit M5's one normal render RETURN only for exact bfk fingerprint
   493095da3dd17632fad71246e296649802ddd8036e0062f061ee9d2eb6f79d51,
   while retaining the original two-return contract. Actual M5 RED/GREEN and
   unrecognized-one-return rejection are verified. Cost: extra maintained pin
   and normal-return contract, not general arbitrary return acceptance.
4. FPS gain and base-world parity remain unverified. Base control is
   menu/clientREADY; selected workload proves execution/lifetimes. Cost: no
   comparative performance or world-parity conclusion.
5. Physical GUI, shader/pack/dimension and installation acceptance are separate.
   Cost: broader regressions remain possible outside automated disposable tests.
6. Forge-session acceptance is not inferred; managed O2 requires Standalone.
   Cost: this integration does not deliver managed O2 on Forge.
7. Latest guard execution is not claimed when it does not fire. Existing kernel
   and source-labeled earlier native proof stay distinct. Cost: current cohort
   does not itself prove a newly executed terminated-owner guard path.
