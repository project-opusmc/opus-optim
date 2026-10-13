plugins { `java-library` }

dependencies {
    api(project(":api"))
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

apply(from = rootProject.file("verify-boundary.gradle.kts"))

tasks.register<JavaExec>("run") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.polydevs.opusmc.optim.bench.BenchmarkCli")
}
