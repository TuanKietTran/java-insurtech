plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:vendors:vendors-domain"))
    api(project(":services:vendors:vendors-api"))
}
