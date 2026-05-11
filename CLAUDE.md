# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew :app:assembleDebug       # Build debug APK
./gradlew :app:assembleRelease     # Build release APK (R8 minification + resource shrinking)
./gradlew :app:test                # Run unit tests
./gradlew :app:testDebugUnitTest --tests "*.RegexBankSmsParserTest"  # Run single test class
```

Requires JDK 17 and Android SDK. The app requires a real device (SMS receiver) for full integration testing.

## Architecture

MVVM + layered architecture with Hilt DI. Four layers:

**UI** (`ui/`) — Jetpack Compose screens with StateFlow-backed ViewModels. Bottom-nav shell: Home, Transactions, Settings. `RootViewModel` gates on onboarding completion before rendering the shell.

**Domain** (`domain/`) — Pure Kotlin, no framework imports. Contains: `BudgetRollup`, `DedupMatcher`/`DedupHash`, `MerchantNormalizer`/`MerchantSimilarity`, `PredictiveEvaluator`, `QuietHours`, `SmallCategoryGate`, `JodMoney`/`MoneyFormat`. All unit-testable without Android.

**Data** (`data/`) — Room DB (`BudgetDatabase`) with 5 entities: `TransactionEntity`, `CategoryEntity`, `RuleEntity`, `BankTemplateEntity`, `AlertEventEntity`. DataStore for user preferences. Repositories: `TransactionRepository`, `CategoryRepository`, `UserPreferencesRepository`.

**Integration** (`sms/`, `alerts/`, `work/`) — SMS broadcast receiver → `TransactionRepository.ingestSmsBodies` → parser pipeline → Room. `DailyBudgetWorker` (WorkManager, every 24h) calls `BudgetAlertCoordinator.refreshAlerts` and, if the user enabled it in Settings, copies the SQLite file to a SAF-chosen folder (`DatabaseExportHelper.checkpointAndCopyToTreeUri`).

## SMS Ingestion Pipeline

```
SmsTransactionReceiver (BroadcastReceiver; no body pre-filter on this path)
  → TransactionRepository.ingestSmsBodies (normalize body)
  → RegexBankSmsParser (+ DB BankTemplateEntity, BudgetSeed fallbacks; Arabic Click + English card patterns)
  → CurrencyToJodConverter (static FX to milli-JOD for supported codes; unknown currency → skip insert)
  → MerchantNormalizer → DedupMatcher (±5 min, amount, card, merchant)
  → TransactionDao.insert (currency stored as JOD; non-JOD SMS → needs_review)
  → BudgetAlertCoordinator.refreshAlerts()
```

`ArabBankSmsFilter` is used for **inbox backfill** heuristics only, not for filtering live `SMS_RECEIVED`.

Bank SMS patterns are stored in `BankTemplateEntity` (user-configurable regexes). Seeded templates and fallbacks cover English card SMS and Arabic “Click” variants.

## Onboarding & first run

- Onboarding finishes with **default categories** (`DefaultCategorySeeds`) and **merchant rule seeds** (`MerchantRuleSeeds`) for the labeled budget month when the month was empty (`CategoryRepository.ensureDefaultCategoriesForMonth` / `ensureMerchantRuleSeedsForMonth`).
- After the main shell loads, a **first-launch tour** (`ui/tour/FirstLaunchTourDialog.kt`) can run once (`UserPreferencesRepository.firstLaunchTourCompleted`).

## Alert System

`BudgetAlertCoordinator` evaluates thresholds (70% notice → 85% approaching → 100% exceeded) plus a predictive month-end projection. Guards: `SmallCategoryGate` (suppresses tiny budgets), `QuietHours` (22:00–08:00 queue, flush at 08:00), `AlertEventEntity` idempotency key `(categoryId, month, thresholdType)`.

## Key Conventions

- Currency: JOD (Jordanian Dinar) via `JodMoney` / milli-JOD fields — never use raw `Double` for money. SMS amounts in **USD, EUR, GBP, SAR, AED, QAR, KWD, BHD, OMR** (and JOD) are converted offline via **`CurrencyToJodConverter`**; rows are still stored with `currency = "JOD"` but `status` is **`needs_review`** when the parsed SMS currency was not JOD.
- Month handling: `YearMonth` throughout. `UserPreferencesRepository.selectedMonth` persists the current view.
- Dedup: `DedupHash` (SHA-256 of amount+merchant+card+date) for pending/settled linkage; `DedupMatcher` for fuzzy ±5 min window checks.
- Category rules: `RuleEntity` maps `normalized_merchant_token → categoryId`. `TransactionRepository.assignCategoryAndOptionalRule()` can back-apply to all uncategorized transactions in the month.
- Logging: Use `BudgetLog` wrapper (strips calls in release via ProGuard).
- DI qualifiers: `@IoDispatcher` for IO work, `@ApplicationScope` for long-lived coroutines.

## Database

Room v1 schema with `fallbackToDestructiveMigration`. When adding new entities or changing columns, write a proper `Migration` or bump the version with awareness that existing installs will wipe data if no migration is provided.

## Navigation

Routes defined in `ui/navigation/Route.kt` as a sealed class. `TransactionEdit` takes an `id` path param. Navigation uses `saveState = true` + `launchSingleTop = true` for bottom-nav tabs.

## Testing

Unit tests live in `src/test/` (domain + parser): e.g. `RegexBankSmsParserTest`, `PredictiveEvaluatorTest`, `MerchantNormalizerTest`, `CurrencyToJodConverterTest`, `DefaultCategorySeedsTest`, `MerchantRuleSeedsTest`, `BudgetCycleTest`, `CategoryBulkImportTest`. Instrumented tests are in `src/androidTest/`. GitHub Actions: `android.yml` runs assemble + unit tests on push/PR; `apk-test-build.yml` uploads a debug APK on workflow dispatch or `v*` tags. No lint/ktlint/detekt configured.

## Docs

- `ARCHITECTURE.md` — detailed design decisions for SMS pipeline, Room schema, WorkManager schedule, alerts, and security model.
- `TASKS.md` — implementation backlog ordered by dependency.
- `Budget_Tracker_PRD.md` — full product requirements.
- `STORE_CHECKLIST.md` — Play Store release checklist.
