plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:regreporting:regreporting-domain"))
    api(project(":services:regreporting:regreporting-api"))
}
