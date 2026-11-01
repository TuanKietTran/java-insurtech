import com.insurtech.build.library

// Shared kernel (CRC-ARCH-006): the only internal dependency a -domain module may have. JDK only.
plugins {
    id("insurtech.java-conventions")
}

dependencies {
    testImplementation(library("jqwik"))
}
