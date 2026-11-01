plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:rules:rules-domain"))
    api(project(":services:rules:rules-api"))
}
