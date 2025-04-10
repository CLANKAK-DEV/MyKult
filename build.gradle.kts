// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    alias(libs.plugins.google.gms.google.services) apply false

}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.0")
        classpath ("com.android.tools.build:gradle:4.0.2") // This is the AGP version// Match your Kotlin version
    }
}