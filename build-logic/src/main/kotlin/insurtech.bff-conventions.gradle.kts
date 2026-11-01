import com.insurtech.build.library

// bff/<name>: backend-for-frontend. Composes context APIs; no domain logic (CRC-ARCH-011).
plugins {
    id("insurtech.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":libs:platform-web"))
    implementation(library("spring-boot-starter-webmvc"))
    implementation(library("spring-boot-starter-actuator"))

    testImplementation(project(":libs:platform-archtest"))
    testImplementation(library("spring-boot-starter-test"))
}
