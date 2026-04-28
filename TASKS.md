# Implementation backlog — Personal Expense & Budget Tracker (v1)

Ordered roughly by dependency. Status reflects the codebase as of the latest implementation pass.

---

## 0. Product decisions (unblock implementation)

- **v1 bank:** Arab Bank — golden SMS in [PRD §14](Budget_Tracker_PRD.md)
- **SMS templates:** English only at launch
- **Cloud backup:** v2 (v1 pure local)
- **Income:** out of scope v1
- **Threshold constants:** frozen per [PRD §12](Budget_Tracker_PRD.md)

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

- Onboarding: SMS disclosure + permission launcher + skip flag
- First category capture (name, target JOD milli, excluded toggle) + more categories from Home
- v1 Arab Bank only (no bank picker)
- Settings: export, inbox backfill, paste-SMS debug, `POST_NOTIFICATIONS` prompt, metrics, privacy link placeholder

---

## 6. UI — Budget & months

- Home: categories for selected month + spent vs target + progress
- Month picker + rollover confirmation (copy from previous month) via `[CategoryRepository.rolloverFromPreviousMonth](app/src/main/java/com/anasexpenses/budget/data/CategoryRepository.kt)`

---

## 7. UI — Transactions

- Transaction list (selected budget month)
- Tap → assign category + rule toggles + back-apply
- Manual entry FAB (`coffee 3` style)
- Full edit: amount, date, refund, dismiss, category

---

## 8. Alerts & notifications

- Channels (`budget_alerts`, `budget_summary`) + `[BudgetNotificationHelper](app/src/main/java/com/anasexpenses/budget/notifications/BudgetNotificationHelper.kt)`
- Quiet hours respected for pushes
- Threshold + predictive; **InboxStyle digest** when several thresholds in one pass
- Deep link taps → `anasexpenses://app/transactions` (and Home route)

---

## 9. WorkManager & alarms

- `[DailyBudgetWorker](app/src/main/java/com/anasexpenses/budget/work/DailyBudgetWorker.kt)` — 24h periodic `refreshAlerts`
- **AlarmManager** — rollover **00:05** + summary **09:00** local, `scheduleAll` on boot + after fire (`[BudgetAlarmScheduler](app/src/main/java/com/anasexpenses/budget/alarm/BudgetAlarmScheduler.kt)`)

---

## 10. Backup & metrics

- v1 local only; **SAF export** of Room DB in Settings
- v2 Google Drive
- On-device counters: SMS vs manual row counts (`[AppMetricsRepository](app/src/main/java/com/anasexpenses/budget/data/metrics/AppMetricsRepository.kt)`, Settings)

---

## 11. QA & polish

- Parser unit tests (incl. second English golden) + predictive unit test
- CI: [.github/workflows/android.yml](.github/workflows/android.yml)
- Paste-SMS debug in Settings; expanded English SMS test corpus
- Partial `values-ar` (UI); bank SMS still English v1 per product decision; full Arabic SMS matching is future

---

## 12. Store readiness

- See [STORE_CHECKLIST.md](STORE_CHECKLIST.md) and [docs/DATA_SAFETY_PLAY_CONSOLE.md](docs/DATA_SAFETY_PLAY_CONSOLE.md)

