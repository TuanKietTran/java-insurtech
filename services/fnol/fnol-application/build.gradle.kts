plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:fnol:fnol-domain"))
    api(project(":services:fnol:fnol-api"))
}
