import com.insurtech.build.library

// Shared HTTP edge for adapters and BFFs: Idempotency-Key enforcement (CRC-IDEM-001), correlation IDs
// (CRC-API-007) and RFC 9457 Problem Details (CRC-API-003). Auto-configured.
plugins {
    id("insurtech.java-conventions")
}

dependencies {
    api(project(":libs:platform-application"))
    api(library("spring-boot-starter-webmvc"))

    testImplementation(library("spring-boot-starter-test"))
}
