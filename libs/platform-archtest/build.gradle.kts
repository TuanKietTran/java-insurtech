import com.insurtech.build.library

// Code Rule Contract as executable ArchUnit rules. Every -service and BFF module runs them in `test`.
plugins {
    id("insurtech.java-conventions")
}

dependencies {
    api(library("archunit"))
}
