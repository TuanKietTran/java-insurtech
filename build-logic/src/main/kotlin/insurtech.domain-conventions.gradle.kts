import com.insurtech.build.library

// <ctx>-domain: pure Java model and invariants (CRC-ARCH-002/003, CRC-DOM-010).
plugins {
    id("insurtech.java-conventions")
}

dependencies {
    api(project(":libs:shared-kernel"))
    testImplementation(library("jqwik"))
}
