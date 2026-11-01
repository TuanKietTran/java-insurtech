import com.insurtech.build.catalogVersion
import com.insurtech.build.enforceModuleBoundaries
import com.insurtech.build.library
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

// Base conventions for every JVM module (CODE-RULE-CONTRACT §1, §19, §20).
plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

group = "com.insurtech"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(catalogVersion("java").toInt())
    }
}

dependencies {
    implementation(platform(library("spring-boot-dependencies")))
    compileOnlyApi(library("jspecify"))
    testCompileOnly(library("jspecify"))
    "errorprone"(library("errorprone-core"))
    "errorprone"(library("nullaway"))

    testImplementation(library("junit-jupiter"))
    testImplementation(library("assertj-core"))
    testRuntimeOnly(library("junit-platform-launcher"))
}

// CRC-STY-002: Error Prone everywhere, NullAway strict on production code (CRC-DOM-006).
tasks.withType<JavaCompile>().configureEach {
    val isTestCompilation = name.contains("Test")
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation,unchecked"))
    options.errorprone {
        disableWarningsInGeneratedCode = true
        excludedPaths = ".*/build/generated/.*"
        option("NullAway:AnnotatedPackages", "com.insurtech")
        check("NullAway", if (isTestCompilation) CheckSeverity.OFF else CheckSeverity.ERROR)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// CRC-STY-001: google-java-format; spotlessCheck runs as part of `check`.
spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat(catalogVersion("google-java-format"))
    }
}

// CRC-STY-003: no Lombok, anywhere.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.projectlombok") {
            throw GradleException("CRC-STY-003: Lombok is prohibited; use records and explicit code")
        }
    }
}

// CRC-ARCH-001/002/004/011/014: module boundaries enforced at configuration time.
enforceModuleBoundaries()
