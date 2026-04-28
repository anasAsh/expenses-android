# Implementation backlog — Personal Expense & Budget Tracker (v1)

Ordered roughly by dependency. Check items off as you go.

---

## 0. Product decisions (unblock implementation)

Resolved — see [PRD §12](Budget_Tracker_PRD.md):

- [x] **v1 bank:** Arab Bank — golden SMS in [PRD §14](Budget_Tracker_PRD.md)
- [x] **SMS templates:** English only at launch
- [x] **Cloud backup:** v2 (v1 pure local)
- [x] **Income:** out of scope v1
- [x] **Threshold constants:** frozen per [PRD §12](Budget_Tracker_PRD.md) (confidence ≥0.85, dedup ±5 min / similarity 0.9, small category OR &lt;5% or &lt;30 JOD, predictive ≥110%, top 5 for pushes)

---

## 1. Project bootstrap

- [x] Android project: Kotlin, minSdk 26, Compose, Gradle Kotlin DSL, version catalog (`settings.gradle.kts`, `gradle/libs.versions.toml`)
- [x] Hilt, Room, Navigation Compose, DataStore, WorkManager deps (`app/build.gradle.kts`)
- [x] App theme (`BudgetTheme`), `BudgetApplication` + `@HiltAndroidApp`, bottom-nav shell (`MainActivity`, Home / Transactions placeholders)
- [ ] Release: strip SMS from logs in debug variants only if needed; Play **SMS declaration** + in-app disclosure ([PRD §6](Budget_Tracker_PRD.md))

---

## 2. Data layer (Room)

- [x] Entities + DAOs: `Transaction`, `Category`, `Rule`, `BankTemplate`, `AlertEvent` (`Card` deferred) per [PRD §5](Budget_Tracker_PRD.md)
- [x] Indices: `(category_id, date_epoch_day)`, `(normalized_merchant_token, date_epoch_day)`, category `month`, unique `(category_id, month, threshold_type)` on `AlertEvent`
- [ ] `TransactionRepository` as single write path for inserts/updates (transactions + rule side effects)
- [x] Derived queries: spend rollup query on `TransactionDao`; needs-review count flow

---

## 3. Domain logic (pure Kotlin / testable)

- Merchant normalization → `normalized_merchant` + `normalized_merchant_token`
- String similarity for dedup (≥0.9) + ±5 min window + amount + `card_last4` rules
- `dedup_hash` generation and pending → settled merge strategy
- Rollups: respect `dismissed`, `is_refund`, `excluded_from_spend` ([PRD §4.1](Budget_Tracker_PRD.md))
- Alert eligibility: top **5** by target, small-category filter (<5% OR <30 JOD), quiet hours **22:00–08:00**
- Threshold bands (70 / 85 / 100%) + **AlertEvent** idempotency
- Predictive: `projected = current_spend × days_in_month / day_of_month`, trigger at ≥110% target, once/month/category

---

## 4. SMS ingestion pipeline

- [x] Seed **`BankTemplate`** for **`arab_bank`** (English regex in `BudgetSeed`) — validate parser against [PRD §14](Budget_Tracker_PRD.md) golden SMS in tests next
- `SmsParser`: regex + capture groups → structured result + **confidence** ∈ [0,1]; **≥0.85** → auto, else `needs_review`
- Reject non-JOD SMS per PRD (or document explicit alternative)
- `SmsBroadcastReceiver`: `SMS_RECEIVED`, filter senders, offload to background dispatcher
- Wire: Parser → Normalizer → Deduper → Categorizer (rules) → Repository → optional AlertEngine trigger
- Optional: **60-day backfill** scan (`Telephony.Sms` inbox) with same pipeline

---

## 5. UI — Onboarding & settings

- Permission rationale screen → SMS grant or skip (manual-only path)
- First-month **category** setup (name, target JOD 3dp, `excluded_from_spend`)
- **v1:** Arab Bank only — hide multi-bank picker or default `arab_bank`; English templates only
- Optional backfill opt-in after permission

---

## 6. UI — Budget & months

- List/edit categories for **current `YYYY-MM`**
- Month rollover UX: prefill from previous month, **one-tap accept** or edit ([PRD §4.6](Budget_Tracker_PRD.md))
- Show derived spend vs target per category (respect exclusions)

---

## 7. UI — Transactions

- Transaction list (filters: month, category, needs review)
- Detail/edit: amount, date, merchant, category, `is_refund`, dismiss
- Manual entry + shorthand (`coffee 3`, etc.) ≤5s path
- User assigns category → **upsert Rule** + optional **back-apply** uncategorized same month + token ([PRD §4.3](Budget_Tracker_PRD.md))
- Needs-review queue until resolved; survives month boundary ([PRD §4.9](Budget_Tracker_PRD.md))

---

## 8. Alerts & notifications

- Channels: threshold, predictive, monthly summary, optional needs-review digest ([ARCHITECTURE.md](ARCHITECTURE.md))
- Quiet-hour queue + flush at 08:00
- Combine multi-category threshold pushes when possible ([PRD §4.7](Budget_Tracker_PRD.md))
- Deep links from notifications → relevant screen

---

## 9. WorkManager

- `MonthRolloverWorker` — 1st **00:05** local
- `MonthlySummaryWorker` — 1st **09:00** local (prior month snapshot + push)
- `PredictiveAlertWorker` — daily (e.g. 08:05) after quiet hours
- Threshold evaluation on txn insert/update or periodic coalescing
- Idempotent writes to `AlertEvent` before showing notifications

---

## 10. Backup & metrics (per decisions)

- **v1:** No Google Drive backup ([PRD §12](Budget_Tracker_PRD.md)); optional SAF/file export only if added without Drive
- **v2:** Google Drive AppFolder JSON export (when prioritized)
- On-device **success metrics** counters ([PRD §9](Budget_Tracker_PRD.md)); optional opt-in anonymous upload later

---

## 11. QA & polish

- Golden **SMS corpus** unit tests; CI runs parser tests
- Optional debug “paste SMS” simulator ([ARCHITECTURE.md](ARCHITECTURE.md))
- Arabic SMS templates + RTL UI pass when Arabic templates ship (post–v1 English-only SMS per PRD §12)

---

## 12. Store readiness

- Play Console privacy policy, Data safety form, SMS permission justification text
- Screenshots, store listing (English; Arabic if applicable)

