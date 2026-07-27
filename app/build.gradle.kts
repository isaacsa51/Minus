import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
    alias(libs.plugins.detekt)

    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/detekt-baseline.xml")
    disableDefaultRuleSets = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

val mockkAgent by configurations.creating {
    isVisible = false
    isCanBeConsumed = false
    isCanBeResolved = true
}

tasks.withType<Test>().configureEach {
    systemProperty("app.cash.paparazzi.differ", "offbytwo")
    systemProperty("paparazzi.maxPercentDifferenceDefault", "5.0")
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
    // Required for MockK / ByteBuddy on JDK 21.
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.time=ALL-UNNAMED"
    )
    jvmArgs("-javaagent:${mockkAgent.files.single { it.name.startsWith("byte-buddy-agent-") }.absolutePath}")
}

fun gitOutput(vararg args: String): String? {
    return runCatching {
        val process =
            ProcessBuilder("git", *args).directory(rootDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() == 0) output.takeIf { it.isNotBlank() } else null
    }.getOrNull()
}

fun normalizedVersionTag(tag: String?): String? {
    val normalizedTag = tag?.removePrefix("refs/tags/") ?: return null
    return normalizedTag.takeIf { it.matches(Regex("^v?\\d+\\.\\d+\\.\\d+(-[A-Za-z0-9.-]+)?$")) }
}

fun versionTagFromEnvOrProperty(): String? =
    System.getProperty("VERSION_TAG")
        ?: System.getenv("VERSION_TAG")
        ?: System.getenv("GITHUB_REF_NAME")

fun releaseVersionName(): String {
    val tag = normalizedVersionTag(versionTagFromEnvOrProperty())
        ?: normalizedVersionTag(
            gitOutput(
                "describe", "--tags", "--exact-match"
            )
        ) ?: normalizedVersionTag(gitOutput("describe", "--tags", "--abbrev=0")) ?: "v0.0.0-dev"

    return tag.removePrefix("v")
}

fun versionCodeFrom(versionName: String): Int {
    val parts = versionName.split("-", limit = 2).first().split(".").map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }

    val code = major * 10_000 + minor * 100 + patch
    return if (code > 0) code else 1
}

val appVersionName = releaseVersionName()
val appVersionCode = versionCodeFrom(appVersionName)

android {
    namespace = "com.serranoie.app.minus"
    compileSdk {
        version = release(36)
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) {
            keystorePropsFile.inputStream().use { stream -> load(stream) }
        }
    }
    val releaseStoreFile = keystoreProps.getProperty("storeFile")?.takeIf { it.isNotBlank() }
    val releaseStorePassword = keystoreProps.getProperty("storePassword")
    val releaseKeyAlias = keystoreProps.getProperty("keyAlias")
    val releaseKeyPassword = keystoreProps.getProperty("keyPassword")
    val hasReleaseSigningConfig = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { v -> !v.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.serranoie.app.minus"
        minSdk = 27
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("foss") {
            dimension = "distribution"
        }
        create("wear") {
            dimension = "distribution"
        }
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = File(releaseStoreFile!!)
                storePassword = releaseStorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "SHOW_LOGS", "true")
            buildConfigField("Boolean", "DEBUG_FEATURES", "true")
            isMinifyEnabled = false
        }

        release {
            buildConfigField("Boolean", "SHOW_LOGS", "false")
            buildConfigField("Boolean", "DEBUG_FEATURES", "false")
            isMinifyEnabled = true
            isShrinkResources = true

            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = false
                isOptimizeCode = true
            }

            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            val output = this as? BaseVariantOutputImpl
            if (output != null) {
                val flavorName = name.replaceFirstChar { it.titlecase() }
                output.outputFileName = if (flavorName.contains("wear", ignoreCase = true)) {
                    "Minus-WearOS-v$appVersionName.apk"
                } else {
                    "Minus-v$appVersionName.apk"
                }
            }
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDir(layout.buildDirectory.dir("generated/source/changelog"))
        }
    }

    packaging {
        resources.excludes += "/META-INF/AL2.0"
        resources.excludes += "/META-INF/LGPL2.1"
        resources.excludes += "/META-INF/LICENSE.md"
        resources.excludes += "/META-INF/LICENSE-notice.md"
    }
    namespace = "com.serranoie.app.minus"

    dependenciesInfo {
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
    }

    androidResources{
        // disable PNG crunching for RB (the aaptOptions.cruncherEnabled = false is deprecated)
        noCompress += listOf("png", "webp")
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateChangelogKotlin)
}

