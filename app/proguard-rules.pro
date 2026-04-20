# ── Debugging — keep line numbers in crash reports ────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Retrofit + OkHttp ─────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Gson — keep all model classes ─────────────────────────────────
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep your Model classes from being obfuscated
# (Gson needs field names to match JSON keys)
-keep class com.idsr_project.Model.** { *; }

# ── Room ──────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ── Firebase ──────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Crashlytics — keep stack traces readable ──────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# ── Glide ─────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# ── Lottie ────────────────────────────────────────────────────────
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ── Parcelize ─────────────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ── WorkManager ───────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── SessionManager / SharedPreferences ───────────────────────────
-keep class com.idsr_project.utils.SessionManager { *; }

# ── Keep Activity and Service names ───────────────────────────────
-keep class com.idsr_project.activities.** { *; }
-keep class com.idsr_project.services.** { *; }