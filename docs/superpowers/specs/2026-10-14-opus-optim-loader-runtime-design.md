# O2 through completed Opus Loader

User intent: Runtime and O2 improvements must run through Claude's completed
Opus Loader. Continue implementation without repeated approval menus. Preserve
exact opus-optim branches, primary dirty sources, Opus LICENSE, opt-in behavior,
separate LGPL dependency and the five Runtime artifact roles. No push/install,
workflows or published superproject gitlink change.

## Chosen integration
Use the loader-owned managed SPI, rather than a second O2 tweaker or another
classloader. Snapshot the completed primary Runtime sources into the existing
isolated Runtime opus-optim branch with a hash receipt. Retain existing M1
history without packaging its old Optim classes.

Runtime approves only known opus.optim / opus.optim.renderer ids from explicit
opus.loader.providers selection in Standalone. Absent selection remains base
loader. Missing selected providers and unsupported selections fail startup.
Provider initialization receives the actual game classloader before registrations
are sealed. Helpers/frameworks use parent exclusions before provider discovery.
Managed providers receive raw names; built-ins retain mapped GUI routes, with
one priority/id-ordered chain. Renderer priority0 precedes built-ins100+; existing
O2 patch priority400 follows them. Original renderer seven-bytecode pins remain.

The renderer registers a managed provider, no LaunchWrapper O2 tweak. The
existing O2 provider prepares an immutable explicit selection of implemented
frustum, glyph lookup, vertex-buffer growth, fixed upload budget and resource
text cache. Unknown patch ids fail; experimental opt-in is deliberate. This is
startup selection, not adaptive governors or a performance claim.

O2 builds against the actual newly built bootstrap JAR for integration; fixture
classes remain test/fallback inputs and are never packaged. Bundle schema2 names
both managed providers and contains8 separate JARs: O2 api/core/bench/platform,
renderer, Celeritas Common, fastutil and JOML, with source/license notices.
Launcher checks host SPI support, exact MC/OF inputs, artifact hashes/sizes,
required provider/contract entries and stages private copies. Existing explicit
bundle option selects renderer; an optional known patch list adds opus.optim.
No bundle/default means no O2 properties or extra transformer. Legacy schema1
O2-tweaker bundles fail with an actionable error after migration.

## Acceptance
Meaningful RED/GREEN tests cover default/invalid/missing approval, initialization
order/helper identity, raw/mapped routing, real provider preparation, early
renderer composition, tampered/old bundle and unsupported loader. Runtime
check/artifact verification, O2 check and actual-Java8 real-bootstrap smoke,
Launcher tests/clippy and a native disposable terrain/reload/VBO/world cycle
must be recorded. Also run a native base-loader control with O2 absent. Real
patch application is distinct from descriptor selection. Physical GUI, shaders,
packs/dimensions and controlled FPS remain unverified unless actually tested.
