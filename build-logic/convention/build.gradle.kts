import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.paperconnekt.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:${libs.findVersion("agp").get().requiredVersion}")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.findVersion("kotlin").get().requiredVersion}")
    compileOnly(
        "org.jetbrains.compose:compose-gradle-plugin:" +
            libs.findVersion("compose").get().requiredVersion,
    )
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatform") {
            id = "com.paperconnekt.kotlin.multiplatform"
            implementationClass = "KotlinMultiplatformConventionPlugin"
        }
        register("kotlinJvm") {
            id = "com.paperconnekt.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "com.paperconnekt.compose.multiplatform"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("desktopApp") {
            id = "com.paperconnekt.desktop.app"
            implementationClass = "DesktopAppConventionPlugin"
        }
    }
}
