import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.detekt)
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

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
	if (versionPropsFile.exists()) {
		versionPropsFile.inputStream().use { load(it) }
	}
}

val appVersionName = versionProps.getProperty("VERSION_NAME") ?: "0.0.0-dev"
val appVersionCode = versionProps.getProperty("VERSION_CODE")?.toIntOrNull() ?: 1

android {
	namespace = "com.serranoie.app.wear.minus"
	compileSdk {
		version = release(36)
	}

	defaultConfig {
		applicationId = "com.serranoie.app.minus"
		minSdk = 30
		targetSdk = 36
		versionCode = appVersionCode
		versionName = appVersionName
	}

	buildTypes {
		release {
			isMinifyEnabled = false
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
	kotlin {
		compilerOptions {
			jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
		}
	}
	useLibrary("wear-sdk")
	buildFeatures {
		compose = true
	}

	applicationVariants.all {
		outputs.all {
			val output = this as? BaseVariantOutputImpl
			if (output != null) {
				output.outputFileName = "Minus-WearOS-v$appVersionName.apk"
			}
		}
	}
}

dependencies {
	implementation(project(":sync-contract"))
	implementation(libs.play.services.wearable)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.material)
	implementation("androidx.compose.material:material-icons-core") {
		version { strictly("1.7.8") }
	}
	implementation("androidx.compose.material:material-icons-extended") {
		version { strictly("1.7.8") }
	}
	implementation(libs.androidx.wear.compose.material)
	implementation(libs.androidx.compose.foundation)
	implementation(libs.androidx.wear.tooling.preview)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.core.splashscreen)
	implementation("androidx.datastore:datastore-preferences:1.1.7")
	implementation("androidx.work:work-runtime-ktx:2.10.0")
	implementation(libs.kotlinx.serialization.json)
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)

	implementation("androidx.wear.compose:compose-material3:1.6.0-rc01")
	implementation("androidx.wear.compose:compose-foundation:1.6.0-rc01")
	implementation("androidx.wear.compose:compose-navigation:1.6.0-rc01")

	// Logcat
	implementation("com.squareup.logcat:logcat:0.4")
}

detekt {
	buildUponDefaultConfig = true
	config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
	baseline = file("$rootDir/config/detekt/detekt-baseline.xml")
	disableDefaultRuleSets = true
}
