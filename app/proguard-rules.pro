# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ───── Kotlin Serialization ─────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep @Serializable classes and their generated serializers
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Serializable data classes' members (needed for reflection-free serialization)
-keepclassmembers class com.ashutosh.mindfultennis.data.remote.model.** {
    *;
}

# ───── Supabase SDK ─────
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ───── Ktor ─────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ───── OkHttp (Ktor engine) ─────
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ───── Room ─────
# Room handles its own keep rules via annotations processor,
# but keep entity classes to be safe
-keep class com.ashutosh.mindfultennis.data.local.db.entity.** { *; }
-keep class com.ashutosh.mindfultennis.data.local.db.dao.** { *; }

# ───── Hilt / Dagger ─────
-dontwarn dagger.hilt.internal.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ───── Enum classes ─────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ───── Parcelable ─────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ───── DataStore ─────
-keep class androidx.datastore.** { *; }

# ───── WorkManager ─────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ───── Kotlin Coroutines ─────
-dontwarn kotlinx.coroutines.**

# ───── Compose ─────
# Prevent R8 from merging Compose classes whose default interface methods
# conflict at runtime (IncompatibleClassChangeError on ModifierLocalProvider)
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }