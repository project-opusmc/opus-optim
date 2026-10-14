# O2 Celeritas renderer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan inline without intermediate approval prompts, as the user explicitly requested continuous engineering.

**Goal:** Wire a real Celeritas-backed region renderer through O2 and the Standard launch path.
**Architecture:** A separate Java8 module links the published Celeritas Common binary. Pinned LaunchWrapper hooks replace OptiFine region storage/submission; a checked bundle extends the standalone Launcher plan.
**Tech Stack:** Java8, ASM5, LaunchWrapper1.12, LWJGL2, Celeritas Common2.5.0-pre.1 downgraded-8, Rust Launcher.
**Spec:** docs/superpowers/specs/2026-10-14-opus-optim-renderer-design.md

## Global Constraints
- Exact branch `opus-optim`; preserve dirty primary Runtime.
- Own code retains Opus LICENSE; external Celeritas remains separately LGPL.
- No push, workflow, published gitlink change or unsupported performance claim.
- Renderer requires explicit `opus.optim.renderer=celeritas-region`.

## Review Focus
- Classloader identity: game helpers must share the actual LWJGL2 context.
- Relocation: queued draws use live segment offsets after arena growth.
- Lifetimes: delete/empty/reupload must release native and GPU memory.
- Runtime option changes: shaders and multitexture must retain their original path.
- Manifest tampering or partial staging must prevent enabling the bundle.

### Task 1: Celeritas module and GL arena adapter
Files: renderer-celeritas/build.gradle.kts; renderer-celeritas/src/main/java/.../RegionRenderer.java; tests and actual GL smoke.
Produces: upload(Object range, ByteBuffer data), queue(int mode,Object range), finish(Runnable pointers), close().
- [x] Write lifecycle/order/reupload/relocation tests; run RED.
- [x] Implement actual Celeritas arena integration and bounded ordered multi-draw queue.
- [x] Run unit checks and real OpenGL upload/readback/pixel/lifetime checks on Java8.
- [x] Commit tested module and dependency/license bundle verification.

### Task 2: Pinned runtime hooks and LaunchWrapper entry
Files: RendererHooks.java, RendererTransformer.java, OptimRendererTweaker.java, hook tests, extracted-class smoke.
Consumes RegionRenderer; produces LaunchWrapper registration, explicit selection, target receipts.
- [x] Write OFF/unknown/fingerprint/method-shape/loader tests; run RED.
- [x] Hook Config display init and region policy; VboRegion constructor/upload/queue/finish/delete/getPositionTop using exact M5 fingerprints.
- [x] Verify complete patched classes on Java8 with actual framework binary; smoke LaunchWrapper order.
- [x] Commit hooks and bundle task with unshaded LGPL dependency/source provenance.

### Task 3: Checked standalone launch integration
Files: Launcher engine launch plan/bundle verifier/tests in isolated existing submodule checkout on opus-optim; integration docs/evidence.
Consumes generated renderer bundle; produces validated classpath, property, tweaker and startup diagnostics.
- [x] Write missing/tampered/disabled/valid bundle plan tests; run RED.
- [x] Implement explicit optional O2 bundle support without changing Runtime artifact roles or user settings.
- [x] Run engine checks; stage disposable client and verify actual startup/rendering when available.
- [x] Record exact verified boundaries, whole-change review and local commits.

### Task 4: Live-found integrated-server lifetime edge
Files: ServerTaskWait.java; RendererHooks/RendererTransformer; managed developer probes/tests.
The pinned M5 integrated server can enqueue its shutdown task after the owner
thread has terminated. Preserve its original Guava exception/uninterruptible path
for ordinary futures; cancel only the abandoned task owned by a TERMINATED
server thread, returning the normal void result to the original shutdown body.
Validate exact bpo input and unchanged MinecraftServer thread-field input in
addition to the five renderer targets. Retain original behavior before readiness.
- [x] Reproduce queued-future wait with actual native thread dumps.
- [x] Write terminated/live/failure/interrupt tests RED and implement GREEN.
- [x] Verify all seven complete classes on actual Java8 and bundle freshness.
- [x] Verify complete native renderer/reload/VBO/world cycle with zero packet errors.
- [x] Obtain whole-change review; do not infer a verdict from quota failure.

Completion evidence: production df955a5; Launcher5b031c7; native session
1791962240351-41185,138-test suite, seven actual Java8 classes and bundle receipt.
Whole-change review returned With fixes; the Important future race was fixed in
one RED/GREEN pass. See evidence/2026-10-14-renderer/REVIEW.md for disposition and
all execution rulings. All candidates remain experimental; no FPS claim or
production promotion is inferred.
