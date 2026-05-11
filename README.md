# Anas Expenses

Android budget tracker (spec: [Budget_Tracker_PRD.md](Budget_Tracker_PRD.md), architecture: [ARCHITECTURE.md](ARCHITECTURE.md), contributor/agent notes: [CLAUDE.md](CLAUDE.md)).

## Build

1. Install **JDK 17** and **Android SDK** (Android Studio bundles both).
2. Create `local.properties` with `sdk.dir=/path/to/Android/sdk`.
3. From the repo root: `./gradlew :app:assembleDebug`
4. Open the `app` module in Android Studio to run on a device or emulator.

## CI

- **[android.yml](.github/workflows/android.yml)** — on push/PR to `main` or `master`: `assembleDebug` + `testDebugUnitTest` (no artifact upload).
- **[apk-test-build.yml](.github/workflows/apk-test-build.yml)** — manual **Build debug APK (testing)** workflow or push a `v*` tag; uploads **`app-debug-apk`** (retention ~21 days).

SMS-related features require a device/emulator with runtime SMS permission. If JDK/SDK are missing locally, use the workflows above or Android Studio to verify builds.
