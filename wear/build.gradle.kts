plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
}

android {
	namespace = "com.serranoie.app.wear.minus"
	compileSdk {
		version = release(36)
	}

	defaultConfig {
		applicationId = "com.serranoie.app.minus"
		minSdk = 30
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

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
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions {
		jvmTarget = "11"
	}
	useLibrary("wear-sdk")
	buildFeatures {
		compose = true
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
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)

	implementation("androidx.wear.compose:compose-material3:1.6.0-rc01")
	implementation("androidx.wear.compose:compose-foundation:1.6.0-rc01")
	implementation("androidx.wear.compose:compose-navigation:1.6.0-rc01")
}