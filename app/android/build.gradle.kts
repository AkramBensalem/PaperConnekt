plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.paperconnekt.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.paperconnekt.app"
        minSdk = 26
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
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.android)
}
