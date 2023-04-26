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

-dontwarn lombok.NonNull
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# This is generated automatically by the Android Gradle plugin.
-dontwarn com.google.android.gms.auth.api.identity.BeginSignInRequest$Builder
-dontwarn com.google.android.gms.auth.api.identity.BeginSignInRequest$GoogleIdTokenRequestOptions$Builder
-dontwarn com.google.android.gms.auth.api.identity.BeginSignInRequest$GoogleIdTokenRequestOptions
-dontwarn com.google.android.gms.auth.api.identity.BeginSignInRequest
-dontwarn com.google.android.gms.auth.api.identity.BeginSignInResult
-dontwarn com.google.android.gms.auth.api.identity.Identity
-dontwarn com.google.android.gms.auth.api.identity.SignInClient
-dontwarn com.google.android.gms.auth.api.identity.SignInCredential
-dontwarn com.google.android.gms.auth.api.signin.GoogleSignIn
-dontwarn com.google.android.gms.auth.api.signin.GoogleSignInClient

-dontwarn com.google.android.gms.safetynet.SafetyNet
-dontwarn com.google.android.gms.safetynet.SafetyNetApi$RecaptchaTokenResponse
-dontwarn com.google.android.gms.safetynet.SafetyNetClient


-keep class org.forgerock.android.auth.** { *; }
-dontwarn org.forgerock.android.auth.**

