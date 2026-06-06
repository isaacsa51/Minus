plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)

	id("dagger.hilt.android.plugin")
	id("com.google.devtools.ksp")
}

fun gitOutput(vararg args: String): String? {
	return runCatching {
		val process = ProcessBuilder("git", *args)
			.directory(rootDir)
			.redirectErrorStream(true)
			.start()
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
		?: normalizedVersionTag(System.getenv("GITHUB_REF_NAME"))
		?: normalizedVersionTag(gitOutput("describe", "--tags", "--exact-match"))
		?: normalizedVersionTag(gitOutput("describe", "--tags", "--abbrev=0"))
		?: "v0.0.0-dev"

	return tag.removePrefix("v")
}

fun versionCodeFrom(versionName: String): Int {
	val parts = versionName.split("-", limit = 2).first()
		.split(".")
		.map { it.toIntOrNull() ?: 0 }
	val major = parts.getOrElse(0) { 0 }
	val minor = parts.getOrElse(1) { 0 }
	val patch = parts.getOrElse(2) { 0 }

	return major * 10_000 + minor * 100 + patch
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
			if (hasReleaseSigningConfig) {
				signingConfig = signingConfigs.getByName("release")
			}
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = "17"
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}

	packaging {
		resources.excludes += "/META-INF/AL2.0"
		resources.excludes += "/META-INF/LGPL2.1"
	}
	namespace = "com.serranoie.app.minus"

	dependenciesInfo {
		// Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
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
	implementation(libs.ui.graphics)
	implementation(libs.androidx.foundation)
	implementation(libs.androidx.ui)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
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
	implementation(libs.androidx.recyclerview)
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
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.accompanist.systemuicontroller)
	implementation(libs.dagger)
	implementation(libs.hilt.android)
	implementation(libs.commons.csv)
	implementation(libs.coil.compose)
	ksp(libs.androidx.room.compiler)
	ksp(libs.androidx.hilt.compiler)
	ksp(libs.dagger.compiler)
	ksp(libs.hilt.androidcompiler)

	// Glance
	implementation(libs.androidx.glance.appwidget)
	implementation(libs.androidx.glance.material3)

	// WorkManager for notifications
	implementation(libs.androidx.work.runtime.ktx)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.androidx.hilt.work)
	ksp(libs.androidx.hilt.compiler)

	// Logcat from Square
	implementation(libs.logcat)

	debugImplementation(libs.androidx.compose.ui.tooling.v106)
	debugImplementation(libs.androidx.compose.ui.testmanifest.v183)

	implementation(libs.androidx.compose.material3.windowsize)
}
