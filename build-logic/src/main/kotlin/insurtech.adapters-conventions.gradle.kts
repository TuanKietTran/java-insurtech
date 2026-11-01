import com.insurtech.build.library

// <ctx>-adapters: HTTP, persistence, messaging and external adapters (CRC-ARCH-008, CRC-PER-*, CRC-EVT-*).
plugins {
    id("insurtech.java-conventions")
}

dependencies {
    implementation(project(":libs:platform-web"))
    implementation(library("spring-boot-starter-webmvc"))
    implementation(library("spring-boot-starter-jdbc"))
    implementation(library("spring-boot-starter-flyway"))
    implementation(library("spring-boot-starter-kafka"))
    implementation(library("flyway-database-postgresql"))
    runtimeOnly(library("postgresql"))

    testImplementation(library("spring-boot-starter-test"))
}
