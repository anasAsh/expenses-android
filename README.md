# Anas Expenses

Android budget tracker (spec: [Budget_Tracker_PRD.md](Budget_Tracker_PRD.md), [ARCHITECTURE.md](ARCHITECTURE.md)).

## Build

1. Install **JDK 17** and **Android SDK** (Android Studio bundles both).
2. Create `local.properties` with `sdk.dir=/path/to/Android/sdk`.
3. From the repo root: `./gradlew :app:assembleDebug`
4. Open the `app` module in Android Studio to run on a device or emulator.

SMS-related features require a device/emulator with runtime SMS permission.
