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

# Keep line numbers so Sentry stack traces stay readable in release builds.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip debug/verbose/info logging from release builds so request details
# (URLs, asset IDs) never reach logcat in production. Log.w/Log.e are kept.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# kotlinx.serialization: keep generated serializers for @Serializable classes
# (SettingsExport, type-safe navigation routes) that are looked up reflectively.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.rrajath.cull.** {
    *** Companion;
}
-keepclasseswithmembers class com.rrajath.cull.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rrajath.cull.**$$serializer { *; }