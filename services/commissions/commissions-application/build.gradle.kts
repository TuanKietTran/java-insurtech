plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:commissions:commissions-domain"))
    api(project(":services:commissions:commissions-api"))
}
