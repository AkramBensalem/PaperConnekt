import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("paperconnekt")
        browser {
            commonWebpackConfig {
                outputFileName = "paperconnekt.js"
            }
        }

        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":app:composeApp"))
            implementation(project(":shared:storage"))
            implementation(project(":shared:secrets"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.napier)
        }
    }
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.paperconnekt.web.resources"
}
