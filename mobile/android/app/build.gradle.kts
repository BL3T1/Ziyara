plugins {
    id("com.android.application")
    // id("kotlin-android")  // REMOVED - migrated to built-in Kotlin
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// ── Signing credentials ───────────────────────────────────────────────────────
// Local dev: set these in android/local.properties (never commit that file).
// CI: set as repository secrets; they are injected as environment variables.
//
// To create the keystore (run once):
//   keytool -genkeypair \
//     -alias ziyara-release \
//     -keyalg RSA -keysize 4096 \
//     -validity 10000 \
//     -keystore ziyara-release.jks \
//     -storepass <KEYSTORE_PASSWORD> \
//     -keypass <KEY_PASSWORD> \
//     -dname "CN=Ziyara, OU=Mobile, O=Ziyara, L=Dubai, ST=Dubai, C=AE"
//
// Then base64-encode it for GitHub:
//   base64 -w 0 ziyara-release.jks > ziyara-release.jks.b64
//   # Paste the contents as secret ANDROID_KEYSTORE_BASE64
//
// Required GitHub Actions secrets:
//   ANDROID_KEYSTORE_BASE64  — base64-encoded .jks file
//   ANDROID_KEY_ALIAS        — ziyara-release
//   ANDROID_KEYSTORE_PASS    — keystore password
//   ANDROID_KEY_PASS         — key password
// ─────────────────────────────────────────────────────────────────────────────

import java.util.Properties
import java.io.FileInputStream

val keystoreProps = Properties()
val keystoreFile = rootProject.file("local.properties")
if (keystoreFile.exists()) keystoreProps.load(FileInputStream(keystoreFile))

fun envOrProp(envKey: String, propKey: String = envKey): String? =
    System.getenv(envKey) ?: keystoreProps.getProperty(propKey)

android {
    namespace = "com.ziyara.app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Required by flutter_local_notifications and other plugins using Java 8+ APIs on older Android
        isCoreLibraryDesugaringEnabled = true
    }

    // kotlinOptions block REMOVED from here

    defaultConfig {
        applicationId = "com.ziyara.app"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    val ksPath: String? = envOrProp("ANDROID_KEYSTORE_PATH", "KEYSTORE_PATH")
    val ksAlias: String? = envOrProp("ANDROID_KEY_ALIAS", "KEY_ALIAS")
    val ksStorePass: String? = envOrProp("ANDROID_KEYSTORE_PASS", "KEYSTORE_PASS")
    val ksKeyPass: String? = envOrProp("ANDROID_KEY_PASS", "KEY_PASS")
    val hasReleaseKey = ksPath != null && ksAlias != null && ksStorePass != null && ksKeyPass != null

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(ksPath!!)
                keyAlias = ksAlias!!
                storePassword = ksStorePass!!
                keyPassword = ksKeyPass!!
            }
        } else {
            logger.warn("[Ziyara] Release signing credentials not found — APK will be signed with debug keys.")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

// ADD THIS AT THE VERY END:
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}