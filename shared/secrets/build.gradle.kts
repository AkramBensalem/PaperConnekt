plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)
    androidLibrary {
        namespace = "com.paperconnekt.shared.secrets"
        compileSdk = 37
        minSdk = 26
        androidResources.enable = true
    }
    jvm("desktop")
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
        }
    }
}
