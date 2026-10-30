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
    implementation(libs.compose.material3)
    implementation(libs.compose.resources)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.swing)
}

configurations.configureEach {
    resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
}

compose.desktop {
    application {
        mainClass = "com.paperconnekt.desktop.MainKt"
        nativeDistributions {
            linux {
                iconFile.set(project.file("src/main/resources/icons/paperconnekt.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icons/paperconnekt.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icons/paperconnekt.icns"))
            }
        }
    }
}
