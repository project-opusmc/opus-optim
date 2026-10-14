import groovy.json.JsonSlurper
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.gradle.jvm.tasks.Jar

plugins { `java-library` }

// Pinned loader fixture is a fallback/test input; integration builds use the real Runtime JAR.
val loaderContract = sourceSets.create("loaderContract")
dependencies {
    api(project(":core"))
    api(project(":bench"))
    compileOnly(if (providers.gradleProperty("opusBootstrapJar").isPresent)
        files(providers.gradleProperty("opusBootstrapJar").get()) else files(loaderContract.output))
    compileOnly("org.ow2.asm:asm:5.0.3")
    testImplementation(files(loaderContract.output))
    testImplementation("org.ow2.asm:asm:5.0.3")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val verifyLoaderContract = tasks.register("verifyLoaderContract") {
    inputs.file("loader-contract-sha256.json")
    inputs.files(loaderContract.allSource)
    doLast {
        val hashes = JsonSlurper().parse(file("loader-contract-sha256.json")) as Map<*, *>
        val actual = loaderContract.allSource.files.map { it.relativeTo(projectDir).invariantSeparatorsPath }.toSet()
        check(actual == hashes.keys) { "Loader fixture file set changed; re-pin explicitly" }
        hashes.forEach { (path, expected) ->
            val bytes = file(path.toString()).readBytes()
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            check(hash == expected) { "Loader fixture drift: $path" }
        }
    }
}
tasks.named(loaderContract.compileJavaTaskName) { dependsOn(verifyLoaderContract) }
tasks.named("check") { dependsOn(verifyLoaderContract) }

val platformJar = tasks.named<Jar>("jar")
val verifyPlatformArtifact = tasks.register("verifyPlatformArtifact") {
    group = "verification"
    dependsOn(platformJar)
    inputs.file(platformJar.flatMap { it.archiveFile })
    doLast {
        val prefix = "org/polydevs/opusmc/optim/platform/"
        val service = "META-INF/services/org.polydevs.opusmc.bootstrap.ClassTransformerProvider"
        val forbidden = listOf("net/minecraft/", "net/minecraftforge/", "org/lwjgl/", "org/cef/",
            "com/jogamp/", "org/polydevs/opusmc/client/",
            "org/polydevs/opusmc/nativeui/", "org/polydevs/opusmc/injector/", "cc/polyfrost/")
            .flatMap { listOf(it, it.replace('/', '.')) }
        ZipFile(platformJar.get().archiveFile.get().asFile).use { zip ->
            val classes = zip.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".class") }.toList()
            check(classes.isNotEmpty()) { "Platform JAR contains no classes" }
            classes.forEach { entry ->
                check(entry.name.startsWith(prefix)) { "Bundled foreign class in O2 artifact: ${entry.name}" }
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                check(bytes.size >= 8 && bytes[6].toInt() == 0 && bytes[7].toInt() == 52) {
                    "Platform class must target Java 8: ${entry.name}"
                }
                val symbols = bytes.toString(Charsets.ISO_8859_1)
                check(forbidden.none { symbols.contains(it) }) { "Forbidden platform symbol in ${entry.name}" }
            }
            val entry = zip.getEntry(service) ?: error("O2 provider SPI missing")
            val providers = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText().trim() }
            check(providers == "org.polydevs.opusmc.optim.platform.OptimTransformerProvider") { "Incorrect O2 provider SPI" }
        }
    }
}
tasks.named("test") { dependsOn(verifyPlatformArtifact) }
tasks.named("check") { dependsOn(verifyPlatformArtifact) }
tasks.named("assemble") { dependsOn(verifyPlatformArtifact) }

