# O2 Celeritas region renderer

The user requests uninterrupted implementation of advanced framework integration and a custom renderer for Opus Minecraft 1.8.9. Work stays on exactly `opus-optim`, preserves Claude's dirty Loader checkout, and keeps the Opus license for O2-owned code.

Use Celeritas Common 2.5.0-pre.1's published `downgraded-8` artifact, dynamically linked as a separate LGPL JAR. Do not copy Argentum's All Rights Reserved implementation. The integration uses Celeritas's real GL buffer arena and LWJGL2 service, and an O2 submission/lifetime adapter. It is a region storage and submission backend; it does not claim the full Sodium mesher, entity instancing, or shader port.

Minecraft/OptiFine retains block meshing, chunk visibility, coordinates, textures, lighting, transparent sorting and vertex pointer setup. O2 replaces the pinned OptiFine M5 VboRegion storage/upload/queue/finish/delete surface. The Celeritas arena owns suballocation, reuse, growth and relocation. O2 queues live segments, resolves their latest offsets at finish and issues ordered multi-draws. Region deletion frees arena and staging resources and clears references. A world ownership registry covers render-list reload and raw Minecraft null-world unload; vanilla Minecraft does not call RenderGlobal.setWorld(null) on that path. No duplicate vanilla GPU mesh is retained once the backend attaches.

A LaunchWrapper tweaker in a separate `renderer-celeritas` module registers these pinned hooks after OptiFine and before Opus, uses a parent loader exclusion for O2/framework helpers, and stays independent of unpublished provider SPI. Config's display initialization establishes capabilities on the render thread. Explicit property `opus.optim.renderer=celeritas-region` opts in; disabled input is byte-identical. Unsupported fingerprints must fail startup for explicit selection instead of silently claiming it ran. Config's region policy enables the region path only when the backend is ready, VBO is enabled and shaders/multitexture are off. Existing user settings files are not rewritten. Unknown targets and unsupported GL contexts are reported.

The Launcher standalone plan has an explicit optional backend bundle path and enables the tweaker plus exact property only after validating a versioned bundle manifest and SHA-256 for every file. The renderer remains optional while validation proceeds. Framework JARs, license texts, source provenance and dependencies are staged separately, not shaded or relabeled. Keep Runtime's five-role manifest unchanged; O2 is a separate launch-plan component.

Verification must exercise invalid pins, OFF bytes, lifecycle/reupload/empty mesh, relocation before draw, draw ordering/mode, partial buffers and thread ownership. Run Java8 against actual extracted M5 classes; use an actual OpenGL context and Celeritas binary for upload/readback/ordered draw/lifecycle and pixel parity. Launch a disposable Standard-compatible game directory where feasible; preserve user's game/options/accounts. Report separately build, GL harness, game startup and actual in-game rendering/FPS. There is no FPS claim without controlled game measurements.

Live-found shutdown edge: the M5 integrated server can enqueue a void shutdown
task after its owner thread has terminated, leaving Guava's wait blocked forever.
Pin bpo's future-wait call and MinecraftServer's aa Thread field. Preserve normal
results, errors and interrupt behavior through the original Guava call. Cancel an
abandoned task only after its owner is TERMINATED, returning the normal null
result to the unchanged shutdown body. Do not cancel work on a live, NEW or
unknown owner. Preserve the original path before renderer readiness. Null-world
GPU cleanup runs after the host unload returns. These extra exact inputs and the
50ms polling interval apply only to the explicitly selected experimental backend.

Developer probes dispatch outside Minecraft's task monitor and apply pending
packet tasks while their world is still valid. They neither drop tasks nor ship
in production. A whole native workload requires its final assertions and no
probe/NPE failures in either output stream; a renderer counter alone is not PASS.
