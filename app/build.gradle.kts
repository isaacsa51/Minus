plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)

	id("kotlin-android")
	id("dagger.hilt.android.plugin")
	id("com.google.devtools.ksp")
}

android {
	namespace = "com.serranoie.app.minus"
	compileSdk {
		version = release(36)
	}

	defaultConfig {
		applicationId = "com.serranoie.app.minus"
		minSdk = 27
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
	kotlinOptions {
		jvmTarget = "17"
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}

	packaging {
		// Multiple dependency bring these files in. Exclude them to enable
		// our test APK to build (has no effect on our AARs)
		resources.excludes += "/META-INF/AL2.0"
		resources.excludes += "/META-INF/LGPL2.1"
	}
	namespace = "com.serranoie.app.minus"
}

dependencies {
	implementation(project(":sync-contract"))
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.runtime)
	implementation(libs.androidx.compose.material)
	implementation(libs.androidx.compose.material.icons.extended)
	implementation(libs.androidx.wear.compose.material)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.compose.foundation.layout)
	implementation(libs.ui.graphics)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
	implementation("androidx.compose.foundation:foundation:1.8.3")
	implementation("androidx.compose.foundation:foundation-layout:1.8.3")
	implementation("androidx.compose.ui:ui-util:1.8.3")
	implementation("androidx.compose.material3:material3:1.5.0-alpha14")
	implementation("androidx.compose.material3:material3-window-size-class:1.5.0-alpha14")
	implementation("androidx.compose.animation:animation:1.8.3")
	implementation("androidx.compose.ui:ui-tooling-preview:1.8.3")
	implementation("androidx.datastore:datastore-preferences:1.1.7")
	implementation("androidx.recyclerview:recyclerview:1.4.0")
	implementation("androidx.room:room-runtime:2.7.2")
	implementation("androidx.room:room-ktx:2.7.2")
	implementation("androidx.room:room-paging:2.7.2")
	implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
	implementation("androidx.navigation:navigation-compose:2.7.7")
	implementation("com.google.android.gms:play-services-wearable:19.0.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
	implementation("androidx.glance:glance-appwidget:1.1.1")
	implementation("androidx.glance:glance-appwidget-preview:1.1.1")
	implementation("androidx.glance:glance-preview:1.1.1")
	implementation("androidx.core:core-splashscreen:1.0.1")
	implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
	implementation("com.google.dagger:dagger:2.57")
	implementation("com.google.dagger:hilt-android:2.57")
	implementation("org.apache.commons:commons-csv:1.14.0")
	implementation("io.coil-kt:coil-compose:2.7.0")
	ksp("androidx.room:room-compiler:2.7.2")
	ksp("androidx.hilt:hilt-compiler:1.2.0")
	ksp("com.google.dagger:dagger-compiler:2.57")
	ksp("com.google.dagger:hilt-android-compiler:2.57")

	// Glance
	implementation("androidx.glance:glance-appwidget:1.1.1")
	implementation("androidx.glance:glance-material3:1.1.1")

	// WorkManager for notifications
	implementation("androidx.work:work-runtime-ktx:2.10.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
	implementation("androidx.hilt:hilt-work:1.2.0")
	ksp("androidx.hilt:hilt-compiler:1.2.0")

//	implementation("io.github.klassenkonstantin:snappyswipe:0.0.3")

	debugImplementation("androidx.compose.ui:ui-tooling:1.8.3")
	debugImplementation("androidx.compose.ui:ui-test-manifest:1.8.3")
}