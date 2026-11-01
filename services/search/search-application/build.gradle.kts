plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:search:search-domain"))
    api(project(":services:search:search-api"))
}