dependencies {
    implementation(project(":sync-contract"))

    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20") {
            because("stdlib pulled in transitively at 2.4.0 by Compose/KSP, compiler on 2.2.x")
        }
        implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.20") {
            because("keep kotlin-reflect aligned with stdlib + compiler")
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui)
    testImplementation(libs.junit)
    testImplementation(libs.touchrobot.core)
    testImplementation(libs.touchrobot.paparazzi)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    "mockkAgent"(libs.byte.buddy.agent)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.foundation.old)
    implementation(libs.androidx.compose.foundation.layout.old)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.tooling.preview.v106)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.hilt.navigationcompose)
    implementation(libs.androidx.navigationcompose)
    "wearImplementation"(libs.play.services.wearable)
    "wearImplementation"(libs.kotlinx.coroutines.playservices)
    implementation(libs.androidx.lifecycle.viewmodelcompose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.dagger)
    implementation(libs.hilt.android)
    implementation(libs.commons.csv)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    ksp(libs.androidx.room.compiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.dagger.compiler)
    ksp(libs.hilt.androidcompiler)

    // WorkManager for notifications
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.hilt.work)

    // Logcat from Square
    implementation(libs.logcat)

    debugImplementation(libs.androidx.compose.ui.tooling.v106)
    debugImplementation(libs.androidx.compose.ui.testmanifest.v183)
}

val prepareReleaseNotes by tasks.registering {
    group = "minus"
    description =
        "Generate fastlane/metadata/android/en-US/changelogs/<versionCode>.txt from git log between tags, then regenerate the compiled-in GeneratedChangelog.kt."

    doLast {
        val versionTag = normalizedVersionTag(versionTagFromEnvOrProperty())
            ?: normalizedVersionTag(gitOutput("describe", "--tags", "--exact-match"))
            ?: normalizedVersionTag(gitOutput("describe", "--tags", "--abbrev=0"))
            ?: throw GradleException(
                "No version tag found. Set VERSION_TAG env var (e.g. VERSION_TAG=v1.3.0) " +
                    "or create a git tag before running this task."
            )

        val versionCode = versionCodeFrom(versionTag.removePrefix("v"))

        val endRef =
            if (gitOutput("rev-parse", "--verify", "--quiet", "$versionTag^{commit}") != null) {
                versionTag
            } else {
                "HEAD"
            }

        val previousTag =
            gitOutput("describe", "--tags", "--abbrev=0", "$versionTag^") ?: gitOutput(
                "describe", "--tags", "--abbrev=0"
            ) ?: ""

        val commits = if (previousTag.isNotBlank()) {
            gitOutput("log", "$previousTag..$endRef", "--pretty=format:- %s")
                ?: "- Initial release."
        } else {
            gitOutput("log", endRef, "--pretty=format:- %s") ?: "- Initial release."
        }

        val changelogContent =
            if (commits.isBlank()) "- Minor improvements and bug fixes." else commits

        val changelogDir = file("$rootDir/fastlane/metadata/android/en-US/changelogs")
        changelogDir.mkdirs()
        val txtFile = File(changelogDir, "$versionCode.txt")
        txtFile.writeText(changelogContent + "\n", Charsets.UTF_8)
        logger.lifecycle(
            "Wrote ${txtFile.path} (${
                commits.lines().count()
            } bullet(s), " + "versionTag=$versionTag, range=$previousTag..$endRef)"
        )

    }

    finalizedBy("generateChangelogKotlin")
}

