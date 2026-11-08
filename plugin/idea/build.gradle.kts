plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.intellij.platform")
}

group = "com.paperconnekt"
version = "0.1.0"

// JVM-only source sharing: bind common store factories to their existing desktop
// implementations without compiling the application's UI artifact.
val platformSourceNames = listOf(
    "PdfMarkdownStore", "PaperAgentChatStore", "IdeaNoteStore", "PaperKnowledgeCache", "ResearchHistory",
)
val nativePlatformSources = layout.buildDirectory.dir("generated/nativePlatformSources")
val generateNativePlatformSources by tasks.registering(Sync::class) {
    into(nativePlatformSources)
    from("../../app/composeApp/src/commonMain/kotlin") {
        include(platformSourceNames.map { "**/$it.kt" })
        filter { line -> if (line.startsWith("expect fun ")) "" else line }
    }
    from("../../app/composeApp/src/desktopMain/kotlin") {
        include("**/*Store.jvm.kt")
        filter { line -> line.removePrefix("actual ") }
    }
    rename { "Native$it" }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        optIn.add("kotlin.time.ExperimentalTime")
    }
    sourceSets.named("main") {
        kotlin.srcDir("../../app/composeApp/src/commonMain/kotlin")
        kotlin.srcDir("../../app/composeApp/src/desktopMain/kotlin")
        kotlin.srcDir(nativePlatformSources)
        kotlin.include(
            "com/paperconnekt/idea/**",
            "com/paperconnekt/app/legacygraph/GraphModels.kt",
            "com/paperconnekt/app/legacygraph/GraphFiltering.kt",
            "com/paperconnekt/app/legacygraph/GraphSidebarSupport.kt",
            "com/akram/bensalem/bistosh/core/designsystem/component/InteractivePointer.kt",
            "com/akram/bensalem/bistosh/core/designsystem/component/InteractiveShape.kt",
            "com/paperconnekt/app/legacygraph/LegacyGraphAdapter.kt",
            "com/paperconnekt/app/legacygraph/storage/SavedGraphSession.kt",
            "com/paperconnekt/app/legacygraph/components/GraphCanvas.kt",
            "com/paperconnekt/app/legacygraph/components/GraphEngine.kt",
            "com/paperconnekt/app/legacygraph/components/GraphCanvasJewel.kt",
            "com/akram/bensalem/paper/connekt/core/**",
            "com/akram/bensalem/paper/connekt/models/**",
            "com/akram/bensalem/paper/connekt/shared/**",
            "com/akram/bensalem/paper/connekt/utils/**",
            "com/akram/bensalem/paper/connekt/feature/assistant/*.kt",
            "com/akram/bensalem/paper/connekt/feature/assistant/api/**",
            "com/akram/bensalem/paper/connekt/feature/assistant/component/ChatMarkdown.kt",
            "com/akram/bensalem/paper/connekt/feature/assistant/component/LatexImage.kt",
            "com/akram/bensalem/paper/connekt/feature/assistant/component/JvmLatexImage.kt",
            "com/akram/bensalem/paper/connekt/feature/assistant/component/MarkdownJewel.kt",
            "com/akram/bensalem/paper/connekt/feature/assistant/component/NativeChat*.kt",
            "com/akram/bensalem/paper/connekt/feature/mcp/**",
        )
        kotlin.exclude("**/AssistantScreen.kt", "**/NucleusPdfPageProvider.kt", "**/PdfDocument.kt")
        kotlin.exclude(platformSourceNames.map { "**/$it.kt" })
        kotlin.exclude(
            "**/PdfMarkdownStore.jvm.kt", "**/PaperAgentChatStore.jvm.kt", "**/IdeaNoteStore.jvm.kt",
            "**/PaperVectorStore.jvm.kt", "**/ResearchHistoryStore.jvm.kt",
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateNativePlatformSources)
}

dependencies {
    implementation(project(":plugin:native-plugins:idea"))
    implementation(project(":shared:secrets"))
    implementation(project(":shared:knowledge"))
    implementation(project(":shared:mcp"))
    intellijPlatform {
        intellijIdea("2026.2")
        @Suppress("UnstableApiUsage")
        composeUI()
    }
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.4")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)
    implementation(enforcedPlatform("io.ktor:ktor-bom:3.4.3"))
    implementation(libs.napier)
    implementation(libs.koog.agents)
    implementation(libs.koog.agents.additions)
    implementation(libs.koog.http.client.ktor)
    implementation(libs.markdown.renderer.core)
    implementation(libs.markdown.renderer.coil3)
    implementation(libs.jlatexmath)
    implementation("org.apache.pdfbox:pdfbox:3.0.7")
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.datetime)
}

configurations.configureEach {
    // Both compilation and packaging must use IDEA's Compose ABI, not a
    // transitive distribution requested by Markdown or other app libraries.
    exclude(group = "org.jetbrains.compose")
    exclude(group = "org.jetbrains.compose.runtime")
    exclude(group = "org.jetbrains.compose.foundation")
    exclude(group = "org.jetbrains.compose.foundation-layout")
    exclude(group = "org.jetbrains.compose.ui")
    exclude(group = "org.jetbrains.compose.animation")
    exclude(group = "org.jetbrains.compose.material")
    exclude(group = "org.jetbrains.compose.material3")
    exclude(group = "androidx.compose.runtime")
    exclude(group = "androidx.compose.foundation")
    exclude(group = "androidx.compose.ui")
    exclude(group = "androidx.compose.animation")
    exclude(group = "org.jetbrains.skiko")
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2",
        "org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2",
    )
}

configurations.named("runtimeClasspath") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    // IntelliJ bundles kotlinx.serialization and kotlinx.datetime in the
    // platform. Keeping project copies in this plugin creates two classloader
    // identities for serializers such as InstantIso8601Serializer.
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-io")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json-io-jvm")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-datetime")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-datetime-jvm")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.paperconnekt.idea"
        name = "PaperConnekt"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "262"
            untilBuild = "262.*"
        }
    }
    buildSearchableOptions = false
}
