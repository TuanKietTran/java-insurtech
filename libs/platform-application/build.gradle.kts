// Cross-cutting application ports (authorization, audit, outbox, transactions, idempotency).
// Framework-free like every -application module (CRC-ARCH-013); no business logic (CRC-ARCH-014).
plugins {
    id("insurtech.java-conventions")
}

dependencies {
    api(project(":libs:shared-kernel"))
}
