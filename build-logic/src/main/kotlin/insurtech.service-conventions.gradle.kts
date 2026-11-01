import com.insurtech.build.library

// <ctx>-service: the independently deployable Spring Boot application for one bounded context.
// Wiring and configuration only — no business logic (CRC-ARCH-001, CRC-ARCH-015).
plugins {
    id("insurtech.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(library("spring-boot-starter-actuator"))

    testImplementation(project(":libs:platform-archtest"))
    testImplementation(library("spring-boot-starter-test"))
}
