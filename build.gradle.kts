// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Fail verification when a new project dependency violates the intended layering.
tasks.register("validateModuleDependencies") {
    group = "verification"
    description = "Checks Ledgerly module boundaries and prohibits dependency cycles."
    doLast {
        val allowed = mapOf(
            ":app" to setOf(":domain", ":data", ":core:designsystem"),
            ":data" to setOf(":domain"),
            ":domain" to emptySet<String>(),
            ":core:designsystem" to emptySet<String>(),
        )
        val graph = allowed.keys.associateWith { path ->
            // Inspect declared dependency buckets, not AGP's generated self-referencing
            // test-target configurations.
            project(path).configurations.filter { configuration ->
                listOf("api", "implementation", "compileOnly", "runtimeOnly").any { bucket ->
                    configuration.name.endsWith(bucket, ignoreCase = true)
                }
            }.flatMap { configuration ->
                configuration.dependencies.withType<org.gradle.api.artifacts.ProjectDependency>()
                    .map { it.dependencyProject.path }
            }.toSet()
        }
        graph.forEach { (path, dependencies) ->
            check(allowed.getValue(path).containsAll(dependencies)) {
                "Unexpected dependencies from $path: ${dependencies - allowed.getValue(path)}"
            }
        }
        fun visit(path: String, ancestors: Set<String>) {
            check(path !in ancestors) { "Module dependency cycle: $ancestors -> $path" }
            graph.getValue(path).forEach { visit(it, ancestors + path) }
        }
        graph.keys.forEach { visit(it, emptySet()) }
        logger.lifecycle("Ledgerly module dependency boundaries verified: $graph")
    }
}
