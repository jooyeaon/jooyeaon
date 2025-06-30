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

####### 1
## Don't note duplicate definition (Legacy Apche Http Client)
#-dontnote android.net.http.*
#-dontnote org.apache.http.**
#
## Firebase Authentication
#-keepattributes *Annotation*
#
#-dontwarn okhttp3.**
#-dontwarn okio.**
#
#-dontnote okhttp3.**


#######2

# Don't note duplicate definition (Legacy Apche Http Client)
-dontnote android.net.http.*
-dontnote org.apache.http.**

# Firebase Authentication
-keepattributes *Annotation*

-dontwarn okhttp3.**
-dontwarn okio.**

-dontnote okhttp3.**

-keepnames class com.covision.utils.*

#-dontwarn com.google.android.gms.**
#-dontwarn om.squareup.okhttp3.** #implementation 'com.squareup.okhttp3:okhttp:3.9.0'

-keepattributes SetJavaScriptEnabled
-keepattributes JavascriptInterface

-ignorewarnings
-keep class * { public private *; }

-keepclassmembers class com.covision.moapp.TabFragmentMO {
    <methods>;
}
-keepclassmembers class com.covision.moapp.TabFragmentNotice {
	<methods>;
}
-keepclassmembers class com.covision.moapp.TabFragmentOrg {
	<methods>;
}
-keepclassmembers class com.covision.moapp.TabFragmentPortal {
	<methods>;
}