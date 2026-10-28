plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.paperconnekt.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.paperconnekt.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":app:composeApp"))
    implementation(project(":shared:storage"))
    implementation(project(":shared:secrets"))
    implementation(libs.androidx.activity.compose)
}
