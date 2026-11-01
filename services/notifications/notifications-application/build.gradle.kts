plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:notifications:notifications-domain"))
    api(project(":services:notifications:notifications-api"))
}
