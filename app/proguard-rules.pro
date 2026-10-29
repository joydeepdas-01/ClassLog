# ClassLog Proguard Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-dontwarn java.lang.invoke.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.classlog.app.data.local.entity.** { *; }
-keep class com.classlog.app.data.local.model.** { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# DataStore & Preferences
-keep class com.classlog.app.data.datastore.AppSettings { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# Biometric & Crypto
-dontwarn androidx.biometric.**
-keep class androidx.biometric.** { *; }

# Security Hardening: Strip verbose/debug logs in release builds to protect internal logic and data
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
