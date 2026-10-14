# O2 renderer dependency notices

O2-owned code retains the Opus license in O2-LICENSE. External dependencies are separate, unshaded JARs and retain their original terms.

- Celeritas Common 2.5.0-pre.1, published Java8 downgraded classifier. LGPL-3.0, with included upstream COPYING/COPYING.LESSER. Source and build instructions: https://git.taumc.org/embeddedt/celeritas . Published source archive: https://maven.taumc.org/releases/org/embeddedt/celeritas/celeritas-common/2.5.0-pre.1/celeritas-common-2.5.0-pre.1-sources.jar . Built artifact SHA256: 51e823d63f8e58d4ece52772909ab9d0364e0ba892c981eb1bc8262836523d87 . Only its arena/native/LWJGL service surfaces are used; this is not the complete Celeritas Minecraft mod.
- fastutil 7.1.0, Apache License 2.0. Source: https://github.com/vigna/fastutil . Original JAR retains notices/resources.
- JOML 1.10.5, MIT. Source: https://github.com/JOML-CI/JOML . Original JAR retains notices/resources.

LaunchWrapper, ASM5, LWJGL2 and Log4j are supplied by the host. No Minecraft/OptiFine/LWJGL/Loader classes are distributed in the O2 renderer JAR.

The bundle manifest is an editable integrity receipt, not a signature or a ban on modifying the LGPL library. A user may replace/rebuild the separate library JAR and update its filename/size/hash in the manifest. The unmodified version above is the compatibility-tested configuration. Replacing the framework requires retaining its public binary interfaces; incompatibilities fail explicitly.
