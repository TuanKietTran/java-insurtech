plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:rating:rating-domain"))
    api(project(":services:rating:rating-api"))
}
