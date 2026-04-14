# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Moshi ---
# Keep generated adapters. Moshi codegen emits *JsonAdapter alongside the data class.
-keep class **.*JsonAdapter { *; }
# Keep @JsonClass-annotated data classes and their members (used via reflection by the adapter).
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *

# --- App domain models ---
-keep class com.example.socketapp.BitcoinTicker { *; }
-keep class com.example.socketapp.CombinedStreamMessage { *; }
-keep class com.example.socketapp.TickerData { *; }

# --- OkHttp / Okio (publicly recommended subset) ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
