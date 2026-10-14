import java.security.MessageDigest
import java.util.zip.ZipFile
import groovy.json.JsonOutput
import org.gradle.jvm.tasks.Jar
plugins { `java-library` }
repositories { maven("https://maven.taumc.org/releases") { content { includeGroup("org.embeddedt.celeritas") }; metadataSources { mavenPom(); artifact(); ignoreGradleMetadataRedirection() } }; maven("https://libraries.minecraft.net") { content { includeGroup("net.minecraft") } } }
dependencies {
    implementation("org.embeddedt.celeritas:celeritas-common:2.5.0-pre.1:downgraded-8@jar")
    implementation("it.unimi.dsi:fastutil:7.1.0")
    implementation("org.joml:joml:1.10.5")
    compileOnly("org.lwjgl.lwjgl:lwjgl:2.9.3") { isTransitive = false }
    compileOnly("net.minecraft:launchwrapper:1.12")
    compileOnly("org.ow2.asm:asm:5.0.3")
    compileOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")
    testImplementation("org.lwjgl.lwjgl:lwjgl:2.9.3") { isTransitive = false }
    testImplementation("net.minecraft:launchwrapper:1.12")
    testImplementation("org.ow2.asm:asm:5.0.3")
    testImplementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
val smoke = sourceSets.create("smoke") { compileClasspath += sourceSets.main.get().output + configurations.getByName("testRuntimeClasspath"); runtimeClasspath += compileClasspath }
tasks.register<Exec>("glVerify") {
    dependsOn(smoke.classesTaskName)
    doFirst {
        val java8 = providers.gradleProperty("java8Home").get()
        val natives = providers.gradleProperty("lwjglNatives").get()
        commandLine("$java8/bin/java", "-Djava.library.path=$natives", "-cp", smoke.runtimeClasspath.asPath,
            "org.polydevs.opusmc.optim.renderer.RegionGlVerify")
    }
}
// Upstream downgraded JAR retains optional NestMembers annotation descriptors.
// No O2 source warnings are suppressed; the binary is separately fingerprinted.
tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Xlint:-classfile") }
val frameworkSources = configurations.create("frameworkSources") { isCanBeConsumed = false }
dependencies { add(frameworkSources.name, "org.embeddedt.celeritas:celeritas-common:2.5.0-pre.1:sources@jar") }
val rendererJar = tasks.named<Jar>("jar")
val rendererBundle = tasks.register("rendererBundle") {
    dependsOn(rendererJar)
    val out = layout.buildDirectory.dir("renderer-bundle")
    outputs.dir(out)
    inputs.file(rendererJar.flatMap { it.archiveFile })
    inputs.files(configurations.getByName("runtimeClasspath"), frameworkSources)
    inputs.files(rootProject.file("LICENSE"), project.file("THIRD_PARTY.md"))
    doLast {
        val dir = out.get().asFile
        dir.deleteRecursively(); dir.mkdirs()
        val jars = listOf(rendererJar.get().archiveFile.get().asFile) + configurations.getByName("runtimeClasspath").files
        val artifacts = jars.sortedBy { it.name }.map { source ->
            val target = source.copyTo(dir.resolve(source.name))
            val bytes = target.readBytes()
            mapOf("file" to target.name, "size" to bytes.size,
                "sha256" to MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
        }
        val sourcesDir = dir.resolve("sources"); sourcesDir.mkdirs()
        frameworkSources.files.forEach { it.copyTo(sourcesDir.resolve(it.name)) }
        val upstream = jars.single { it.name.startsWith("celeritas-common-") }
        ZipFile(upstream).use { zip ->
            for (name in listOf("COPYING", "COPYING.LESSER")) {
                dir.resolve(name).writeBytes(zip.getInputStream(zip.getEntry(name)).use { it.readBytes() })
            }
        }
        rootProject.file("LICENSE").copyTo(dir.resolve("O2-LICENSE"))
        project.file("THIRD_PARTY.md").copyTo(dir.resolve("THIRD_PARTY.md"))
        val manifest = mapOf("schemaVersion" to 1, "backend" to "celeritas-region", "minecraftVersion" to "1.8.9",
            "minecraftSha256" to "14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298",
            "optifineSha256" to "2739760a70fca6ae5c7dc817bcc6283f18f55e38fa23778da7f387a6b724b2c4",
            "entrypoint" to "org.polydevs.opusmc.optim.renderer.OptimRendererTweaker", "artifacts" to artifacts)
        dir.resolve("opus-optim-renderer.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(manifest))+"\n")
    }
}
tasks.named("assemble") { dependsOn(rendererBundle) }
tasks.named<Exec>("glVerify") { doFirst { val dir=layout.buildDirectory.dir("gl-smoke").get().asFile;dir.mkdirs();workingDir(dir) } }
tasks.register<Exec>("hookVerify") {
    dependsOn(smoke.classesTaskName)
    doFirst {
        val minecraft = file(providers.gradleProperty("minecraftJar").get())
        val libraries = fileTree(minecraft.parentFile.parentFile.parentFile.resolve("libraries")) { include("**/*.jar") }
        commandLine("${providers.gradleProperty("java8Home").get()}/bin/java", "-cp", files(smoke.runtimeClasspath, libraries).asPath,
            "org.polydevs.opusmc.optim.renderer.HookClassVerify", providers.gradleProperty("minecraftJar").get(), providers.gradleProperty("optifineJar").get())
    }
}
val verifyRendererJar = tasks.register("verifyRendererJar") {
    dependsOn(rendererJar)
    doLast {
        ZipFile(rendererJar.get().archiveFile.get().asFile).use { zip ->
            val entries=zip.entries().asSequence().filter { it.name.endsWith(".class") }.toList()
            check(entries.isNotEmpty())
            entries.forEach { entry ->
                check(entry.name.startsWith("org/polydevs/opusmc/optim/renderer/")) { "Foreign renderer class ${entry.name}" }
                val bytes=zip.getInputStream(entry).use { it.readBytes() }
                check(bytes[6].toInt()==0 && bytes[7].toInt()==52) { "Renderer must target Java8: ${entry.name}" }
            }
        }
    }
}
tasks.named("check") { dependsOn(verifyRendererJar) }
// Test-only workload JAR is never included in the renderer bundle or production JAR.
val gameProbe = sourceSets.create("gameProbe") {
    compileClasspath += configurations.getByName("compileClasspath")
}
tasks.register<Jar>("gameProbeJar") {
    dependsOn(gameProbe.classesTaskName)
    archiveClassifier.set("game-probe")
    from(gameProbe.output)
    manifest { attributes("Premain-Class" to "org.polydevs.opusmc.optim.probe.GameProbeAgent") }
}
val verifyRendererBundle = tasks.register("verifyRendererBundle") {
    dependsOn(rendererBundle, verifyRendererJar)
    doLast {
        val bundle=layout.buildDirectory.dir("renderer-bundle").get().asFile
        val jar=rendererJar.get().archiveFile.get().asFile
        check(jar.readBytes().contentEquals(bundle.resolve(jar.name).readBytes())) { "O2 renderer bundle is stale" }
    }
}
tasks.named("check") { dependsOn(verifyRendererBundle) }
