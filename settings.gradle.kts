pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "java-insurtech"

// Module discovery (CRC-ARCH-001). Every directory with a build.gradle.kts is a module:
//   libs/<name>                  -> :libs:<name>
//   bff/<name>                   -> :bff:<name>
//   services/<ctx>/<ctx>-<role>  -> :services:<ctx>:<ctx>-<role>
fun modulesIn(dir: File): List<File> =
    dir.listFiles().orEmpty().filter { File(it, "build.gradle.kts").isFile }.sortedBy { it.name }

modulesIn(file("libs")).forEach { include(":libs:${it.name}") }
modulesIn(file("bff")).forEach { include(":bff:${it.name}") }
file("services").listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }.forEach { context ->
    modulesIn(context).forEach { include(":services:${context.name}:${it.name}") }
}