val generateChangelogKotlin by tasks.registering {
    group = "minus"
    description =
        "Generate app/build/generated/source/changelog/GeneratedChangelog.kt from fastlane/metadata/android/en-US/changelogs/*.txt. The runtime reads the generated Kotlin directly — no assets/changelog.json intermediate."

    val fastlaneDir = file("$rootDir/fastlane/metadata/android/en-US/changelogs")
    val outputDir = layout.buildDirectory.dir("generated/source/changelog")

    inputs.dir(fastlaneDir).withPropertyName("fastlaneChangelogDir")
    outputs.dir(outputDir).withPropertyName("generatedChangelogDir")

    doLast {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()

        val txtFiles = fastlaneDir.takeIf { it.isDirectory }
            ?.listFiles { f -> f.isFile && f.name.endsWith(".txt") }
            ?.sortedByDescending { changelogVersionCodeFromFileName(it.name) }
            ?: emptyList()

        if (txtFiles.isEmpty()) {
            File(outDir, "GeneratedChangelog.kt").writeText(
                """
                package com.serranoie.app.minus.generated

                import com.serranoie.app.minus.domain.model.changelog.VersionRelease

                object GeneratedChangelog {
                    val releases: List<VersionRelease> = emptyList()
                }
                """.trimIndent()
            )
            logger.warn("No changelog .txt files at ${fastlaneDir.path}; emitted empty GeneratedChangelog.kt")
            return@doLast
        }

        val sb = StringBuilder()
        sb.appendLine("// AUTO-GENERATED by :app:generateChangelogKotlin — do not edit by hand.")
        sb.appendLine("// Source of truth: fastlane/metadata/android/en-US/changelogs/*.txt")
        sb.appendLine("// Regenerate via: ./gradlew :app:generateChangelogKotlin")
        sb.appendLine()
        sb.appendLine("package com.serranoie.app.minus.generated")
        sb.appendLine()
        sb.appendLine("import com.serranoie.app.minus.domain.model.changelog.ChangelogItem")
        sb.appendLine("import com.serranoie.app.minus.domain.model.changelog.ReleaseType")
        sb.appendLine("import com.serranoie.app.minus.domain.model.changelog.VersionRelease")
        sb.appendLine()
        sb.appendLine("object GeneratedChangelog {")
        sb.appendLine("    val releases: List<VersionRelease> = listOf(")

        var isFirst = true
        for (txt in txtFiles) {
            val versionCode = changelogVersionCodeFromFileName(txt.name)
            if (versionCode <= 0) continue
            val versionName = changelogVersionCodeToName(versionCode)
            val releaseDate = changelogResolveReleaseDate(versionName)
            if (releaseDate == null) {
                logger.warn("Skipping ${txt.name} — tag v$versionName not found locally (skip pre-release tags like 1.3.2-pre)")
                continue
            }

            val items = parseChangelogTxtItems(txt.readText())
            if (items.isEmpty()) continue

            if (!isFirst) sb.appendLine(",")
            sb.append("        VersionRelease(")
            sb.append("versionCode = $versionCode, ")
            sb.append("versionName = ${kotlinStringLiteral(versionName)}, ")
            sb.append("releaseDate = ${kotlinStringLiteral(releaseDate)},")
            sb.appendLine()
            sb.appendLine("            items = listOf(")

            for ((idx, item) in items.withIndex()) {
                sb.append("                ChangelogItem(")
                sb.append("title = ${kotlinStringLiteral(item["title"] as String)}, ")
                sb.append("type = ReleaseType.${item["type"]}")
                sb.append(")")
                if (idx < items.lastIndex) sb.append(",")
                sb.appendLine()
            }

            sb.append("            )")
            sb.append(")")
            isFirst = false
        }

        sb.appendLine()
        sb.appendLine("    )")
        sb.appendLine("}")

        File(outDir, "GeneratedChangelog.kt").writeText(sb.toString())
        logger.lifecycle("Generated ${txtFiles.size} release(s) into ${outDir.name}")
    }
}

fun changelogVersionCodeFromFileName(fileName: String): Int =
    fileName.substringBeforeLast('.').toIntOrNull() ?: 0

fun changelogVersionCodeToName(code: Int): String {
    val major = code / 10000
    val minor = (code % 10000) / 100
    val patch = code % 100
    return "$major.$minor.$patch"
}

