# Ziyara Android — ProGuard / R8 rules
# ─────────────────────────────────────────────────────────────────────────────

# Keep all app classes
-keep class com.ziyara.app.** { *; }

# Dio / OkHttp networking
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Flutter embedding
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# Preserve generic type information (required by Dio/Retrofit serialisation)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# local_auth (biometrics)
-keep class androidx.biometric.** { *; }

# flutter_secure_storage
-keep class com.it_nomads.fluttersecurestorage.** { *; }

# STOMP WebSocket client
-keep class ua.naiksoftware.stomp.** { *; }
-keep class com.stomp.** { *; }
