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
./gradlew :renderer-celeritas:rendererBundle
```

The output is `renderer-celeritas/build/renderer-bundle`. Its editable integrity
manifest enumerates exactly four separate JARs and pins the Minecraft1.8.9 and
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

The tweaker order is OptiFine -> O2 -> Opus. This lets exact raw Minecraft
unload hooks run before owned Loader transformations; subsequent Opus transforms
continue normally. Exact pins cover Config, bqs, bfr, ave and VboRegion. Unknown
selected inputs fail startup. Off mode returns original bytes. Display init
establishes GL2.1/LWJGL2 readiness on the render thread. The custom path requires
VBO enabled and shaders/AF/AA multitexture disabled; other settings preserve the
host path. Shader/AF/AA transition acceptance has not been established in game.

Verified on actual Java8u74 and GL2.1 Metal Apple M4: full extracted class verifier;
real arena growth/replacement/empty/free; ordered pixel parity with conventional
VBO draws; GPU/native release in the GL fixture. A production Standard client
with installed, pinned Runtime artifacts reached READY and drew grass terrain
through O2 (2000x1300, GL error0). That earlier bundle exposed incomplete world
cleanup. Source corrections now add explicit world ownership/null-world unload
and fix stale Gradle bundle output; fresh game cleanup replay is pending because
the last production host launch reported a locked macOS console.

Build/unit verification is separate from live replay, A/A/A/B FPS comparisons,
shader/pack/dimension acceptance, installation and independent review. None of
those unverified stages is inferred from READY or from a class name.
