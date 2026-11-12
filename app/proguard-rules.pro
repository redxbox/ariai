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

# kotlinx.serialization is not a dependency (JSON is org.json), so no rules are needed for it.

# PdfBox's optional JP2 decoder is not needed for PDF text extraction.
-dontwarn com.gemalto.jp2.**

# Tesseract's native JNI entry points use these Java wrapper names.
-keep class com.googlecode.tesseract.android.** { *; }
