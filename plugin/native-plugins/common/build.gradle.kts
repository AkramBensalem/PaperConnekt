plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":shared:domain"))
    api(project(":shared:storage"))
    api(project(":plugin:native-api"))
    implementation(libs.kotlinx.serialization.json)
}