fun parseChangelogTxtItems(text: String): List<Map<String, Any?>> {
    val items = mutableListOf<Map<String, Any?>>()
    val summaryPatterns = listOf(
        Regex("^initial (public )?release\\.?$", RegexOption.IGNORE_CASE),
        Regex("^first (public )?release\\.?$", RegexOption.IGNORE_CASE),
        Regex("^(bug fixes and )?various improvements\\.?$", RegexOption.IGNORE_CASE),
        Regex("^minor improvements( and bug fixes)?\\.?$", RegexOption.IGNORE_CASE),
        Regex("^stability improvements\\.?$", RegexOption.IGNORE_CASE),
        Regex("^chore\\b.*", RegexOption.IGNORE_CASE),
    )

    for (rawLine in text.lines()) {
        val line = rawLine.trim().trimEnd(',', ' ')
        if (line.isEmpty()) continue

        val bulletContent = when {
            line.startsWith("- ") -> line.removePrefix("- ")
            line.startsWith("* ") -> line.removePrefix("* ")
            else -> continue
        }
        val cleaned = bulletContent.trim().trimEnd('.').trim()
        if (cleaned.isEmpty()) continue
        if (summaryPatterns.any { it.matches(cleaned) }) continue

        items.add(
            mapOf(
                "title" to humanizeChangelogTitle(cleaned),
                "type" to classifyChangelogType(cleaned),
            )
        )
    }
    return items
}

fun classifyChangelogType(text: String): String {
    val lower = text.lowercase()
    val prefixType = when {
        lower.startsWith("feat:") || lower.startsWith("feat(") -> "FEATURE"
        lower.startsWith("fix:") || lower.startsWith("fix(") || lower.startsWith("bug:") || lower.startsWith(
            "bug("
        ) -> "BUG_FIX"

        lower.startsWith("refactor:") || lower.startsWith("refactor(") || lower.startsWith("perf:") || lower.startsWith(
            "perf("
        ) || lower.startsWith("improve:") || lower.startsWith("improve(") -> "IMPROVEMENT"

        else -> null
    }
    if (prefixType != null) return prefixType

    return when {
        lower.contains("fix") || lower.contains("bug") || lower.contains("crash") || lower.contains(
            "resolve"
        ) || lower.contains("issue") || lower.contains("patch") -> "BUG_FIX"

        else -> "IMPROVEMENT"
    }
}

fun humanizeChangelogTitle(text: String): String {
    var t = text
    val conventionalPrefixes = listOf(
        "feat:", "feat(",
        "fix:", "fix(",
        "bug:", "bug(",
        "refactor:", "refactor(",
        "perf:", "perf(",
        "improve:", "improve(",
    )
    for (prefix in conventionalPrefixes) {
        if (t.lowercase().startsWith(prefix)) {
            val remainder = t.substring(prefix.length)
            t = if (prefix.endsWith("(")) {
                // Conventional Commits `feat(scope): rest` form
                val close = remainder.indexOf("):")
                if (close >= 0) remainder.substring(close + 2) else remainder
            } else {
                remainder
            }
            break
        }
    }
    t = t.trim()
    if (t.isEmpty()) return t
    return t[0].uppercaseChar() + t.substring(1)
}

fun changelogResolveReleaseDate(versionName: String): String? {
    val tag = "v$versionName"
    val gitDate = gitOutput("log", "-1", "--format=%cs", tag)
    return gitDate
}

private fun changelogTodayIsoDate(): String {
    val daysSinceEpoch = System.currentTimeMillis() / 86_400_000L
    val z = daysSinceEpoch + 719468L
    val era = if (z < 0L) (z - 146096L) / 146097L else z / 146097L
    val doe = z - era * 146097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
    val y = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val d = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
    val m = if (mp < 10L) (mp + 3L).toInt() else (mp - 9L).toInt()
    val year = if (m <= 2) (y + 1L).toInt() else y.toInt()
    return "%04d-%02d-%02d".format(year, m, d)
}

fun kotlinStringLiteral(s: String): String {
    val escaped = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("$", "\\$")
    return "\"$escaped\""
}
