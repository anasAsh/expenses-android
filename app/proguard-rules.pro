# Keep Hilt / Room generated code (defaults + keep rules from deps usually suffice)
# Strip android.util.Log in release (verbose/debug logging only).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
-keepattributes Signature
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn kotlinx.coroutines.**
