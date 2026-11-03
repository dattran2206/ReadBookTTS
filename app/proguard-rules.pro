# Proguard Rules for Wordwave

# Sherpa-ONNX JNI classes
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**

# Google Play Services Auth
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# Domain Models
-keep class com.vietreader.tts.domain.model.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# PDFBox-Android rules
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn com.tom_roush.fontbox.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
