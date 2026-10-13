plugins {
    base
}

allprojects {
    group = "org.polydevs.opusmc"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}

subprojects {
    apply(plugin = "java-library")

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-options", "-Werror"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
