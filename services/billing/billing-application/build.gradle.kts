plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:billing:billing-domain"))
    api(project(":services:billing:billing-api"))
}
