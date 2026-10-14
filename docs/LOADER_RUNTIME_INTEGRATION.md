# O2 on completed Opus Loader

The current isolated opus-optim sources use the loader-owned managed provider
SPI. There is no O2 tweaker or second game classloader. Runtime remains a
five-role artifact set, with a separately selected eight-JAR O2 bundle.

## Startup contract
Launcher validates schema2, protocol1, exact Minecraft1.8.9/OptiFineM5 hashes,
eight JAR receipts, provider service descriptors and the current host SPI marker.
It copies/re-hashes private session artifacts, then sets only the requested O2
properties. Schema1 tweaker bundles and old Loader artifacts are rejected.

OpusTweaker installs helper/framework parent exclusions before discovery.
Runtime approves only explicit known opus.optim.renderer / opus.optim ids.
Providers initialize against the actual game loader before registrations seal;
missing selected providers fail. With O2 absent, discovery does not construct
external services and the original nine built-ins remain.

One priority/id chain routes raw target names to managed providers and retains
mapped GUI names for built-ins. Renderer priority0 runs before lifecycle/hooks
105+ on exact raw inputs. Independent implemented O2 patches at400 follow the
built-ins. Fingerprints still determine application, never class-name guesses.

## Build and local staging
Build Runtime first; integration compilation uses its actual bootstrap JAR:

```sh
cd ../runtime
./gradlew check prepareRuntime verifyRuntimeArtifacts
cd ../opus-optim
./gradlew test check assemble -PopusBootstrapJar=../runtime/bootstrap/build/libs/bootstrap-0.0.1.jar
```

The O2 output remains renderer-celeritas/build/renderer-bundle, now schema2 with
api/core/bench/platform/renderer plus Celeritas/fastutil/JOML as separate JARs.
Loader/ASM/LWJGL/game classes and the test agent are never bundled. Own Opus
license and separate Celeritas LGPL texts/corresponding source remain intact.
The pinned Loader fixture is only a test/fallback input, not an installed API.

For a newly built Runtime, synchronize existing Launcher artifact fingerprints
before verifying/staging a candidate. This does not install or publish anything:

```sh
cd ../launcher
node scripts/update-runtime-launch-contract.mjs ../runtime/build/runtime
node scripts/stage-runtime-artifacts.mjs ../runtime/build/runtime /absolute/private/bootstrap
node scripts/verify-runtime-launch-contract.mjs /absolute/private/bootstrap
```

The sync validates all five sizes/SHA256/checksum records before writing pins;
existing native ABI/marker checks remain. Stage the freshly built CEF helper
beside the five JARs when using the packaged native game-host API. Source pin
changes require rebuilding Launcher. Existing installed binaries use their old
contract until an explicit installation/release step.

## Explicit selection
Desktop developer environment: OPUS_OPTIM_RENDERER_BUNDLE=/absolute/path/to/bundle.
Optional OPUS_OPTIM_PATCHES is a comma-separated list of implemented startup ids:

- render.frustum.positive-vertex
- text.glyph_lookup
- render.buffer_growth
- world.fixed_budget
- runtime.resource_string_cache

CLI: launch --standalone --optim-renderer-bundle /absolute/path/to/bundle
--optim-patches text.glyph_lookup,world.fixed_budget --dry-run. Direct CLI game
launch stays disabled on macOS. Unknown/duplicate ids or patches without a
checked bundle fail. No config/world/preferences are rewritten by selection.

The base native control boots the client/menu with O2 absent and requires client
READY before shutdown. The selected workload runs actual terrain, reload,
VBO OFF/ON and world unload/reopen in the isolated offline instance. Neither
control is physical GUI acceptance or a controlled FPS comparison. Current
receipts and independent-review disposition live in
[evidence](../evidence/2026-10-14-loader-runtime/RESULTS.md).
