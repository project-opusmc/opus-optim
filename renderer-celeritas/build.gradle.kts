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
