import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

class DesktopAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<org.jetbrains.compose.ComposeExtension> {
                desktop.application {
                    mainClass = "com.paperconnekt.app.MainKt"
                    nativeDistributions {
                        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                        packageName = "com.paperconnekt.app"
                        packageVersion = "0.1.0"
                    }
                }
            }
        }
    }
}
