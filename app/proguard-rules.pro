# Keep models
-keep class com.ariai.app.data.models.** { *; }
-keep class com.ariai.app.data.local.** { *; }

# OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Serialization
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**

# Coil
-keep class coil.** { *; }
