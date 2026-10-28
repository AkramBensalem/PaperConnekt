import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.gradle.kotlin.dsl.configure

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")

            extensions.configure<KotlinMultiplatformExtension> {
                jvmToolchain(17)
                jvm("desktop") {
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
}
