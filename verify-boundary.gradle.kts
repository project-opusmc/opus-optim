import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.tasks.SourceSetContainer

val mainSources = extensions.getByType<SourceSetContainer>().getByName("main")
val productionClasspath = configurations.getByName("compileClasspath")
val forbiddenSymbols = listOf(
    "net/minecraft/", "net/minecraftforge/", "org/lwjgl/", "org/cef/", "com/jogamp/",
    "org/polydevs/opusmc/bootstrap/", "org/polydevs/opusmc/standalone/",
    "org/polydevs/opusmc/forge/", "org/polydevs/opusmc/injector/",
    "org/polydevs/opusmc/client/", "org/polydevs/opusmc/nativeui/", "cc/polyfrost/",
).flatMap { listOf(it, it.replace('/', '.')) }

val verifyOptimBoundary = tasks.register("verifyOptimBoundary") {
    group = "verification"
    description = "Rejects game, Loader, UI and GL dependencies/symbols in pure Optim contracts/core."
    dependsOn(tasks.named("classes"))
    inputs.files(mainSources.allJava, mainSources.output.classesDirs, productionClasspath)
    doLast {
        val allowedProjects = if (project.path == ":optim:api") setOf(project.path)
            else setOf(project.path, ":optim:api")
        val forbiddenGroups = listOf("net.minecraft", "net.minecraftforge", "org.lwjgl", "org.ow2.asm",
            "org.spongepowered", "cc.polyfrost", "org.jogamp", "me.friwi")
        val invalidDependencies = productionClasspath.incoming.resolutionResult.allComponents.mapNotNull {
            when (val id = it.id) {
                is ProjectComponentIdentifier -> if (id.projectPath !in allowedProjects) id.displayName else null
                is ModuleComponentIdentifier -> if (forbiddenGroups.any { group ->
                    id.group == group || id.group.startsWith("$group.")
                }) id.displayName else null
                else -> null
            }
        }
        check(invalidDependencies.isEmpty()) { "Forbidden Optim production dependencies: $invalidDependencies" }
        val sourceFiles = mainSources.allJava.files
        val classFiles = mainSources.output.classesDirs.asFileTree.matching { include("**/*.class") }.files
        val violations = (sourceFiles + classFiles).sortedBy { it.path }.mapNotNull { file ->
            val symbols = file.readBytes().toString(Charsets.ISO_8859_1)
            forbiddenSymbols.firstOrNull { symbols.contains(it) }?.let {
                "${file.relativeTo(projectDir)} references $it"
            }
        }
        check(violations.isEmpty()) { "Forbidden Optim symbols:\n${violations.joinToString("\n")}" }
    }
}

tasks.named("test") { dependsOn(verifyOptimBoundary) }
tasks.named("check") { dependsOn(verifyOptimBoundary) }
