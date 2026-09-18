import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Real signing details, if they exist.
 *
 * Looked for in `~/JApps-Signing/keystore.properties` first -- the one key
 * every app on this machine signs with. One key per author rather than one per
 * app, because the failure that actually matters is losing a keystore: every
 * extra key is another thing whose loss would strand an installed app with no
 * way to ever update it.
 *
 * A copy in the project root still wins if this is a fresh clone somewhere
 * else. Absent both, the build falls back to the debug key.
 */
val releaseKeystore = Properties().apply {
    val shared = rootProject.file(
        System.getProperty("user.home") + "/JApps-Signing/keystore.properties")
    val local = rootProject.file("keystore.properties")
    val file = if (shared.exists()) shared else local
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.remainder.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.remainder.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // The real key when there is one, the debug keystore when there is not.
    //
    // The debug fallback stays so `assembleRelease` works with nothing set up,
    // but it is not what ships: that keystore ships with the SDK and its
    // password is public, so its signature says nothing about who built the
    // APK. A real key is picked up automatically the moment the properties
    // file exists -- see the block above the android { } block.
    signingConfigs {
        create("sideload") {
            storeFile = File(System.getProperty("user.home"), ".android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        // Created only when keystore.properties is present, so the build never
        // demands a key that is not there.
        if (releaseKeystore.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")

                // All three schemes, stated rather than inferred. AGP decides
                // v1 from minSdk and leaves it off at 26 -- right as far as
                // Android goes, which has preferred v2 since 7.0. But this is
                // sideloaded from a file, and the tools someone checks it with
                // can be older than their phone: jarsigner and a good few
                // file-manager installers read v1 and nothing else.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Model.kt, Budget.kt and Cash.kt are plain Kotlin with no Android in
    // them, so the arithmetic is testable on the JVM with no device.
    testImplementation("junit:junit:4.13.2")

    // android.jar stubs org.json, so every call throws "not mocked" under unit
    // tests. This puts a real implementation on the test classpath so the
    // backup file is exercised for real rather than against a stub.
    testImplementation("org.json:json:20240303")
}
