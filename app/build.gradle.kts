import groovy.json.JsonSlurper

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

// Paparazzi snapshot tests run in the test JVM. These system properties must be
// passed explicitly to that JVM (gradle.properties alone only affects Gradle's
// own JVM, not forked test JVMs).
//
// `app.cash.paparazzi.differ=offbytwo`  — allows 2-pixel offset per pixel
// `paparazzi.maxPercentDifferenceDefault` — up to 5% of pixels may differ
//
// Together these absorb cross-platform rasterization drift (Linux CI vs
// Windows/macOS dev machines) without hiding real layout regressions.
tasks.withType<Test>().configureEach {
    systemProperty("app.cash.paparazzi.differ", "offbytwo")
    systemProperty("paparazzi.maxPercentDifferenceDefault", "5.0")
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

fun releaseVersionName(): String {
    val tag = normalizedVersionTag(System.getenv("VERSION_TAG"))
        ?: normalizedVersionTag(System.getenv("GITHUB_REF_NAME")) ?: normalizedVersionTag(
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

    val releaseStoreFile = System.getenv("MINUS_RELEASE_STORE_FILE")
    val hasReleaseSigningConfig = listOf(
        releaseStoreFile,
        System.getenv("MINUS_RELEASE_STORE_PASSWORD"),
        System.getenv("MINUS_RELEASE_KEY_ALIAS"),
        System.getenv("MINUS_RELEASE_KEY_PASSWORD")
    ).all { !it.isNullOrBlank() }

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
                storeFile = file(releaseStoreFile!!)
                storePassword = System.getenv("MINUS_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("MINUS_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("MINUS_RELEASE_KEY_PASSWORD")
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
}

dependencies {
    implementation(project(":sync-contract"))

    implementation(libs.androidx.core.ktx)
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

/*
 * Single source of truth for release notes is fastlane/metadata/android/en-US/
 * changelogs/<versionCode>.txt. Two Gradle tasks produce the runtime asset:
 *
 *   :app:prepareReleaseNotes      — writes the <versionCode>.txt from
 *                                   `git log` between tags (mirrors fastlane's
 *                                   :generate_changelog lane). Run this BEFORE
 *                                   tagging so the committed JSON is
 *                                   reproducible across IzzyOnDroid / F-Droid.
 *   :app:generateAppChangelogJson — reads those .txt files (+ optional
 *                                   fastlane/changelogs/<versionName>.json
 *                                   overrides) and writes
 *                                   app/src/main/assets/changelog.json so
 *                                   ChangelogRepository.kt has structured
 *                                   release notes without anyone hand-editing
 *                                   JSON.
 *
 * prepareReleaseNotes.finalizedBy(generateAppChangelogJson) so the canonical
 * workflow is a single command.
 */
val prepareReleaseNotes by tasks.registering {
    group = "minus"
    description =
        "Generate fastlane/metadata/android/en-US/changelogs/<versionCode>.txt from git log between tags, then regenerate app/src/main/assets/changelog.json."

    doLast {
        val versionTag = normalizedVersionTag(System.getenv("VERSION_TAG")) ?: normalizedVersionTag(
            System.getenv("GITHUB_REF_NAME")
        ) ?: normalizedVersionTag(gitOutput("describe", "--tags", "--exact-match"))
        ?: normalizedVersionTag(gitOutput("describe", "--tags", "--abbrev=0"))
        ?: throw GradleException(
            "No version tag found. Set VERSION_TAG env var (e.g. VERSION_TAG=v1.3.0) " + "or create a git tag before running this task."
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
        txtFile.writeText(changelogContent + "\n")
        logger.lifecycle(
            "Wrote ${txtFile.path} (${
                commits.lines().count()
            } bullet(s), " + "versionTag=$versionTag, range=$previousTag..$endRef)"
        )

        val ghAvailable = runCatching {
            val proc =
                ProcessBuilder("gh", "auth", "status").directory(rootDir).redirectErrorStream(true)
                    .start()
            proc.inputStream.bufferedReader().readText()
            proc.waitFor() == 0
        }.getOrDefault(false)

        if (ghAvailable) {
            project.extra["minus.changelog.enrichPrBodies"] = true
            logger.lifecycle(
                "gh authenticated — will auto-fetch PR bodies for items with (#NN) references"
            )
        } else {
            logger.warn("gh not authenticated or not installed — PR bodies will NOT be auto-fetched.")
            logger.warn("  To enable: run 'gh auth login', then re-run this task.")
            logger.warn("  Or set MINUS_CHANGELOG_ENRICH_PR_BODIES=true to force.")
        }
    }

    finalizedBy("generateAppChangelogJson")
}

val generateAppChangelogJson by tasks.registering {
    group = "minus"
    description =
        "Generate app/src/main/assets/changelog.json from fastlane/metadata/android/en-US/changelogs/*.txt"

    doLast {
        val changelogDir = file("$rootDir/fastlane/metadata/android/en-US/changelogs")
        val overrideDir = file("$rootDir/fastlane/changelogs")
        val outputFile = file("src/main/assets/changelog.json")

        outputFile.parentFile.mkdirs()

        if (!changelogDir.exists() || !changelogDir.isDirectory) {
            outputFile.writeText("[]\n")
            logger.warn("Fastlane changelog directory not found at ${changelogDir.path}; wrote empty changelog.json.")
            return@doLast
        }

        val txtFiles = changelogDir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }?.toList()
            ?.sortedByDescending { changelogVersionCodeFromFileName(it.name) } ?: emptyList()

        val shouldEnrich = shouldEnrichChangelogPrBodies()

        val releases = mutableListOf<Map<String, Any?>>()
        var totalItemsWithImages = 0
        for (txt in txtFiles) {
            val versionCode = changelogVersionCodeFromFileName(txt.name)
            if (versionCode <= 0) continue
            val versionName = changelogVersionCodeToName(versionCode)

            val release =
                buildChangelogRelease(txt, versionCode, versionName, overrideDir, shouldEnrich)
            if (release != null) {
                @Suppress("UNCHECKED_CAST") totalItemsWithImages += (release["items"] as List<Map<String, Any?>>).count { (it["imageName"] as? String)?.isNotBlank() == true }
                releases.add(release)
            }
        }

        outputFile.writeText(buildChangelogReleasesJson(releases))
        val enrichNote = if (shouldEnrich) " (PR bodies enriched)" else ""
        logger.lifecycle(
            "Generated ${outputFile.path} with ${releases.size} releases, " + "$totalItemsWithImages item(s) with images$enrichNote"
        )
    }
}

fun shouldEnrichChangelogPrBodies(): Boolean =
    System.getenv("MINUS_CHANGELOG_ENRICH_PR_BODIES") == "true" || project.extra.properties["minus.changelog.enrichPrBodies"] == true

fun changelogVersionCodeFromFileName(fileName: String): Int =
    fileName.substringBeforeLast('.').toIntOrNull() ?: 0

fun changelogVersionCodeToName(code: Int): String {
    val major = code / 10000
    val minor = (code % 10000) / 100
    val patch = code % 100
    return "$major.$minor.$patch"
}

fun buildChangelogRelease(
    txt: File,
    versionCode: Int,
    versionName: String,
    overrideDir: File,
    enrichPrBodies: Boolean = false,
): Map<String, Any?>? {
    val overrideFile = File(overrideDir, "$versionName.json")
    val versionShort = versionName.replace(".", "_")
    val imagesByPr = changelogScanChangelogImages(versionShort)

    if (overrideFile.exists()) {
        val overridden = readChangelogOverride(overrideFile, versionCode, versionName)
        if (overridden != null) {
            @Suppress("UNCHECKED_CAST") val items =
                (overridden["items"] as? List<Map<String, Any?>>)?.map {
                    changelogAutoFillImageName(
                        it, imagesByPr
                    )
                } ?: emptyList()
            return overridden.toMutableMap().apply { this["items"] = items }
        }
        logger.warn("Override ${overrideFile.path} could not be parsed; falling back to ${txt.name}.")
    }

    val items = parseChangelogTxtItems(txt.readText())
    if (items.isEmpty()) return null

    val enrichedItems = if (enrichPrBodies) {
        changelogEnrichItemsWithPrBodies(items)
    } else {
        items
    }

    val finalItems = enrichedItems.map { changelogAutoFillImageName(it, imagesByPr) }

    return mapOf(
        "versionCode" to versionCode,
        "versionName" to versionName,
        "releaseDate" to changelogResolveReleaseDate(versionName, overrideDir),
        "items" to finalItems,
    )
}

/**
 * Scans the standard Android drawable folders for changelog images matching
 * the convention `changelog_<versionShort>_<PR_NUMBER>.<ext>`. Returns a map
 * of `PR number -> resource name` (without extension) for use in auto-filling
 * the `imageName` field on items whose title contains the same `(#NN)`
 * reference.
 *
 * Convention examples (for version "1.3.0", versionShort = "1_3_0"):
 *   changelog_1_3_0_46.webp   →  PR #46  →  "changelog_1_3_0_46"
 *   changelog_1_3_0_44.gif    →  PR #44  →  "changelog_1_3_0_44"
 *
 * Scans `drawable/` AND `drawable-nodpi/` (the two folders Android developers
 * typically use for changelog screenshots, since they're display-once and
 * don't need density scaling). Other density-qualified folders are skipped.
 *
 * Returns an empty map if no folders exist or no matching files are found.
 * Silently ignores files that don't match the strict PR-number pattern —
 * descriptive names like `changelog_1_3_0_chart.webp` are not auto-mapped
 * (wire those via the override JSON's `imageName` field instead).
 */
fun changelogScanChangelogImages(versionShort: String): Map<Int, String> {
    val resDir = file("$rootDir/app/src/main/res")
    val drawableFolders = listOf("drawable", "drawable-nodpi").mapNotNull { name ->
        val dir = File(resDir, name)
        if (dir.isDirectory) dir else null
    }
    if (drawableFolders.isEmpty()) return emptyMap()

    val pattern = Regex("^changelog_${versionShort}_(\\d+)\\.(webp|png|jpg|jpeg|gif)$")
    val result = mutableMapOf<Int, String>()
    for (folder in drawableFolders) {
        folder.listFiles()?.forEach { f ->
            val match = pattern.matchEntire(f.name) ?: return@forEach
            val prNumber = match.groupValues[1].toIntOrNull() ?: return@forEach
            if (prNumber !in result || folder.name == "drawable-nodpi") {
                result[prNumber] = f.nameWithoutExtension
            }
        }
    }
    return result
}

fun changelogAutoFillImageName(
    item: Map<String, Any?>,
    imagesByPr: Map<Int, String>,
): Map<String, Any?> {
    val currentImageName = item["imageName"] as? String
    if (!currentImageName.isNullOrBlank()) return item

    val title = item["title"] as? String ?: return item
    val prNumber = changelogExtractPrNumber(title) ?: return item
    val autoImage = imagesByPr[prNumber] ?: return item

    logger.lifecycle("Changelog: auto-mapped image '$autoImage' to PR #$prNumber")
    return item.toMutableMap().apply { this["imageName"] = autoImage }
}

fun readChangelogOverride(
    overrideFile: File, defaultVersionCode: Int, defaultVersionName: String
): Map<String, Any?>? {
    return try {
        @Suppress("UNCHECKED_CAST") val parsed =
            JsonSlurper().parse(overrideFile) as? Map<String, Any?> ?: return null

        @Suppress("UNCHECKED_CAST") val items =
            (parsed["items"] as? List<Map<String, Any?>>)?.map { normalizeChangelogItem(it) }
                ?: emptyList()

        if (parsed.containsKey("items") && items.isEmpty()) return null

        mapOf(
            "versionCode" to (parsed["versionCode"] as? Int ?: defaultVersionCode),
            "versionName" to (parsed["versionName"] as? String ?: defaultVersionName),
            "releaseDate" to (parsed["releaseDate"] as? String ?: changelogResolveReleaseDate(
                defaultVersionName, overrideFile.parentFile
            )),
            "items" to items,
        )
    } catch (e: Exception) {
        logger.warn("Failed to parse override ${overrideFile.path}: ${e.message}")
        null
    }
}

fun normalizeChangelogItem(raw: Map<String, Any?>): Map<String, Any?> = mapOf(
    "title" to (raw["title"] as? String ?: ""),
    "description" to raw["description"] as? String,
    "type" to (raw["type"] as? String ?: "IMPROVEMENT"),
    "imageName" to (raw["imageName"] as? String),
)

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
                "description" to null,
                "type" to classifyChangelogType(cleaned),
                "imageName" to null,
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

fun changelogResolveReleaseDate(versionName: String, overrideDir: File): String {
    val overrideFile = File(overrideDir, "$versionName.json")
    if (overrideFile.exists()) {
        try {
            @Suppress("UNCHECKED_CAST") val parsed =
                JsonSlurper().parse(overrideFile) as? Map<String, Any?>
            val date = parsed?.get("releaseDate") as? String
            if (!date.isNullOrBlank()) return date
        } catch (_: Exception) {
            // Fall through to git / today
        }
    }
    val tag = "v$versionName"
    val gitDate = gitOutput("log", "-1", "--format=%cs", tag)
    if (!gitDate.isNullOrBlank()) return gitDate
    return changelogTodayIsoDate()
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

private val CHANGELOG_PR_NUMBER_REGEX = Regex("""\(#(\d+)\)""")

private fun changelogExtractPrNumber(title: String): Int? =
    CHANGELOG_PR_NUMBER_REGEX.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

private fun changelogFetchPrBody(prNumber: Int): String? {
    val proc = try {
        ProcessBuilder(
            "gh",
            "pr",
            "view",
            prNumber.toString(),
            "--json",
            "body",
            "--jq",
            ".body",
        ).directory(rootDir).redirectErrorStream(true).start()
    } catch (e: Exception) {
        return null
    }

    val deadline = System.currentTimeMillis() + 15_000L
    while (proc.isAlive && System.currentTimeMillis() < deadline) {
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            proc.destroy()
            return null
        }
    }
    if (proc.isAlive) {
        proc.destroy()
        logger.warn("changelog: gh pr view #$prNumber timed out after 15s")
        return null
    }

    val output = proc.inputStream.bufferedReader().readText().trim()
    if (proc.exitValue() != 0) {
        return null
    }
    if (output.isBlank() || output == "null") return null
    return changelogFilterPrBody(output)
}

/**
 * Strips boilerplate sections from a PR body so the in-app changelog card
 * shows only the "what changed" prose. PR templates often include sections
 * like "## Working demo", "## Testing notes", "## Notes" that add visual
 * noise without informing the user about the release.
 *
 * Walks the body line by line and tracks which section we're in. When a
 * `# / ## / ###` header matches a boilerplate keyword (demo, testing,
 * notes, etc.) we skip it AND every body line that follows until the next
 * header. Non-boilerplate headers and their body lines are preserved as-is
 * so the Markdown renderer in the card can still style them as headings.
 *
 * Returns null when the result is empty (every section was boilerplate),
 * so the card falls back to its title-only rendering.
 */
private fun changelogFilterPrBody(body: String): String? {
    val trimmed = body.trim()
    if (trimmed.isBlank()) return null

    val boilerplateKeywords = listOf(
        "demo", "screenshot", "video", "preview", "recording",
        "test", "testing", "qa", "how to test",
        "note", "notes",
        "checklist", "todo",
    )
    val headerRegex = Regex("^#{1,3}\\s+")

    val lines = mutableListOf<String>()
    var skipUntilNextHeader = false

    for (line in trimmed.lines()) {
        if (headerRegex.containsMatchIn(line)) {
            val headerText = line.replace(headerRegex, "").trim().lowercase()
            skipUntilNextHeader = boilerplateKeywords.any { headerText.contains(it) }
        }
        if (!skipUntilNextHeader) {
            lines.add(line)
        }
    }

    return lines.joinToString("\n").trim().takeIf { it.isNotBlank() }
}

private fun changelogEnrichItemWithPrBody(item: Map<String, Any?>): Map<String, Any?> {
    val title = item["title"] as? String ?: return item
    val description = item["description"] as? String
    if (!description.isNullOrBlank()) return item

    val prNumber = changelogExtractPrNumber(title) ?: return item
    val body = changelogFetchPrBody(prNumber) ?: return item

    return item.toMutableMap().apply {
        this["description"] = body
    }
}

private fun changelogEnrichItemsWithPrBodies(
    items: List<Map<String, Any?>>,
): List<Map<String, Any?>> = items.map { changelogEnrichItemWithPrBody(it) }

fun buildChangelogReleasesJson(releases: List<Map<String, Any?>>): String {
    if (releases.isEmpty()) return "[]\n"
    val sb = StringBuilder()
    sb.append("[\n")
    for ((i, release) in releases.withIndex()) {
        if (i > 0) sb.append(",\n")
        sb.append("  {\n")
        sb.append("    \"versionCode\": ${release["versionCode"]},\n")
        sb.append("    \"versionName\": ${changelogJsonString(release["versionName"] as String)},\n")
        sb.append("    \"releaseDate\": ${changelogJsonString(release["releaseDate"] as String)},\n")
        sb.append("    \"items\": ")
        @Suppress("UNCHECKED_CAST") val items = release["items"] as List<Map<String, Any?>>
        sb.append(buildChangelogItemsJson(items))
        sb.append("\n  }")
    }
    sb.append("\n]\n")
    return sb.toString()
}

fun buildChangelogItemsJson(items: List<Map<String, Any?>>): String {
    if (items.isEmpty()) return "[]"
    val sb = StringBuilder()
    sb.append("[\n")
    for ((i, item) in items.withIndex()) {
        if (i > 0) sb.append(",\n")
        sb.append("      {\n")
        sb.append("        \"title\": ${changelogJsonString(item["title"] as String)},\n")
        sb.append("        \"description\": ${changelogJsonStringOrNull(item["description"] as String?)},\n")
        sb.append("        \"type\": ${changelogJsonString(item["type"] as String)},\n")
        sb.append("        \"imageName\": ${changelogJsonStringOrNull(item["imageName"] as String?)}")
        sb.append("\n      }")
    }
    sb.append("\n    ]")
    return sb.toString()
}

fun changelogJsonString(s: String): String {
    val escaped =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
            .replace("\t", "\\t")
    return "\"$escaped\""
}

fun changelogJsonStringOrNull(s: String?): String =
    if (s == null) "null" else changelogJsonString(s)
