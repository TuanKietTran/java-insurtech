plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:product:product-domain"))
    api(project(":services:product:product-api"))
}
