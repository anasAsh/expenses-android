# Implementation backlog — Personal Expense & Budget Tracker (v1)

Ordered roughly by dependency. Status reflects the codebase as of the latest implementation pass.

---

## 0. Product decisions (unblock implementation)

- **v1 bank:** Arab Bank — golden SMS in [PRD §14](Budget_Tracker_PRD.md)
- **SMS templates:** English card pattern + Arabic “Click” paths shipped; DB `BankTemplateEntity` can override/extend
- **Cloud backup:** v2 (v1 on-device only); **manual DB export** + **optional daily SQLite copy** to a user-picked folder (`DailyBudgetWorker` + SAF tree URI)
- **Income:** out of scope v1
- **Threshold constants:** frozen per [PRD §12](Budget_Tracker_PRD.md)
- **First month categories:** shipped **starter categories** + **merchant rule seeds** (`DefaultCategorySeeds`, `MerchantRuleSeeds`) when the labeled month is empty at onboarding

---

## 1. Project bootstrap

- Android project: Kotlin, minSdk 26, Compose, Gradle Kotlin DSL, version catalog
- Hilt, Room, Navigation Compose, DataStore, WorkManager deps
- App theme, `BudgetApplication`, bottom-nav shell, onboarding gate
- SMS disclosure strings + onboarding SMS section ([PRD §6](Budget_Tracker_PRD.md))
- Release: ProGuard strips `Log.`* in release; Play Data safety notes: [docs/DATA_SAFETY_PLAY_CONSOLE.md](docs/DATA_SAFETY_PLAY_CONSOLE.md) + [STORE_CHECKLIST.md](STORE_CHECKLIST.md) (host privacy URL before release)

---

## 2. Data layer (Room)

- Entities + DAOs + indices ([PRD §5](Budget_Tracker_PRD.md))
- `TransactionRepository` — SMS ingest, manual line, assign category + rule + back-apply, dedup hash
- `CategoryRepository` — CRUD-by-month, rollover copy, included-target sum
- Derived queries — spend rollups, needs-review count

---

## 3. Domain logic (pure Kotlin / testable)

- Merchant normalization + similarity + dedup window ([DedupMatcher](app/src/main/java/com/anasexpenses/budget/domain/dedup/DedupMatcher.kt))
- `DedupHash` SHA-256 keys for pending/settled linkage (merge logic still minimal)
- `BudgetRollup` signed amounts
- Quiet hours, small-category gate, predictive evaluator (+ unit test)
- `BudgetAlertCoordinator` — thresholds 70/85/100 + predictive + `AlertEvent` dedupe + top 5 categories

---

## 4. SMS ingestion pipeline

- Arab Bank `BankTemplate` seed + `RegexBankSmsParser` + golden tests
- `SmsTransactionReceiver` → `SmsReceiverEntryPoint` + IO + `goAsync()`
- **Sender/body filter:** `[ArabBankSmsFilter](app/src/main/java/com/anasexpenses/budget/sms/ArabBankSmsFilter.kt)`
- **Inbox backfill helper:** `[SmsInboxBackfill](app/src/main/java/com/anasexpenses/budget/sms/SmsInboxBackfill.kt)` (Settings: import recent)
- Alerts after ingest (via shared `refreshAlerts` paths)

---

## 5. UI — Onboarding & settings

- Onboarding: SMS disclosure + permission launcher + skip flag; finishing onboarding ensures **default categories + merchant rules** for the current labeled month
- **First-launch tour:** tab walkthrough after shell (`ui/tour/`), persisted via `firstLaunchTourCompleted`
- v1 Arab Bank only (no bank picker)
- Settings: **manual DB export**, **optional daily backup folder**, inbox backfill, paste-SMS debug, `POST_NOTIFICATIONS` prompt, metrics, privacy URL, bulk category import, budget cycle start day

---

## 6. UI — Budget & months

- Home: categories for selected month + spent vs target + progress
- Month picker + rollover confirmation (copy from previous month) via `[CategoryRepository.rolloverFromPreviousMonth](app/src/main/java/com/anasexpenses/budget/data/CategoryRepository.kt)`

---

## 7. UI — Transactions

- Transaction list (selected budget month)
- Tap row → assign category + optional **remember rule** + optional **back-apply** (from list flow)
- Manual entry FAB (`coffee 3` style)
- **Transaction edit** screen: merchant, amount, date, refund, dismissed, **category** (radio list), save/delete

---

## 8. Alerts & notifications

- Channels (`budget_alerts`, `budget_summary`) + `[BudgetNotificationHelper](app/src/main/java/com/anasexpenses/budget/notifications/BudgetNotificationHelper.kt)`
- Quiet hours respected for pushes
- Threshold + predictive; **InboxStyle digest** when several thresholds in one pass
- Deep link taps → `anasexpenses://app/transactions` (and Home route)

---

## 9. WorkManager & alarms

- `[DailyBudgetWorker](app/src/main/java/com/anasexpenses/budget/work/DailyBudgetWorker.kt)` — 24h periodic `refreshAlerts` + optional **daily SQLite backup** to SAF folder (`anas-budget-daily-backup.db`); retries if backup fails
- **AlarmManager** — rollover **00:05** + summary **09:00** local, `scheduleAll` on boot + after fire (`[BudgetAlarmScheduler](app/src/main/java/com/anasexpenses/budget/alarm/BudgetAlarmScheduler.kt)`)

---

## 10. Backup & metrics

- v1 local only; **SAF export** of Room DB in Settings + **optional scheduled copy** to user-chosen folder (`DatabaseExportHelper`, `UserPreferencesRepository.dailyBackupTreeUri`)
- v2 Google Drive
- On-device counters: SMS vs manual row counts (`[AppMetricsRepository](app/src/main/java/com/anasexpenses/budget/data/metrics/AppMetricsRepository.kt)`, Settings)

---

## 11. QA & polish

- Parser unit tests + `CurrencyToJodConverterTest` + seed tests (`DefaultCategorySeedsTest`, `MerchantRuleSeedsTest`) + predictive / `BudgetCycle` tests
- CI: [.github/workflows/android.yml](.github/workflows/android.yml) (`assembleDebug` + `testDebugUnitTest` on push/PR); optional [.github/workflows/apk-test-build.yml](.github/workflows/apk-test-build.yml) for **debug APK artifact** (manual / `v*` tag)
- Paste-SMS debug in Settings
- Partial `values-ar` (UI); Arabic **Click** SMS paths covered in parser; additional Arabic **card** templates incremental

---

## 12. Store readiness

- See [STORE_CHECKLIST.md](STORE_CHECKLIST.md) and [docs/DATA_SAFETY_PLAY_CONSOLE.md](docs/DATA_SAFETY_PLAY_CONSOLE.md)

