import groovy.json.JsonSlurper
import java.security.MessageDigest

plugins { `java-library` }

// Local draft API fixture. These classes are compile/test inputs, never JAR inputs.
val loaderContract = sourceSets.create("loaderContract")
dependencies {
    api(project(":core"))
    api(project(":bench"))
    compileOnly(files(loaderContract.output))
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
