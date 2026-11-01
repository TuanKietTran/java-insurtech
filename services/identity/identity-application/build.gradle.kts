plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:identity:identity-domain"))
    api(project(":services:identity:identity-api"))
}
