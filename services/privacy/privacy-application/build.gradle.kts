plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:privacy:privacy-domain"))
    api(project(":services:privacy:privacy-api"))
}
