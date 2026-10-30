plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(project(":shared:storage"))
            implementation(project(":shared:secrets"))
            implementation(project(":shared:knowledge"))
            implementation(project(":shared:mcp"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.animation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.napier)
            implementation(libs.markdown.renderer.core)
            implementation(libs.markdown.renderer.m3)
            implementation(libs.markdown.renderer.coil3)
            implementation(libs.androidx.datastore)
            api(libs.koog.agents)
            api(libs.koog.agents.additions)
            implementation(libs.koog.http.client.ktor)
            implementation(libs.nucleus.pdfium)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.jlatexmath)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.io.encoding.ExperimentalEncodingApi")
    }

}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.paperconnekt.app.resources"
}

android {
    namespace = "com.paperconnekt.app"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}
