plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(libs.kotlinx.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        named("desktopMain") {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
    }

    android {
        namespace = "com.paperconnekt.shared.storage"
        compileSdk = 35

        defaultConfig {
            minSdk = 26
        }
    }
}
