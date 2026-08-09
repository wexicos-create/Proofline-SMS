# ProGuard configuration for Proofline SMS
# http://proguard.sourceforge.net/index.html#manual/usage.html

# General settings
-dontusemixedcaseclassnames
-verbose
-optimizationpasses 5
-mergeinterfacesaggressively

# ============== PRESERVE ATTRIBUTES ==============
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-renamesourcefileattribute SourceFile

# ============== ANDROID FRAMEWORK ==============
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgent
-keep public class * extends android.preference.Preference
-keep public class * extends android.support.v4.app.Fragment
-keep public class * extends android.app.Fragment
-keep public class com.android.internal.policy.PhoneWindow

# ============== PROOFLINE SMS APPLICATION ==============
-keep class com.proofline.sms.** { *; }
-keepclassmembers class com.proofline.sms.** {
    <methods>;
    <fields>;
}

# ============== BOUNCYCASTLE CRYPTOGRAPHY ==============
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keepnames class org.bouncycastle.crypto.** { *; }
-keepnames class org.bouncycastle.jce.** { *; }
-keepnames class org.bouncycastle.asn1.** { *; }

# ============== ANDROIDX SECURITY ==============
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.**
-keepnames class androidx.security.crypto.** { *; }

# ============== NATIVE METHODS ==============
-keepclassmembers class * {
    native <methods>;
}

# ============== ENUMS ==============
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============== SERIALIZATION ==============
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============== KOTLIN ==============
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# ============== REMOVED WARNINGS ==============
-dontwarn java.lang.invoke.**
-dontwarn javax.naming.**
-dontwarn sun.misc.Unsafe