val smoke = sourceSets.create("smoke") {
    compileClasspath += sourceSets.main.get().output + configurations.getByName("runtimeClasspath") +
        configurations.getByName("compileClasspath") + loaderContract.output
}
val artifactJars = listOf(":api", ":core", ":bench", ":platform-opus").map {
    project(it).tasks.named<Jar>("jar")
}
val smokeRuntime = files(smoke.output,
    if (providers.gradleProperty("opusBootstrapJar").isPresent) files(providers.gradleProperty("opusBootstrapJar").get()) else loaderContract.output,
    artifactJars.map { it.flatMap { jar -> jar.archiveFile } },
    configurations.getByName("runtimeClasspath").filter { it.name.startsWith("gson-") },
    configurations.getByName("compileClasspath").filter { it.name.startsWith("asm-") })
tasks.register<Exec>("java8Smoke") {
    group = "verification"
    description = "Runs synthetic bridge checks on Java 8 using built O2 JARs and the separate draft API fixture"
    dependsOn(artifactJars, smoke.classesTaskName, verifyPlatformArtifact)
    doFirst {
        val home = providers.gradleProperty("java8Home").orNull ?: error("Supply -Pjava8Home=/path/to/java8/home")
        val output = layout.buildDirectory.dir("smoke").get().asFile
        output.mkdirs()
        commandLine(file("$home/bin/java").absolutePath, "-cp", smokeRuntime.asPath,
            "org.polydevs.opusmc.optim.smoke.BridgeSmoke", output.absolutePath)
    }
}

tasks.register("writeFrustumHarnessClasspath") {
    dependsOn(artifactJars, smoke.classesTaskName, verifyPlatformArtifact)
    doLast {
        val output = layout.buildDirectory.file("frustum-classpath.txt").get().asFile
        output.parentFile.mkdirs(); output.writeText(smokeRuntime.asPath)
    }
}
tasks.register<Exec>("frustumVerify") {
    group = "verification"
    dependsOn(artifactJars, smoke.classesTaskName, verifyPlatformArtifact)
    doFirst {
        val java8 = providers.gradleProperty("java8Home").orNull ?: error("Supply java8Home")
        val minecraft = providers.gradleProperty("minecraftJar").orNull ?: error("Supply minecraftJar")
        val optifine = providers.gradleProperty("optifineJar").orNull ?: error("Supply optifineJar")
        commandLine(file("$java8/bin/java").absolutePath, "-cp", smokeRuntime.asPath,
            "org.polydevs.opusmc.optim.smoke.FrustumVerify", "verify", minecraft, optifine)
    }
}
tasks.register<Exec>("pipelineVerify") {
    group = "verification"
    dependsOn(artifactJars, smoke.classesTaskName, verifyPlatformArtifact)
    doFirst {
        val java8 = providers.gradleProperty("java8Home").orNull ?: error("Supply java8Home")
        val minecraft = providers.gradleProperty("minecraftJar").orNull ?: error("Supply minecraftJar")
        val optifine = providers.gradleProperty("optifineJar").orNull ?: error("Supply optifineJar")
        commandLine(file("$java8/bin/java").absolutePath, "-cp", smokeRuntime.asPath,
            "org.polydevs.opusmc.optim.smoke.PipelineVerify", "verify", minecraft, optifine)
    }
}
tasks.register<Exec>("resourceVerify") {
    group = "verification"
    dependsOn(artifactJars, smoke.classesTaskName, verifyPlatformArtifact)
    doFirst {
        val java8 = providers.gradleProperty("java8Home").orNull ?: error("Supply java8Home")
        val minecraft = providers.gradleProperty("minecraftJar").orNull ?: error("Supply minecraftJar")
        val optifine = providers.gradleProperty("optifineJar").orNull ?: error("Supply optifineJar")
        val commons = providers.gradleProperty("commonsLangJar").orNull ?: error("Supply commonsLangJar")
        commandLine(file("$java8/bin/java").absolutePath, "-cp", smokeRuntime.asPath,
            "org.polydevs.opusmc.optim.smoke.ResourceVerify", "verify", minecraft, optifine, commons)
    }
}
