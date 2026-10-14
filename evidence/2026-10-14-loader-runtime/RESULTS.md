# O2 through completed Opus Loader

The current implementation uses Loader-owned managed providers and one game
classloader/ordered transform chain. Custom Celeritas region rendering runs at
priority0 before built-ins105+; implemented startup patches400 run afterward.
Raw provider targets and mapped built-in GUI routing remain distinct. O2 is
absent by default; missing required registration stops startup.

O2 sourcef45c7c4; Runtime productione983989; Launcher813783f. Runtime retains five
roles; O2 schema2 contains eight separate JARs, their integrity receipts and
separate license/source notices. No O2 tweaker, bundled Loader contract classes,
workflow, installation, push or published root gitlink change. Own Opus LICENSE
is byte-identical. Primary Claude source remains untouched, with32-file snapshot
hash receipt in Runtime docs/optim-integration/loader-snapshot.json.

O2:143 tests; Runtime:146 tests (including historical unbundled M1 modules);
engine71,CLI2,developer example3 and contract-sync negative test1 pass. One
official-network test is explicitly ignored. Runtime check/package verification,
O2 checks/fresh bundle/Java8 boundary, real-bootstrap Java8 synthetic SPI smoke,
seven complete renderer-class verification, Launcher clippy and desktop check
have separate recorded logs. Synthetic target execution is not game acceptance.

Pre-fix source-labeled native proof: base1791974937720-7355 reached clientREADY
and normal native exit0 with O2 absent. Selected1791975088865-8559 applied actual
five startup patches (rawjy/avn/bfd/bid/bho), plus all seven renderer pins, via
opus.optim5 and opus.optim.renderer1 registrations. Its terrain/reload/VBO
OFF/ON/unload/reopen completed96 created/96 deleted/0live, GL0, no NPE/probeFAILED,
nativehost0/strict example0. Its eight staged JARs were independently rehashed
and matched the built bundle; screenshot inspected. These receipts belong to
Runtimee2b69fb production/Launcherde10cde, before the final registration fix.

Fresh independent review returned With fixes. The Important swallowed adapter
registration failure is fixed with real negative injection RED/GREEN tests;
see REVIEW.md. Post-fix base1791975963930-15258 again reached clientREADY with O2 absent;
selected1791976067478-16150 again applied all five patches/seven renderer pins
and completed96/96/0, GL0, zero NPE/probe failures. Both normal native/strict
verdicts exited0 on Runtimee983989/Launcher813783f. Their current-native logs and
verification.json are separate from the earlier pre-fix receipts. No old session is
silently reassigned to a new Runtime cohort.

Earlier rejected sessions remain provenance:1791965548206-59237 failed old
Launcher artifact pins before game;1791966009460-61814 failed before window when
Loader assumed two render returns but actual pinned M5 has one. Both issues were
reproduced and fixed with explicit contracts, not validation bypasses.

The base control is startup/menu/client readiness, not a comparative world/FPS
baseline. Shader/pack/dimension, physical GUI, Forge-session, controlled FPS and
installation remain unverified. Full Sodium CPU meshing and entity instancing
are outside this integration. Configurable fixture paths are the deferred minor.
Raw logs/helper capability URLs remain private; committed logs contain only
sanitized O2/Loader state.
