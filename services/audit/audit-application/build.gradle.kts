plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:audit:audit-domain"))
    api(project(":services:audit:audit-api"))
}
