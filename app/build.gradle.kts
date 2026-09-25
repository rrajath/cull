plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"

    id("io.sentry.android.gradle") version "6.14.0"
}

// Single source of truth for the app version, bumped by hand on release (semver).
// Reused for versionName, the Sentry release manifest placeholder, and (via
// `printVersionName`) the release workflow, which checks the pushed tag matches it.
val appVersionName = "1.0.58"

// versionCode = MAJOR * 10000 + MINOR * 100 + PATCH (1.2.3 -> 10203)
val appVersionCode = run {
    val parts = appVersionName.split(".").map { it.toIntOrNull() }
    require(parts.size == 3 && parts.all { it != null && it >= 0 }) {
        "versionName must be MAJOR.MINOR.PATCH, got \"$appVersionName\""
    }
    val (major, minor, patch) = parts.map { it!! }
    require(minor <= 99 && patch <= 99) {
        "MINOR and PATCH must be <= 99 for the versionCode scheme, got \"$appVersionName\""
    }
    major * 10000 + minor * 100 + patch
}

android {
    namespace = "com.rrajath.cull"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rrajath.cull"
        minSdk = 34
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        manifestPlaceholders["sentryRelease"] = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Debug installs alongside release: distinct package, launcher name, and version label
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Cull Debug")
            versionNameSuffix = " (debug)"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Falls back to debug signing locally when no keystore secrets are present
            // (e.g. on a developer machine) so `assembleRelease` still works without CI secrets.
            signingConfig = if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // AGP 9 disables resValue by default; needed for the debug app_name override
        resValues = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register("printVersionName") {
    doLast {
        println(appVersionName)
    }
}

sentry {
    org.set("rajath-ramakrishna")
    projectName.set("cull")

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)
}
