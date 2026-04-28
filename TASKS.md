# Implementation backlog — Personal Expense & Budget Tracker (v1)

Ordered roughly by dependency. Status reflects the codebase as of the latest implementation pass.

---

## 0. Product decisions (unblock implementation)

- [x] **v1 bank:** Arab Bank — golden SMS in [PRD §14](Budget_Tracker_PRD.md)
- [x] **SMS templates:** English only at launch
- [x] **Cloud backup:** v2 (v1 pure local)
- [x] **Income:** out of scope v1
- [x] **Threshold constants:** frozen per [PRD §12](Budget_Tracker_PRD.md)

---

## 1. Project bootstrap

- [x] Android project: Kotlin, minSdk 26, Compose, Gradle Kotlin DSL, version catalog
- [x] Hilt, Room, Navigation Compose, DataStore, WorkManager deps
- [x] App theme, `BudgetApplication`, bottom-nav shell, onboarding gate
- [x] SMS disclosure strings + onboarding SMS section ([PRD §6](Budget_Tracker_PRD.md))
- [x] Release: ProGuard strips `Log.*` in release; Play Data safety notes: [docs/DATA_SAFETY_PLAY_CONSOLE.md](docs/DATA_SAFETY_PLAY_CONSOLE.md) + [STORE_CHECKLIST.md](STORE_CHECKLIST.md) (host privacy URL before release)

---

## 2. Data layer (Room)

- [x] Entities + DAOs + indices ([PRD §5](Budget_Tracker_PRD.md))
- [x] `TransactionRepository` — SMS ingest, manual line, assign category + rule + back-apply, dedup hash
- [x] `CategoryRepository` — CRUD-by-month, rollover copy, included-target sum
- [x] Derived queries — spend rollups, needs-review count

---

## 3. Domain logic (pure Kotlin / testable)

- [x] Merchant normalization + similarity + dedup window ([DedupMatcher](app/src/main/java/com/anasexpenses/budget/domain/dedup/DedupMatcher.kt))
- [x] `DedupHash` SHA-256 keys for pending/settled linkage (merge logic still minimal)
- [x] `BudgetRollup` signed amounts
- [x] Quiet hours, small-category gate, predictive evaluator (+ unit test)
- [x] `BudgetAlertCoordinator` — thresholds 70/85/100 + predictive + `AlertEvent` dedupe + top 5 categories

---

## 4. SMS ingestion pipeline

- [x] Arab Bank `BankTemplate` seed + `RegexBankSmsParser` + golden tests
- [x] `SmsTransactionReceiver` → `SmsReceiverEntryPoint` + IO + `goAsync()`
- [x] **Sender/body filter:** [`ArabBankSmsFilter`](app/src/main/java/com/anasexpenses/budget/sms/ArabBankSmsFilter.kt)
- [x] **Inbox backfill helper:** [`SmsInboxBackfill`](app/src/main/java/com/anasexpenses/budget/sms/SmsInboxBackfill.kt) (Settings: import recent)
- [x] Alerts after ingest (via shared `refreshAlerts` paths)

---

## 5. UI — Onboarding & settings

- [x] Onboarding: SMS disclosure + permission launcher + skip flag
- [x] First category capture (name, target JOD milli, excluded toggle) + more categories from Home
- [x] v1 Arab Bank only (no bank picker)
- [x] Settings: export, inbox backfill, paste-SMS debug, `POST_NOTIFICATIONS` prompt, metrics, privacy link placeholder

---

## 6. UI — Budget & months

- [x] Home: categories for selected month + spent vs target + progress
- [x] Month picker + rollover confirmation (copy from previous month) via [`CategoryRepository.rolloverFromPreviousMonth`](app/src/main/java/com/anasexpenses/budget/data/CategoryRepository.kt)

---

## 7. UI — Transactions

- [x] Transaction list (selected budget month)
- [x] Tap → assign category + rule toggles + back-apply
- [x] Manual entry FAB (`coffee 3` style)
- [x] Full edit: amount, date, refund, dismiss, category

---

## 8. Alerts & notifications

- [x] Channels (`budget_alerts`, `budget_summary`) + [`BudgetNotificationHelper`](app/src/main/java/com/anasexpenses/budget/notifications/BudgetNotificationHelper.kt)
- [x] Quiet hours respected for pushes
- [x] Threshold + predictive; **InboxStyle digest** when several thresholds in one pass
- [x] Deep link taps → `anasexpenses://app/transactions` (and Home route)

---

## 9. WorkManager & alarms

- [x] [`DailyBudgetWorker`](app/src/main/java/com/anasexpenses/budget/work/DailyBudgetWorker.kt) — 24h periodic `refreshAlerts`
- [x] **AlarmManager** — rollover **00:05** + summary **09:00** local, `scheduleAll` on boot + after fire ([`BudgetAlarmScheduler`](app/src/main/java/com/anasexpenses/budget/alarm/BudgetAlarmScheduler.kt))

---

## 10. Backup & metrics

- [x] v1 local only; **SAF export** of Room DB in Settings
- [ ] v2 Google Drive
- [x] On-device counters: SMS vs manual row counts ([`AppMetricsRepository`](app/src/main/java/com/anasexpenses/budget/data/metrics/AppMetricsRepository.kt), Settings)

---

## 11. QA & polish

- [x] Parser unit tests (incl. second English golden) + predictive unit test
- [x] CI: [.github/workflows/android.yml](.github/workflows/android.yml)
- [x] Paste-SMS debug in Settings; expanded English SMS test corpus
- [x] Partial `values-ar` (UI); bank SMS still English v1 per product decision; full Arabic SMS matching is future

---

## 12. Store readiness

- See [STORE_CHECKLIST.md](STORE_CHECKLIST.md) and [docs/DATA_SAFETY_PLAY_CONSOLE.md](docs/DATA_SAFETY_PLAY_CONSOLE.md)
