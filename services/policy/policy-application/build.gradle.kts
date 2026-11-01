plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:policy:policy-domain"))
    api(project(":services:policy:policy-api"))
}
