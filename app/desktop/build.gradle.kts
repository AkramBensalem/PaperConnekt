plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":app:composeApp"))
    implementation(project(":shared:storage"))
    implementation(project(":shared:secrets"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.datetime)
}

compose.desktop {
    application {
        mainClass = "com.paperconnekt.desktop.MainKt"
    }
}
