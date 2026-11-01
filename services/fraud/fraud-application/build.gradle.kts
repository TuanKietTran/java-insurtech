plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:fraud:fraud-domain"))
    api(project(":services:fraud:fraud-api"))
}
