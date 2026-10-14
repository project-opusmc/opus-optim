# O2 Celeritas renderer evidence

Current source: O2 5559294; isolated Launcher cb67916. The source contains a
real custom region arena/upload/ordered draw backend linked to the separate
Celeritas Common2.5.0-pre.1 Java8 artifact. Neither the full Sodium mesher nor
an FPS improvement is claimed.

Build/check/assemble:126 tests, no failures/errors/skips. Java8u74 full-class
verification covers exact Config, bqs, bfr, ave and VboRegion inputs. Bundle
freshness and own Java8 namespace checks pass. Launcher:69 engine tests and1 CLI
test pass; official-network contract test is explicitly ignored. Clippy with
warnings denied and desktop cargo check pass. Opus LICENSE remains byte-identical.
Four-JAR hashes/sizes are recorded in bundle-manifest.json and verification.json.

The real GL2.1 Metal Apple M4 harness ran on Java8u502 and managed Java8u74,
with unmodified Celeritas binary. Ordered conventional-VBO/custom-region output
matched exactly across arena growth, with21522 colored pixels; replacement,
empty uploads, GPU/native release and GL error0 were observed. Its storage/math
path is unchanged by the later world-lifetime hooks.

An earlier bundle ran through the production packaged macOS Minecraft host and
pinned installed Runtime. Session1791943414382-29260 reached custom draw submission
(1024 vertices on the first logged batch), world=true/screen=none, C128/4624 D8,
and captured actual2000x1300 grass terrain with GL error0. See earlier-terrain.png.
It also exposed created32/deleted16 after a test null-world unload; this is not
reported as a successful current lifecycle test.

Subsequent author investigation found vanilla Minecraft's null-world path does
not call RenderGlobal.setWorld(null), and found Gradle's custom bundle task could
leave a stale JAR in the output directory. Latest source adds explicit Minecraft
unload/reload ownership, early tweaker ordering and declared bundle inputs plus
a freshness check. Unit/whole-class/build checks are green. The current-source
live replay attempted at1791944823555-36487 was refused by the production host:
"macOS console is locked; unlock before launching Minecraft". No bypass was
attempted. Current GPU world-cleanup replay remains NOT_TESTED pending unlock.

Previous no-draw runs were paused before initial server chunk packets. The
bounded test agent now disables pauseOnLostFocus only in its validated disposable
instance, disconnects/drains queued packets, returns to menu and exits. It is
excluded from the production bundle. Automated game capture is not physical
widget/input acceptance. FPS/A/A/A/B, shaders/AF/AA transitions, resource packs,
dimensions and full terrain compiler replacement remain unverified.

Raw full client logs stay in the disposable workspace; committed selected logs
contain only O2/probe state and the native activation refusal, avoiding helper
capability URLs. Independent whole-change review is pending. No push, merge,
installed Launcher replacement or published superproject gitlink change occurred.
