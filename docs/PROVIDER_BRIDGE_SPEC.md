# O2 provider bridge and frame callback

This implements a locally testable bridge, against the explicitly archived
Loader draft of 2026-10-13. It does not install O2 in the managed client.
The published Runtime revision `3471487` lacks this provider API. The draft
rejects empty approved providers and its default adapters approve no provider.
Those host changes and a pinned managed artifact/classpath contract are required
before a real game baseline. Keep Runtime's five current artifact roles.

## Startup provider

`platform-opus` is the only O2 module permitted to refer to Loader types.
Archived Loader sources compile into a separate fixture source set: never into
the platform JAR. Source hashes are checked on every build; Java release 8,
the common Opus license and branch `opus-optim` remain required.

`TransformerBinding` binds one trusted STARTUP_PATCH descriptor to a transformer
of the same id and explicitly declared injected helper classes. Claims must be
nonempty and name exact raw target classes/methods/anchors supplied by the host.
No guessed mappings, optimization candidate, renderer or GL probe is added.

`ProviderPlan.prepare(config, capabilities, recovery, candidates, baseline,
gameLoader)` plans the candidate catalogue using existing core eligibility and
baseline claim compatibility. Missing/disabled/SAFE config and recovery retain
their exclusion reasons. Only selected candidates become registrations for
provider `opus.optim`. OFF contributes zero registrations and checks no helpers.
Runtime policies/backends are rejected as unsupported bindings rather than
reported as installed but inactive.

Before installation, verify selected order against Loader's actual priority/id
sort; verify composed baseline/candidate claim order against that same sort;
reject duplicate ids, and require every selected helper to resolve to the same
Class object through the supplied game loader without initializing the helper or
loading a target game class. Descriptor/claim metadata are trusted declarations;
the host remains responsible for actual mappings, anchors and composition receipts.
Returned wrappers freeze id/priority/target metadata, filter exact targets and
delegate bytecode work. Registrations and target sets are immutable.

`OptimBootstrap.install(plan)` installs once. The service provider has a no-arg
constructor with no config I/O or game/GL initialization. Its first approved
`registrations()` call seals the snapshot, including an unconfigured OFF snapshot.
Repeated reads are stable; installation after sealing or a second installation
fails and requires restart. The host must perform this before class definition;
the O2 bridge cannot independently inspect JVM class-definition history.

## Frame callback

`FrameTimingBridge.start(identity, scope, warmup, capacity, gameLoader)` creates
and attaches exactly one capture session, after checking shared helper identity.
`frameCompleted(durationNanos)` accepts the existing telemetry frame wall interval;
it does not add another transformer, clock read, game reference, GL call or I/O.
With no capture attached it is a no-op. Benchmark instrumentation is separate
from optimizer enablement so an optimizer-OFF baseline can still be measured.

The callback and session finish synchronize to protect the single-writer sampler.
The first callback owns the session's writer thread. Nonpositive samples or a
different writer invalidate the session: callbacks cease recording, game callers
do not receive a diagnostic validation exception, and requested COMPLETE is
downgraded to INTERRUPTED with a separately accessible reason. Warmup and overflow
use the existing bounded FrameCapture rules. Finishing seals/detaches the session;
duplicate finish fails. No callback clears a crash marker. Report output remains
an explicit `RunReports.writeNew` call outside the frame callback.

## Acceptance and handoff

Exercise real ServiceLoader discovery, actual pinned draft TransformerPlan,
synthetic ASM bytecode outside vanilla target lists, executable injected helper
visibility across distinct classloaders, actual ordering, OFF/SAFE/recovery,
late/double registration, and callback concurrency/failure/report roundtrip.
Explicitly characterize the draft's rejection of an approved empty O2 provider.
Artifact verification rejects bundled Loader/ASM/game classes and checks the SPI
entry; pure API/core/bench keep their existing boundary guard.

Leave a concrete Loader handoff and baseline protocol. There is no real game
sample, measured overhead, parity acceptance or performance gain from these
fixtures. Do not push local work or advance the superproject's published gitlink.
