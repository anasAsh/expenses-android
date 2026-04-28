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
- [ ] Release polish: strip verbose logs in release (beyond `BudgetLog`); full Play Console narrative + Data safety ([STORE_CHECKLIST.md](STORE_CHECKLIST.md))

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

- [x] Arab Bank `BankTemplate` seed + `RegexBankSmsParser` + golden test
- [x] `SmsTransactionReceiver` → `SmsReceiverEntryPoint` + IO + `goAsync()`
- [x] **Sender/body filter:** [`ArabBankSmsFilter`](app/src/main/java/com/anasexpenses/budget/sms/ArabBankSmsFilter.kt)
- [x] **Inbox backfill helper:** [`SmsInboxBackfill`](app/src/main/java/com/anasexpenses/budget/sms/SmsInboxBackfill.kt) (wire from onboarding/settings opt-in UI if desired)
- [x] Alerts after ingest (via shared `refreshAlerts` paths)

---

## 5. UI — Onboarding & settings

- [x] Onboarding: SMS disclosure + permission launcher + skip flag
- [x] First category capture (name, target JOD milli, excluded toggle)
- [x] v1 Arab Bank only (no bank picker)

---

## 6. UI — Budget & months

- [x] Home: categories for current month + spent vs target + progress (included categories only for bar)
- [ ] Dedicated month picker / rollover confirmation UI (repository has [`rolloverFromPreviousMonth`](app/src/main/java/com/anasexpenses/budget/data/CategoryRepository.kt))

---

## 7. UI — Transactions

- [x] Transaction list (current month)
- [x] Tap → assign category + rule toggles + back-apply
- [x] Manual entry FAB (`coffee 3` style)
- [ ] Full edit screen for amount/date/refund/dismiss (repository supports `updateTransaction`; UI still minimal)

---

## 8. Alerts & notifications

- [x] Channels (`budget_alerts`, `budget_summary`) + [`BudgetNotificationHelper`](app/src/main/java/com/anasexpenses/budget/notifications/BudgetNotificationHelper.kt)
- [x] Quiet hours respected for pushes
- [x] Threshold + predictive notifications via coordinator
- [ ] Combined digest notification (optional); deep links from notifications

---

## 9. WorkManager

- [x] [`DailyBudgetWorker`](app/src/main/java/com/anasexpenses/budget/work/DailyBudgetWorker.kt) — 24h periodic `refreshAlerts`
- [ ] Exact-time workers for month rollover **00:05** and summary **09:00** (architecture doc; requires `AlarmManager` or expedited work policies — future)

---

## 10. Backup & metrics

- [ ] v1 local only ([PRD §12](Budget_Tracker_PRD.md)); optional SAF export
- [ ] v2 Google Drive
- [ ] On-device success metrics ([PRD §9](Budget_Tracker_PRD.md))

---

## 11. QA & polish

- [x] Parser unit tests + predictive unit test
- [x] CI: [.github/workflows/android.yml](.github/workflows/android.yml)
- [ ] Expanded golden SMS corpus; paste-SMS debug UI ([ARCHITECTURE.md](ARCHITECTURE.md))
- [ ] Arabic SMS templates + RTL when prioritized

---

## 12. Store readiness

- See [STORE_CHECKLIST.md](STORE_CHECKLIST.md)
