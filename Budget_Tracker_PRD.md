# Personal Expense & Budget Tracker — PRD

## 1. Overview

A low-effort, automation-first personal expense tracking system designed to minimize user interaction.

The system passively captures transactions (primarily via SMS), categorizes them automatically, and surfaces only meaningful insights such as overspending risks and monthly summaries.

The user is not expected to open the app daily or manage detailed financial records.

**v1 platform:** Android only. SMS-first ingestion requires SMS access that is not available to third-party apps on iOS in the background; see §6.

**Implementation note:** Feature coverage that matches the **current Android codebase** is summarized in the **[Appendix — Shipped behavior](#appendix--shipped-behavior-code-aligned)**. Elsewhere, this PRD states **intent**; where intent and implementation diverge, treat the appendix as authoritative for what users see today.


| Quick nav          |                                                                                                          |
| ------------------ | -------------------------------------------------------------------------------------------------------- |
| Core product       | [§4 Core Features](#4-core-features) · [§8 MVP Scope](#8-mvp-scope)                                      |
| Data & tech        | [§5 Data Model](#5-data-model) · [§6 Technical Requirements](#6-technical-requirements)                  |
| Decisions          | [§12 Product decisions](#12-product-decisions-v1) · [§13 Internationalization](#13-internationalization) |
| **Shipped (code)** | **[Appendix — Shipped behavior](#appendix--shipped-behavior-code-aligned)**                              |
| SMS QA sample      | [§14 Reference SMS](#14-reference-arab-bank-v1-sms-sample)                                               |


---

## 2. Product Principles

- Default to **automation over manual input**
- Optimize for **<10 seconds per interaction**
- Require **no daily app usage**
- Focus on **signal (insights), not bookkeeping**
- Be **accurate enough**, not perfect
- Let users define their own budgeting structure
- Communicate only when **action is needed**

---

## 3. User Persona

An individual managing personal and household finances who:

- Has flexible and evolving spending categories
- Prefers defining budgets in their own way
- Does not want to maintain category structures
- Wants passive monitoring with proactive alerts
- Values simplicity over precision

---

## 4. Core Features

---

### 4.0 Onboarding

#### Flow

1. **Permission rationale** — Explain why SMS access is needed (read bank SMS only; no upload in v1 unless user opts into backup — see §6).
2. **Grant SMS permission** — Required for primary ingestion; user can skip and rely on manual entry only (degraded experience).
3. **Optional historical backfill** — Opt-in: process SMS from device inbox for the **last 60 days** using the same parser and dedup rules as live SMS.
4. **First month setup** — **Shipped:** app seeds **starter categories + merchant rules** when the labeled month is empty; user adjusts targets/names on Home. Targets still refer to the **current labeled budget month** (from today + [budget cycle](#appendix--shipped-behavior-code-aligned) start day; calendar month when start day = 1).

#### Success criteria

User reaches a state where at least one category exists and the app can ingest SMS (if permission granted).

---

### 4.1 User-Defined Budget Categories

#### Functionality

- Users define categories at the start of each month (or on first launch per §4.0).
- Each category is scoped to a **calendar month** (`YYYY-MM`).
- Each category includes:
  - **Name** (free text)
  - **Monthly target amount** (JOD, 3 decimal places)
  - `**excluded_from_spend`** (boolean, default `false`) — When `true`, transactions assigned to this category do **not** count toward “total spent” vs budget rollups or threshold alerts for that category’s spend (use for internal transfers, loan principal moves, etc.). User decides per category.

#### Example

- Groceries — 400 JOD — included in spend  
- Transfers — 650 JOD — user may set `excluded_from_spend: true` if these are not discretionary spend  
- Eating Out — 200 JOD — included in spend

#### Requirements

- No **mandatory** vendor-defined category taxonomy — users edit or delete starter rows freely (**shipped:** optional **starter categories + merchant rules** seed the first empty month at onboarding; see Appendix).
- No category hierarchy (flat structure only)
- Categories are:
  - Lightweight
  - Editable anytime within the same month
  - **Keyed by labeled budget month** `YYYY-MM` (see [Appendix — Budget cycle](#appendix--shipped-behavior-code-aligned)); spending totals for that label use the **budget-cycle date range**, not necessarily calendar `1st–last` when a custom cycle start day is set (§Appendix).
  - **Recreated or copied each month** — On rollover (§4.6), previous month’s categories **prefill** the new month; **shipped:** automatic copy when switching to an empty month in the Home picker and via scheduled rollover hook — **no confirmation dialog** (see Appendix).

#### Spending totals

- `**current_spend`** is **derived**, not stored: sum of non-dismissed, non-refund-adjusted logic per §5 (refunds subtract; dismissed excluded).

#### Design Principle

> “Categories are user-defined and disposable, not system-managed.”

---

### 4.2 Transaction Ingestion (SMS-first)

#### Objective

Automatically capture most transactions with minimal user effort.

---

#### 4.2.1 SMS Parsing (Primary Method)

##### Functionality

- Read incoming bank SMS messages (Android `SMS_RECEIVED`).
- Extract:
  - Amount
  - Currency (v1: see currency policy below)
  - Merchant
  - Date
  - Time
  - Card last 4 (optional)

##### Currency policy (v1)

- **Budget math and alerts** use **milli-JOD** only. **Shipped SMS behavior:** parsed amounts in **JOD, USD, EUR, GBP, SAR, AED, QAR, KWD, BHD, OMR** are converted to milli-JOD via **static offline rates** (`CurrencyToJodConverter`); the row is stored with `currency` shown as JOD in the data layer for v1. If the SMS currency was **not JOD**, the transaction is stored with **`needs_review`** so the user can confirm the implied FX. **Unknown** currency codes → **no insert** (same as a failed parse from a budgeting perspective).

##### Example SMS

“A Trx using Card XXXX4259 from Talabat for JOD 12.410 on 04-Apr-2026 at 13:52 GMT+3.”

##### Parsed Output

- Amount: 12.410 JOD  
- Merchant: Talabat  
- Date: 2026-04-04  
- Time: 13:52  
- Card last 4: 4259  
- Source: SMS  
- `raw_sms`: full body (stored for audit and re-parse)

##### Parsing strategy

- Regex-based extraction per `**BankTemplate`** (bank + language).
- Merchant normalization (e.g. TALABAT.COM → `talabat` → `**normalized_merchant_token`** for rules).

##### Confidence scoring

- Numeric `**confidence` ∈ [0, 1]`** computed from template match quality (all required capture groups present, checksums if any, etc.).
- `**confidence ≥ 0.85`** → transaction `status: auto` (accepted).
- `**confidence < 0.85`** → `status: needs_review` (shown in UI; no auto-delete at month boundary — carries forward until user resolves or dismisses per §4.9).

##### Pending vs settled

- Some banks send authorization SMS then settlement. **Rule:** Prefer **settled** amount when both exist for the “same” transaction. **Dedup hash** (see §4.2.2) links pending → settled; **replace** pending row with settled data when settlement arrives, or merge per implementation with single logical transaction.

##### Refunds / reversals

- Parser or user marks `**is_refund: true`**. Refund subtracts from spend for the assigned `**(category_id, month)`** (same rules as positive amounts, inverted sign in rollup).

---

#### 4.2.2 De-duplication

- **Duplicate** if all hold:
  - Same `**card_last4`** (or both null → use merchant-only path with stricter window)
  - Same **amount** to 3 decimal places
  - `**normalized_merchant`** string similarity **≥ 0.9** (e.g. normalized Levenshtein or token Jaccard — implementation detail)
  - Timestamps within **±5 minutes** of each other
- **Prevent** duplicate entries: second insert skipped or merged into first with updated fields (settlement wins over pending).

---

#### 4.2.3 Silent Processing

- No notifications per transaction
- Background processing only

---

### 4.3 Auto-Categorization

#### v1 (Rule-based)

- Rules keyed on `**normalized_merchant_token`** (single canonical string per merchant after normalization).
- Example seed mappings (optional app seeds, user-overridable):
  - `carrefour` → Groceries  
  - `talabat` → Eating Out

#### Behavior

- If no rule matches: `**category_id` null** = **Uncategorized** (included in spend totals only after user assigns category or rule applies retroactively).
- **Shipped — ingest-time rules:** When an SMS transaction is inserted, the app looks up `Rule` by `normalized_merchant_token` **before** persist; if found, the transaction is stored **with that category** and `status: auto` (same merchant token key as manual correction path).
- **User correction** (user sets category on a transaction):
  - **Upsert** `Rule` for that `merchant_token` → `category_id`, `source: user_correction`
  - **Back-apply** (optional product default: **on**) to all **Uncategorized** transactions in the **current budget month window** (calendar month **or** budget-cycle range — **shipped:** epoch-day range from `**BudgetCycle`** for the labeled month) with the same `normalized_merchant_token`

---

### 4.4 Manual Entry (Fallback)

#### Functionality

Fast entry for:

- Cash transactions
- Missing SMS transactions

#### UX requirements

- ≤ 5 seconds per entry
- Support shorthand:
  - `coffee 3`
  - `taxi 7`
  - `rent 500`

#### Defaults

- Current date
- Currency: JOD
- Suggested category via rule lookup on parsed merchant keyword if user types a merchant name

#### Status

- Manual entries: `source: Manual`, `status: manual` (no SMS confidence).

---

### 4.5 Cash Handling

#### Approach

- User logs only meaningful cash transactions  
**OR**  
- User inputs monthly estimate (optional future; v1: manual line items only unless PRD extended)

---

### 4.6 Monthly Auto-Rollover

- **Triggers (shipped):**
  - **Scheduled:** Alarm-driven `**rolloverFromPreviousMonth`** for the **current labeled budget month** (see Appendix — not only raw `YearMonth.now()` when budget cycle is configured).
  - **On demand:** When the user selects a **budget month** in Home that has **no categories**, the app **automatically copies** from the **prior calendar month’s** category rows (`targetMonth.minusMonths(1)` in code) **without a confirmation dialog**. If the target month already has categories, copy is skipped.
- **Behavior:**
  - Spending totals for prior periods remain in the database (reporting sense: labeled month + cycle window).
  - **Prefill** copies names, targets, `excluded_from_spend` from the previous month key.
  - Uncategorized / needs_review items: **not auto-deleted**; they remain until resolved (§4.9).

---

### 4.7 Budget Alerts & Monitoring

#### Objective

Provide proactive insights without requiring app usage.

---

#### 4.7.1 Threshold alerts

Trigger at **spent / target** for a category (only categories **included in spend**, i.e. `excluded_from_spend == false`):

- **70%** → awareness  
- **85%** → warning  
- **100%+** → overspend

##### Rules

- **Once per threshold per category per month** — tracked via `**AlertEvent`** (see §5).
- **Eligible categories for notification:** Top **N = 5** categories by **monthly_target** (descending). Others still show in-app but no push unless user opts into “all categories” (future).
- **Ignore “very small” categories** for push if **either**: target **< 5%** of total monthly budget (sum of all category targets that month), **or** target **< 30 JOD** (no push for that category; in-app still shows status).
- **Combine alerts** when possible (single notification summarizing multiple categories crossing thresholds in same evaluation window).
- **Quiet hours:** No push between **22:00–08:00** local. **Shipped:** evaluations during quiet hours are **skipped** for that pass (no separate in-memory queue + flush at 08:00); a later `**refreshAlerts`** (new transaction, **DailyBudgetWorker**, **~09:00 daily summary alarm**, etc.) re-evaluates. **Not** a literal queued notification backlog.

---

#### 4.7.2 Predictive alerts

##### Formula

Let `day_index` = 1-based **day within the current budget cycle window**, `D` = **number of days in that window**, `current_spend` = category spend in-window to date, `target` = monthly target. When the budget cycle start day is **1**, this matches calendar month-day / `D` = month length.

**Projected period-end spend:** `projected = current_spend × (D / day_index)` (same formula as `PredictiveEvaluator`; **shipped** uses cycle-aware `day_index` / `D` when budget cycle ≠ calendar month).

##### Trigger

- When `**projected ≥ 1.10 × target`** (110% of target) and category is eligible (same N / small-category rules as §4.7.1).
- **At most one predictive alert per category per month** (record in `AlertEvent` with `threshold: predictive`).

##### Example copy

“At this pace, you’ll exceed Eating Out budget before month end.”

---

#### Notification principle

> “Notify only when user action is valuable.”

---

### 4.8 Monthly Summary Report

#### Delivery

- Push notification on **1st of month at 09:00 local** (primary). Optional: also allow **last day of month 21:00** as setting (future).

#### Content

**Snapshot:**

- Total spent vs total budget (only categories with `excluded_from_spend == false` unless user toggles “show all” in app)
- % over/under

**Insights:**

- Highest overspend category (largest positive delta vs target)
- Highest “saving” category (largest underspend vs target)

**Example**
“You spent 2,300 JOD (+10%)  
Overspent: Lifestyle (+180 JOD)  
Under budget: Subscriptions (-15 JOD)”

#### Optional (future)

- Compare vs last month

---

### 4.9 Editability & Lifecycle

- **Any transaction field** editable by user (amount, date, merchant, category, refund flag).
- **Dismissed** transactions: `status: dismissed` — **retained for audit**, **excluded** from spend totals and alerts.
- **Needs review** persists across month boundaries until user fixes, assigns category, or dismisses.

---

## 5. Data Model

### Transaction


| Field                       | Description                                    |
| --------------------------- | ---------------------------------------------- |
| `id`                        | UUID or monotonic                              |
| `amount`                    | Decimal (3 dp JOD)                             |
| `currency`                  | Shipped: stored as **`JOD`** for accepted rows; SMS may have been parsed in another supported code (see §4.2.1) |
| `merchant`                  | Raw display string                             |
| `normalized_merchant`       | Normalized for dedup / display                 |
| `normalized_merchant_token` | Canonical key for `Rule` lookup                |
| `category_id`               | Nullable → Uncategorized                       |
| `date`                      | Transaction date                               |
| `time`                      | Time of day                                    |
| `source`                    | `SMS`                                          |
| `confidence`                | Float [0, 1]                                   |
| `status`                    | `auto`                                         |
| `is_refund`                 | Boolean                                        |
| `raw_sms`                   | Nullable; full SMS body for SMS-sourced rows   |
| `card_last4`                | Nullable                                       |
| `dedup_hash`                | Nullable; stable key for pending/settled merge |
| `bank_template_id`          | Nullable FK to `BankTemplate`                  |
| `created_at`                | Timestamp                                      |
| `updated_at`                | Timestamp                                      |


---

### Category


| Field                 | Description       |
| --------------------- | ----------------- |
| `id`                  |                   |
| `month`               | `YYYY-MM` — scope |
| `name`                | Free text         |
| `monthly_target`      | JOD, 3 dp         |
| `excluded_from_spend` | Boolean           |
| `created_at`          |                   |


**Note:** `current_spend` is **derived** in queries: sum of applicable `Transaction` amounts for `(category_id, month)`.

---

### Rule


| Field            | Description                         |
| ---------------- | ----------------------------------- |
| `id`             |                                     |
| `merchant_token` | Matches `normalized_merchant_token` |
| `category_id`    | FK                                  |
| `source`         | `user_correction`                   |
| `created_at`     |                                     |


---

### BankTemplate


| Field      | Description                      |
| ---------- | -------------------------------- |
| `id`       |                                  |
| `bank_id`  | Identifier (e.g. issuer code)    |
| `language` | e.g. `en`, `ar`                  |
| `regex`    | Pattern + capture group contract |
| `version`  | Integer for migrations           |


---

### AlertEvent


| Field         | Description |
| ------------- | ----------- |
| `id`          |             |
| `category_id` |             |
| `month`       | `YYYY-MM`   |
| `threshold`   | `70`        |
| `sent_at`     | Timestamp   |


Enforces **one notification per (category, month, threshold)** for threshold types.

---

### Card (optional UX)


| Field        | Description           |
| ------------ | --------------------- |
| `card_last4` | Primary key or unique |
| `nickname`   | Optional user label   |


---

## 6. Technical Requirements

- **Platform:** **Android only (v1).** Min SDK aligned with architecture doc (e.g. 26+). **Jetpack Compose** UI recommended.
- **Currency:** JOD, **3 decimal places** throughout storage and UI.
- **Permissions:** `READ_SMS` / receive SMS as required by target SDK; runtime permission flow on onboarding.
- **Google Play:** SMS and Call Log permissions are **restricted**. The app must comply with [Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/9047303): declare narrow use (financial transaction parsing), in-app disclosure, and **no misuse** as default SMS handler unless product explicitly becomes the default SMS app (not required for `SMS_RECEIVED` listener pattern — follow current Play policy for your integration choice).
- **Privacy / parsing:** **Local-only** parsing and storage for v1; no server upload unless user opts into **anonymous metrics** (§9). **Cloud backup** (e.g. Google Drive AppFolder) is **out of scope until v2** (§12).
- **Storage:** SQLite via **Room**; optional **SQLCipher** or file encryption with Keystore-wrapped keys for sensitive fields.
- **Preferences:** **DataStore** for user flags (onboarding, selected budget month, budget cycle start day, SMS skip, optional daily backup folder URI, first-launch tour, …); separate metrics store (Appendix).
- **Notifications:** Runtime `**POST_NOTIFICATIONS`** where required by API level; guarded notification post path in shipped app.
- **Background:** **WorkManager** (`DailyBudgetWorker`); **AlarmManager** for scheduled rollover/summary (exact alarms per device policy).

## 7. Non-Goals (v1)

- **iOS** / cross-platform parity
- **Income** tracking and net cashflow (see roadmap if added later)
- **Multi-currency** and FX
- Full accounting system
- Detailed cash envelope accounting
- Category hierarchies
- Daily engagement features / gamification
- Complex dashboards

---

## 8. MVP Scope

- Android app (**Compose**, **MVVM**, **Hilt**, **Room**, **DataStore**, **WorkManager** — see Appendix)
- Bottom navigation: **Home**, **Transactions**, **Settings**; **transaction edit**; **tap category → filtered transactions** route
- SMS parsing (**Arab Bank** v1 — English template seed + golden corpus §14); regex + `BankTemplate` pipeline; live SMS receiver path + dedup
- User-defined monthly categories (labeled `YYYY-MM`) with **automatic** rollover copy when selecting empty month + scheduled rollover
- **Configurable budget month start day (1–28)** and `**BudgetCycle`**-aware ranges for transactions, alerts, workers/receivers (Appendix)
- Rule-based categorization + **Rules applied on SMS insert** + user correction → rules + optional back-apply
- Manual entry + shorthand
- Threshold + predictive budget alerts (with `AlertEvent` deduplication; **top-5** targets; **small-category gate**; **quiet-hours skip**; safe `**notify`** path)
- **DailyBudgetWorker** + alarm receivers; **daily summary** notification (~09:00) + `**refreshAlerts`** hook
- **Settings:** DB **export**, SMS **backfill** (~60 days), paste-SMS debug, privacy link, **local metrics**, **bulk category import** (`Name: amount`)
- Onboarding + optional 60-day backfill (Settings action); **first-launch tab tour** after shell; **optional daily local DB backup** to user-picked folder

---

## 9. Success Metrics


| Metric                       | Target      | Measurement (v1)                                                    |
| ---------------------------- | ----------- | ------------------------------------------------------------------- |
| Transactions manually edited | < 5%        | On-device counter: `edits / total_transactions` per rolling 30 days |
| App opens                    | ≤ 2× / week | On-device: session count / week                                     |
| Alert engagement             | > 60%       | Tap or dismiss on notification within 48h / alerts sent             |
| Monthly retention            | > 80%       | Return in same calendar month+1 (local only)                        |


**Optional:** Opt-in **anonymous** telemetry batch (no raw SMS) if a backend is introduced later; v1 can use **local-only** aggregates shown in a debug or “privacy” screen for dogfooding.

---

## 10. Future Roadmap

- Bank API integration (replace SMS)
- ML-based categorization
- Spending trends visualization
- Export **CSV/XLSX** (database **file export** exists in Settings — see Appendix)
- Multi-currency support
- Recurring transaction detection
- Category reuse suggestions (smarter than copy)
- Shared household mode
- iOS companion with manual / share-based ingestion (no background SMS)
- **Income** field and “spent vs income” snapshot
- **Cloud backup** (e.g. Google Drive AppFolder JSON export)

---

## 11. Key Design Principle

> “The system runs in the background and only surfaces when it has something important to say.”

---

## 12. Product decisions (v1)

### Resolved


| Topic                      | Decision                                                                                                                                                 |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Bank(s) in v1**          | **Arab Bank** only — drives `BankTemplate` seeds and QA golden SMS corpus (§14).                                                                         |
| **SMS template languages** | **English** `BankTemplate` **seed** shipped for Arab Bank; parser supports Arabic/English bodies per regex. Additional seeded templates are incremental. |
| **Cloud backup**           | **Google Drive AppFolder / cloud sync not in v1.** **SQLite file export to user-chosen location** (SAF) **shipped** in Settings — see Appendix.          |
| **Income**                 | **Out of scope for v1** — no “monthly income” field; variance vs income is roadmap ([§10](Budget_Tracker_PRD.md)).                                       |


### Frozen for v1 (confirmed)

The following constants are **locked for v1** implementation and QA:


| Constant                            | Value                                                                           |
| ----------------------------------- | ------------------------------------------------------------------------------- |
| SMS parse auto-accept               | `confidence ≥ 0.85`; else `needs_review`                                        |
| Dedup time window                   | **±5 minutes**                                                                  |
| Dedup merchant match                | Similarity **≥ 0.9** (with amount + card rules per §4.2.2)                      |
| Push suppression (“small” category) | **OR** gate: target **< 5%** of total monthly budget **or** target **< 30 JOD** |
| Predictive alert trigger            | **Projected ≥ 110%** of monthly target ([§4.7.2](Budget_Tracker_PRD.md))        |
| Push eligibility scope              | **Top 5** categories by `monthly_target` ([§4.7.1](Budget_Tracker_PRD.md))      |


---

## 13. Internationalization

### Bank SMS messages (templates)

- **Shipped:** English `**BankTemplate`** seed for **Arab Bank** (§12, §14).
- Parser regex pipeline can match **Arabic or English** SMS bodies; additional **seeded** Arabic templates are incremental / roadmap.

### App UI

- **English** (`values/`) primary strings; **Arabic** (`values-ar/`) provided for key screens where translated — independently of SMS template language.

---

## Appendix — Shipped behavior (code-aligned)

> Source of truth: `CLAUDE.md`, `ARCHITECTURE.md`, and Kotlin under `app/src/main/java/com/anasexpenses/budget/`.

### Architecture & shell


| Area        | Shipped                                                                                                                                                                                           |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Pattern     | **MVVM**, **Hilt** dependency injection                                                                                                                                                           |
| UI          | **Jetpack Compose**                                                                                                                                                                               |
| Local DB    | **Room** (`BudgetDatabase`): `TransactionEntity`, `CategoryEntity`, `RuleEntity`, `BankTemplateEntity`, `AlertEventEntity`                                                                        |
| Preferences | **DataStore** (`UserPreferencesRepository`): selected budget month (`YearMonth` string), budget cycle start day (1–28), onboarding flags, optional **daily backup** SAF tree URI, **first-launch tour** completed flag, … |
| Metrics     | Separate **DataStore** (`AppMetricsRepository`) for on-device counters (e.g. SMS vs manual rows) surfaced in Settings                                                                             |
| Background  | **WorkManager** (`DailyBudgetWorker`); **AlarmManager** receivers for rollover / daily summary scheduling                                                                                         |
| Navigation  | Bottom tabs **Home**, **Transactions**, **Settings**; routes include `transactionEdit/{id}`, `**transactions/category/{categoryId}`**; `**RootViewModel`** gates **onboarding** before main shell; **first-launch tour** overlay until completed |


### Home

- Month **picker** updates persisted selected `**YearMonth`** (labeled budget month).
- **Category cards**: spent vs target, progress, excluded-from-spend badge.
- **Share** summary via Android share sheet (`Intent.ACTION_SEND`).
- **Tap category card** → `**transactions/category/{categoryId}`**: transaction list filtered to that category for the selected month/cycle; TopAppBar back uses `**navigateUp`** when filtered.
- **Rollover:** Selecting a month with **zero** categories triggers `**rolloverFromPreviousMonth`** **without** dialog.

### Budget cycle (`BudgetCycle`)

- User setting: **start day** ∈ **[1, 28]** (Settings).
- **Labeled `YearMonth`** still keys categories in Room (`month = "YYYY-MM"`).
- **Transaction date ranges**, threshold spend sums, predictive **day index / length**, `**refreshAlerts(month)`** arguments from txn dates, **workers/receivers** use `**labeledYearMonthForDate`** / `**epochDayRangeInclusive`** — not always calendar month boundaries when start day ≠ 1.

### Transactions

- List + filters honor **budget cycle** epoch-day range for the selected labeled month.
- **Manual** entry FAB + shorthand parser.
- **Assign category** (from list): optional **remember rule**, optional **back-apply** (same token, uncategorized, within cycle window).
- **Transaction edit** screen: merchant, amount (JOD), date, refund, dismissed, **category** (per-month list), save/delete.

### Merchant rules

- `**RuleEntity`**: unique `merchant_token` → `category_id`.
- **SMS insert:** `**ruleDao.getByMerchantToken`** before insert → pre-fill `**category_id`** when rule exists (**AUTO** path when categorized by rule).
- **Manual assign:** upsert rule + optional back-apply as above.

### Settings

- **Export** SQLite backup via Storage Access Framework (create document).
- **Optional daily backup:** user picks a **folder** (SAF tree URI); `DailyBudgetWorker` writes `anas-budget-daily-backup.db` on each run when enabled.
- **Import recent SMS** (~60 days) from inbox through same ingest pipeline.
- **Paste SMS** debug ingest.
- **Privacy policy** opens browser URL from strings.
- **Metrics** (local counters).
- **Import categories** bulk text (`Name: amount` lines; **JodMoney** parsing; skips duplicate names case-insensitively for current month).
- **Budget month starts on [day 1–28]** — updates preference and **snaps selected month** to current labeled month when changed.

### Alerts & notifications


| Topic       | Shipped                                                                                                 |
| ----------- | ------------------------------------------------------------------------------------------------------- |
| Thresholds  | ~**70% / 85% / 100%** tier evaluation (`BudgetAlertCoordinator`)                                        |
| Predictive  | ≥ **110%** projected vs target; `**PredictiveEvaluator`**                                               |
| Eligibility | **Top 5** categories by target (included); `**SmallCategoryGate`**                                      |
| Dedup       | `**AlertEvent`** per `(category_id, month, threshold_type)`                                             |
| Quiet hours | **22:00–08:00** — **skip** this evaluation (`QuietHours`); **no** separate queued flush in-app          |
| Posting     | `**notifySafe`** checks notifications enabled; catches `**SecurityException`** (**POST_NOTIFICATIONS**) |
| Periodic    | `**DailyBudgetWorker`** calls `**refreshAlerts`** for **labeled** current month                         |
| Daily alarm | **Daily summary** notification text + `**refreshAlerts`** for labeled month + reschedule alarms         |


### SMS pipeline (Arab Bank)

- `**SmsTransactionReceiver`** → `**TransactionRepository.ingestSmsBodies**` (no body pre-filter on receiver) → `**RegexBankSmsParser**` + `**BankTemplateEntity**` + shipped fallbacks (`**BudgetSeed**`, Arabic Click paths). `**ArabBankSmsFilter**` is for **inbox backfill** heuristics only.
- Supported non-JOD currencies: converted to milli-JOD with static rates; **`needs_review`** when parsed currency ≠ JOD; unknown currency → skip insert.
- **Dedup:** `**DedupMatcher`** / `**DedupHash`** per `**PrdConstants**` (confidence threshold, ±5 min window, similarity).

### Onboarding & first run

- Completing onboarding runs `**ensureDefaultCategoriesForMonth**` + `**ensureMerchantRuleSeedsForMonth**` when the labeled month has no categories yet (`**DefaultCategorySeeds**`, `**MerchantRuleSeeds**`).
- **First-launch tour:** multi-step overlay across Home / Transactions / Settings (`**firstLaunchTourCompleted**` in DataStore).

### Internationalization

- App strings: **English** defaults + **Arabic** `values-ar` resources (where translated).

---

## 14. Reference: Arab Bank v1 SMS sample

Use as the canonical golden string for parser tests and `BankTemplate` QA (`bank_id` e.g. `arab_bank`).

**Raw SMS (English):**

```text
A Trx using Card XXXX4259 from Talabat for JOD 12.410 on 04-Apr-2026 at 13:52 GMT+3. Available balance is JOD 6951.447.
```

**Expected parse (transaction line):**


| Field       | Value      |
| ----------- | ---------- |
| Amount      | 12.410 JOD |
| Merchant    | Talabat    |
| Date        | 2026-04-04 |
| Time        | 13:52      |
| Card last 4 | 4259       |
| Currency    | JOD        |


**Note:** The trailing **available balance** clause is present for realism; v1 parser may **ignore** balance unless a future PRD adds `available_balance` to the model.