# Loader integration handoff for O2

The platform bridge is locally executable against the archived draft described
in `platform-opus/LOADER_CONTRACT.md`. It is not in managed Launcher staging.
The published Runtime base `3471487` lacks provider types; both draft adapters'
default constructors approve an empty set. An approved OFF O2 provider currently
fails draft TransformerPlan discovery because it returns zero registrations.
`ServiceProviderTest` deliberately preserves and demonstrates that failure.

## Required host changes before integration

1. Publish/pin Loader's provider/mapping contract and replace O2's compile-only
   fixture with that exact API artifact. Do not ship the archived fixture classes.
   In `TransformerPlan.discover`, accept an empty nonnull registration list from
   an approved provider; still reject null lists, mismatched provider ids, invalid
   target declarations and duplicate provider/transformer ids. Verify OFF retains
   9 Standalone / 8 Forge built-ins using the real adapters.
2. Supply the approved `opus.optim` id and its provider loader from verified
   managed launch inputs. A Java system property alone is not the managed approval
   boundary. Make O2 API/core/bench/platform and their existing Gson dependency
   visible to both the provider and game loaders with shared helper identities.
   If adding an artifact role, update manifest, release-lock, Launcher validation,
   staging and fixtures together; these commits change none of those contracts.
3. Before approved discovery or any target definition, create trusted bindings
   from actual mapped raw classes/methods/anchors, and describe the actual built-in
   transformers as baseline bindings with their reviewed composition receipts.
   Call `ProviderPlan.prepare(...)`, inspect its exclusions, then
   `OptimBootstrap.install(plan)` in the SAME provider classloader that SPI will
   use. No optimization binding is shipped by this bridge; the test probe is a
   synthetic fixture. The host must check the bytecode shape and receipts for each
   future candidate, and forbid later installation. Do not mark client-ready or
   first-frame as clean shutdown; preserve crash-marker semantics.
4. Add a host-neutral observer to `ClientTelemetry`, with an explicit managed
   attach/detach lifecycle. Common Runtime must not import O2. The managed platform
   connects the observer to `FrameTimingBridge.frameCompleted(durationNanos)`.
   `ClientTelemetry.frameFinished()` already measures `now - started`; forward
   that exact raw interval after resetting its start slot. Do not reconstruct raw
   frames from TimingWindow summaries. Ensure frameStarted/frameFinished run for
   an attached observer even when the diagnostics file writer is disabled;
   current `ENABLED` guards otherwise produce no frames. Preserve existing
   diagnostic windows/logging when diagnostics are enabled. Reject multiple
   observers, contain/report observer failures and downgrade the capture to
   INTERRUPTED if a hook/observer/session becomes incomplete. No reflection,
   method lookup, JSON serialization, file I/O or additional timer per frame.
5. On an explicitly completed scenario, finish the session and write its new
   report outside the render loop. Persist `failureReason()` alongside the run
   receipt; failed/interrupted captures cannot become baseline evidence. Process
   crashes, unhandled game-loop exits, detached/missing callbacks and user aborts
   are interrupted runs. JVM shutdown hooks alone do not prove a clean scenario
   or clean launch recovery. Validate callback/helper visibility and target
   routing with both real adapters and distinct parent/game classloaders.

## Controlled baseline protocol

Record the exact Minecraft/Loader/client/O2 revisions and artifact/config hashes,
Java/OS/CPU/GPU/GL, settings/resource-pack hashes, scenario/fixture identity,
sampling policy `frame.wall.nanos.v1` and instrumentation revision. Probe GL only
on a valid render-thread context after initialization; the bridge does no probe.
Use a controlled disposable game/settings copy rather than rewriting user
preferences or enabling saved HUD modules. Observe the actual launched process
and artifact identity; never print account/authentication command arguments.

First verify a complete optimizer-OFF run: zero O2 registrations, normal built-ins,
and explicit frame observer instrumentation. Then measure the observer's overhead
with controlled instrumentation-disabled/enabled A/A sessions, using an independent
measurement channel while the observer is disabled. The existing window summaries
cannot supply the disabled arm's exact raw lows. Keep overhead results distinct
from optimizer effects; do not call synthetic or partial data a real baseline.

For every candidate, run matched A/A control/repeat and A/B control/treatment
arms with at least five independent process sessions per arm, at least 1,000
complete undropped measured frames per run, matching warmup/capacity/scope, and
balanced randomized order to control thermal/background drift. VSync/FPS limiting
and scene/load must be explicitly controlled and recorded. Keep raw wall-frame
time separate from CPU-only, allocation or GPU counters; supply only actually
measured counters. Review parity, mechanism and compatibility receipts before
comparison. A comparator PERFORMANCE_SIGNAL does not enable/promote a feature.

## Current evidence boundary

O2 unit/SPI/ASM/classloader/JDK8 checks are fixtures. No Standalone/Forge live
adapter session, Loader-managed O2 approval, telemetry observer wiring, controlled
game baseline, A/A instrumentation overhead, game parity or FPS gain is claimed.
