plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:aiharness:aiharness-domain"))
    api(project(":services:aiharness:aiharness-api"))
}
