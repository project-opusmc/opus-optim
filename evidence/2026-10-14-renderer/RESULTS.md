# O2 Celeritas renderer evidence

Production source: O2 df955a5; isolated Launcher 5b031c7. Own Opus license
remains byte-identical. Separate LGPL Celeritas Common2.5.0-pre.1 Java8 artifact
owns the real GPU arena. O2 provides upload, ordered multi-draw, world ownership,
linked vertex-pointer callbacks and the exact integrated-server wait guard.
CPU block meshing, visibility, lighting, textures and transparent sort remain
Minecraft/OptiFine. Full Sodium/Celeritas mod, entity instancing and FPS gain are
not claimed.

Full suite:138 tests, no failures/errors/skips (renderer21). Actual Java8u74
verifies seven complete target classes: Config, bqs, bfr, ave, bpo,
MinecraftServer and VboRegion. Bundle freshness/own Java8 namespace checks pass.
Launcher:69 engine,1 CLI and3 developer-example tests pass; one official-network
contract test is explicitly ignored. Clippy with warnings denied and desktop
cargo check pass. Four-JAR hashes/sizes are in bundle-manifest.json and
verification.json; every staged current-session JAR was independently rehashed,
and the staged own JAR matches the current build byte-for-byte.

The earlier real GL2.1 Metal Apple M4 fixture ran on Java8u502 and managed8u74
with the unmodified Celeritas binary. Ordered conventional-VBO/custom-region
pixels match exactly across arena growth:21522 colored pixels. Replacement,
empty upload, GPU/native release and GL error0 were checked. Storage/math is
unchanged by the later ownership/callback/server fixes.

Current native session1791962240351-41185 uses the production packaged macOS
Minecraft.app and pinned installed Runtime, in an isolated offline game root.
All seven exact inputs applied; real terrain rendered at2000x1300 with GL error0.
See current-terrain.png and current-lifecycle.log. Renderer reload released16
old regions and created replacement regions. VBO OFF left0 O2 regions; ON
resumed real draw submission. First unload:created64/deleted64/live0. The log
proves the terminated-server guard handled a genuinely abandoned shutdown future.
A reopened world rendered again; final unload:created96/deleted96/live0, GL0.
No probe failure or NullPointerException appears in either output stream, and
the native lifecycle/strict developer verdict completed successfully.

These are automated terrain/render/lifecycle checks. Physical GUI/input,
shaders/AF/AA transitions, resource packs, dimensions and controlled A/A/A/B FPS
acceptance remain unverified. No installed Launcher replacement or default
auto-enable occurred. Existing user worlds/options/accounts were not used.

Earlier failures are retained as provenance, not reassigned to the latest build:
- Older bundle1791943414382-29260 rendered terrain but left16 regions.
- Source5559294 session1791956506074-84950 passed a single world cycle32/32/0.
- Several extended probes hung in integrated-server shutdown or Netty close.
- Direct public API1791958093915-4233 reached96/96/0 but produced582 NPE log
  occurrences; whole-workload PASS was rejected.
- Renderer-OFF control1791958813490-9151 also produced46 NPE occurrences.
The developer fixture now dispatches outside the Minecraft task monitor and
applies pending packets before their handler world is cleared, without dropping
tasks. Native thread dumps also exposed M5 enqueueing shutdown work after its
owner thread terminated. The production guard pins both owner metadata and
call-site inputs; it cancels only an abandoned task after TERMINATED, keeps the
original Guava result/failure/interrupt path, and leaves the original shutdown
body/pinger cleanup in place. Live/NEW/unknown owners are not cancelled.

Raw full logs remain private in the disposable workspace. Committed selected
logs contain only O2/probe state and never helper capability URLs. Fresh independent whole-change review returned With fixes on cff5c0b. Its
Important future-completion race was reproduced RED and fixed GREEN in df955a5,
with the complete suite and this new native workload verified afterward. See
REVIEW.md for the frozen review, single fix pass and exhaustive rulings. An older
review attempt ended at quota without a verdict. No push, merge or published superproject gitlink change occurred.
