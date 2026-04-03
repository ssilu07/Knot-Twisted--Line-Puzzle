# WebView JS Bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Keep game classes
-keep class com.tangledline.game.** { *; }
-keep class com.tangledline.game.MainActivity$GameBridge { *; }

# AdMob / Google Play Services
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Keep Annotation
-keepattributes *Annotation*
-keepattributes Signature
