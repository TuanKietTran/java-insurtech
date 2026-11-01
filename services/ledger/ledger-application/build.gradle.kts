plugins {
    id("insurtech.application-conventions")
}

dependencies {
    api(project(":services:ledger:ledger-domain"))
    api(project(":services:ledger:ledger-api"))
}
