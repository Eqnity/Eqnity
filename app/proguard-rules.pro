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

# Keep Room entities
-keep class com.eqnitymusic.damn.data.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }

# Keep Media3 classes
-keep class androidx.media3.** { *; }

# Keep Glide
-keep public class com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Keep Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep ViewBinding
-keep class com.eqnitymusic.damn.databinding.** { *; }

# Keep Navigation
-keep class androidx.navigation.** { *; }

# Keep Material Components
-keep class com.google.android.material.** { *; }

# Keep ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.exoplayer.** { *; }