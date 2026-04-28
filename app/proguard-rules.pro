# Keep Hilt / Room generated code (defaults + keep rules from deps usually suffice)
-keepattributes Signature
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn kotlinx.coroutines.**
