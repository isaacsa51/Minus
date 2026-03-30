plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

group = "com.serranoie.app.minus"
version = "1.0"


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
