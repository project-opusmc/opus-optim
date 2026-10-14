# O2 Celeritas renderer integration

The custom backend links the original Java8 Celeritas Common2.5.0-pre.1 JAR.
Its real arena owns GPU suballocation/growth/relocation. O2 submits ordered
multi-draws and owns world/reload lifetimes. No Minecraft, OptiFine, Loader or
LWJGL classes are bundled. Own code keeps Opus LICENSE; upstream LGPL notices
and the corresponding published source archive accompany the separate library.
Argentum's All Rights Reserved implementation is not copied.

This is a region storage/submission backend. Block meshing, visibility, lighting,
texture format and transparent sort remain Minecraft/OptiFine code. There is no
claim of the full Sodium mesher, instanced entities, shader integration or FPS gain.

Build the separately licensed bundle with:

```
./gradlew :renderer-celeritas:rendererBundle -PopusBootstrapJar=../runtime/bootstrap/build/libs/bootstrap-0.0.1.jar
```

The output is `renderer-celeritas/build/renderer-bundle`. Its editable integrity
schema2 manifest enumerates exactly eight separate JARs and pins the Minecraft1.8.9 and
OptiFine M5 inputs. Launcher copies/rechecks the JARs in the private session
before enabling `opus.optim.renderer=celeritas-region`. A modified LGPL library
may be rebuilt/replaced and its receipt updated; public binary interfaces must
remain compatible. The original published binary is the tested configuration.

The isolated Launcher branch `opus-optim` adds:

- Engine `LaunchOptions.optim_renderer_bundle` (None by default).
- Desktop `OPUS_OPTIM_RENDERER_BUNDLE=/absolute/path/to/bundle` opt-in in Standard.
- CLI `launch --standalone --optim-renderer-bundle /absolute/path/to/bundle --dry-run`.
- `optim_renderer_probe` example using the production packaged Minecraft.app
  API and a disposable explicit OPUS_HOME. Direct CLI game launch stays disabled
  on macOS. The optional game-probe JAR is test-only and excluded from the bundle.

LaunchWrapper retains OptiFine -> Opus. The Loader-owned renderer provider has
priority0 before the built-ins, preserving raw input access without an extra O2
tweaker. Managed patches receive raw names; built-ins keep mapped GUI routing.
Exact renderer pins cover Config, bqs, bfr, ave, bpo, MinecraftServer and VboRegion. Unknown
selected inputs fail startup. Off mode returns original bytes. Display init
establishes GL2.1/LWJGL2 readiness on the render thread. The custom path requires
VBO enabled and shaders/AF/AA multitexture disabled; other settings preserve the
host path. Shader/AF/AA transition acceptance has not been established in game.

Historical renderer-slice verification on actual Java8u74 and GL2.1 Metal Apple M4: seven full extracted
classes; real arena growth/replacement/empty/free; ordered pixel parity with
conventional VBO draws; GPU/native release in the GL fixture. The earlier separately-tweaked
Standard host workload rendered grass terrain (2000x1300, GL0), reloaded the
renderer, switched VBO OFF/ON, unloaded/reopened a world and finished96 created /
96 deleted /0 live regions with no probe/NPE errors. Vertex setup is linked once
and its callback reused without a per-batch reflective lookup.

A live-found M5 edge queued shutdown work after its server thread terminated.
The exact bpo/owner-field guard cancels only this abandoned task, while the
original Guava call retains ordinary results/errors/interrupt semantics and
original shutdown/pinger cleanup continues. Incomplete waits poll at50ms;
live/NEW/unknown owner threads are not cancelled. GPU null-world release follows
host unload completion. Developer packet-drain/frame helpers are test-only.
If cancellation loses a completion/cancellation race, the original future is
returned unchanged. Two deterministic regressions cover both outcomes.

Fresh independent review of the frozen package returned With fixes. Its one
Important race is fixed in df955a5 and verified by138 tests, the seven-class
Java8 verifier and native session1791962240351-41185. The review did not approve
the later fix or production promotion; see the covering tests and
[review disposition](../evidence/2026-10-14-renderer/REVIEW.md).

Build/unit verification is separate from live replay, A/A/A/B FPS comparisons,
shader/pack/dimension or physical GUI acceptance, installation and independent
review. None of those unverified stages is inferred from READY or from a class name.

Current managed SPI delivery and its own source/session receipts are documented
in [Loader Runtime integration](LOADER_RUNTIME_INTEGRATION.md). Earlier renderer
evidence remains attached to its original source and bundle.
