plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    runtimeOnly("ch.qos.logback:logback-classic:1.4.11")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.7")

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
