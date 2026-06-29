# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Gson 模型通过反射读写字段，保留模型类与字段名，避免 release 混淆后解析失败。
-keep class com.krelinnbios.neodblite.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
