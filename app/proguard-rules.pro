# ProGuard rules for SmartSpend Universal Release

# Room Database and SQLite bindings
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.migration.Migration

# Keep all Room database generated implementations
-keep class * extends androidx.room.RoomOpenHelper$Delegate { *; }
-keep class *_Impl { *; }

# SmartSpend Data Models and Database Entities
-keep class com.eyuel.smartspend.data.local.** { *; }
-keep class com.eyuel.smartspend.data.model.** { *; }
-keep class com.eyuel.smartspend.ui.model.** { *; }

# Keep Enums (TransactionType, CashflowGranularity, BankType, etc.)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin Coroutines & Reflection safety
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
