# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/mikealexeev/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# VoltDB configuration
-keep class org.voltdb.** { *; }
-keep interface org.voltdb.** { *; }
-keepattributes InnerClasses
-keep class org.voltdb.VoltResponse**
-keepclassmembers class org.voltdb.VoltResponse** { *; }

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepattributes Signature
-keepattributes Exceptions

-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# okhttp
-dontwarn rx.**
-dontwarn okio.**

-dontwarn com.squareup.okhttp.**
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }

#gson
-keepattributes SourceFile, *Annotation*, InnerClasses, Signature
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

-dontwarn java.nio.**

-dontwarn android.support.v4.**
-keep class android.support.v7.** { *; }
-keep class android.support.v4.** { *; }
