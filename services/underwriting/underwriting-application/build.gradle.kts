plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:underwriting:underwriting-domain"))
    api(project(":services:underwriting:underwriting-api"))
}
