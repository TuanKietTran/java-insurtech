plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:claims:claims-domain"))
    api(project(":services:claims:claims-api"))
}
