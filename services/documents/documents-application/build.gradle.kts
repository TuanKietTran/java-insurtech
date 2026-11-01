plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:documents:documents-domain"))
    api(project(":services:documents:documents-api"))
}
