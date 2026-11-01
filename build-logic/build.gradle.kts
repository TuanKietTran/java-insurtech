plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugins.spotless.asDependency())
    implementation(libs.plugins.errorprone.asDependency())
    implementation(libs.plugins.spring.boot.asDependency())
}

/** Turns a version-catalog plugin alias into the coordinates of its plugin marker artifact. */
fun Provider<PluginDependency>.asDependency(): Provider<String> =
    map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }
