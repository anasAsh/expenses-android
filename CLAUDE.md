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

**Integration** (`sms/`, `alerts/`, `work/`) — SMS broadcast receiver → parser pipeline → repository. `DailyBudgetWorker` (WorkManager, every 24h) calls `BudgetAlertCoordinator`.

## SMS Ingestion Pipeline

```
SmsTransactionReceiver (BroadcastReceiver)
  → ArabBankSmsFilter (heuristic: sender + body check)
  → RegexBankSmsParser (named groups: card, merchant, amount, date, time)
  → MerchantNormalizer → DedupMatcher (±5 min, amount, card, merchant)
  → TransactionRepository.insert()
  → BudgetAlertCoordinator.refreshAlerts()
```

Bank SMS patterns are stored in `BankTemplateEntity` (user-configurable regexes). The parser supports both Arabic and English SMS bodies.

## Alert System

`BudgetAlertCoordinator` evaluates thresholds (70% notice → 85% approaching → 100% exceeded) plus a predictive month-end projection. Guards: `SmallCategoryGate` (suppresses tiny budgets), `QuietHours` (22:00–08:00 queue, flush at 08:00), `AlertEventEntity` idempotency key `(categoryId, month, thresholdType)`.

## Key Conventions

- Currency: JOD (Jordanian Dinar) via `JodMoney` wrapper — never use raw `Double` for money.
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

Unit tests live in `src/test/` and cover domain logic only (`RegexBankSmsParserTest`, `PredictiveEvaluatorTest`, `MerchantNormalizerTest`). Instrumented tests are in `src/androidTest/`. No lint/ktlint/detekt configured.

## Docs

- `ARCHITECTURE.md` — detailed design decisions for SMS pipeline, Room schema, WorkManager schedule, alerts, and security model.
- `TASKS.md` — implementation backlog ordered by dependency.
- `Budget_Tracker_PRD.md` — full product requirements.
- `STORE_CHECKLIST.md` — Play Store release checklist.
