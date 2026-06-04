# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep data classes used with kotlinx.serialization
-keepattributes *Annotation*
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Retrofit interfaces
-keep interface de.gartenflora.data.remote.** { *; }

# Keep Room entities
-keep class de.gartenflora.data.local.** { *; }

# Kotlin serialization
-keepattributes EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
