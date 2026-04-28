# Personal Expense & Budget Tracker — PRD

## 1. Overview

A low-effort, automation-first personal expense tracking system designed to minimize user interaction.

The system passively captures transactions (primarily via SMS), categorizes them automatically, and surfaces only meaningful insights such as overspending risks and monthly summaries.

The user is not expected to open the app daily or manage detailed financial records.

**v1 platform:** Android only. SMS-first ingestion requires SMS access that is not available to third-party apps on iOS in the background; see §6.

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
4. **First month setup** — User defines categories + monthly targets for the current calendar month (or next month if onboarding on last day — product choice: default to current month).

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
  - **`excluded_from_spend`** (boolean, default `false`) — When `true`, transactions assigned to this category do **not** count toward “total spent” vs budget rollups or threshold alerts for that category’s spend (use for internal transfers, loan principal moves, etc.). User decides per category.

#### Example
- Groceries — 400 JOD — included in spend  
- Transfers — 650 JOD — user may set `excluded_from_spend: true` if these are not discretionary spend  
- Eating Out — 200 JOD — included in spend  

#### Requirements
- No predefined categories
- No category hierarchy (flat structure only)
- Categories are:
  - Lightweight
  - Editable anytime within the same month
  - **Recreated or copied each month** — On rollover (§4.6), previous month’s categories **prefill** the new month; user **one-tap accepts** or edits before the new month is “locked” for alerts (alerts use accepted categories).

#### Spending totals
- **`current_spend`** is **derived**, not stored: sum of non-dismissed, non-refund-adjusted logic per §5 (refunds subtract; dismissed excluded).

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
- **JOD only** for budget math and alerts. SMS in other currencies: either **reject** (no transaction) or store with `needs_review` — product default: **reject** for v1 to avoid silent FX errors; `currency` field retained for forward compatibility.

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
- Regex-based extraction per **`BankTemplate`** (bank + language).
- Merchant normalization (e.g. TALABAT.COM → `talabat` → **`normalized_merchant_token`** for rules).

##### Confidence scoring
- Numeric **`confidence` ∈ [0, 1]`** computed from template match quality (all required capture groups present, checksums if any, etc.).
- **`confidence ≥ 0.85`** → transaction `status: auto` (accepted).
- **`confidence < 0.85`** → `status: needs_review` (shown in UI; no auto-delete at month boundary — carries forward until user resolves or dismisses per §4.9).

##### Pending vs settled
- Some banks send authorization SMS then settlement. **Rule:** Prefer **settled** amount when both exist for the “same” transaction. **Dedup hash** (see §4.2.2) links pending → settled; **replace** pending row with settled data when settlement arrives, or merge per implementation with single logical transaction.

##### Refunds / reversals
- Parser or user marks **`is_refund: true`**. Refund **subtracts** from spend for the assigned **`(category_id, month)`** (same rules as positive amounts, inverted sign in rollup).

---

#### 4.2.2 De-duplication

- **Duplicate** if all hold:
  - Same **`card_last4`** (or both null → use merchant-only path with stricter window)
  - Same **amount** to 3 decimal places
  - **`normalized_merchant`** string similarity **≥ 0.9** (e.g. normalized Levenshtein or token Jaccard — implementation detail)
  - Timestamps within **±5 minutes** of each other
- **Prevent** duplicate entries: second insert skipped or merged into first with updated fields (settlement wins over pending).

---

#### 4.2.3 Silent Processing

- No notifications per transaction
- Background processing only

---

### 4.3 Auto-Categorization

#### v1 (Rule-based)
- Rules keyed on **`normalized_merchant_token`** (single canonical string per merchant after normalization).
- Example seed mappings (optional app seeds, user-overridable):
  - `carrefour` → Groceries  
  - `talabat` → Eating Out  

#### Behavior
- If no rule matches: **`category_id` null** = **Uncategorized** (included in spend totals only after user assigns category or rule applies retroactively).
- **User correction** (user sets category on a transaction):
  - **Upsert** `Rule` for that `merchant_token` → `category_id`, `source: user_correction`
  - **Back-apply** (optional product default: **on**) to all **Uncategorized** transactions in the **current calendar month** with the same `normalized_merchant_token`

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

- **Trigger:** First local calendar day of new month (job at **00:05** local — see architecture doc).
- **Behavior:**
  - Spending totals for the closed month are frozen in reporting sense (data retained).
  - **New month** starts with **prefilled categories** copied from previous month (names, targets, `excluded_from_spend`); user **confirms or edits** in one flow.
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
- **Once per threshold per category per month** — tracked via **`AlertEvent`** (see §5).
- **Eligible categories for notification:** Top **N = 5** categories by **monthly_target** (descending). Others still show in-app but no push unless user opts into “all categories” (future).
- **Ignore “very small” categories** for push if **either**: target **< 5%** of total monthly budget (sum of all category targets that month), **or** target **< 30 JOD** (no push for that category; in-app still shows status).
- **Combine alerts** when possible (single notification summarizing multiple categories crossing thresholds in same evaluation window).
- **Quiet hours:** No push between **22:00–08:00** local; queue and send at **08:00** unless critical (v1: all alerts non-critical, batch at 08:00).

---

#### 4.7.2 Predictive alerts

##### Formula
Let `day_of_month` = today’s day index (1..D), `D` = days in month, `current_spend` = category spend month-to-date, `target` = monthly target.

**Projected month-end spend:** `projected = current_spend × (D / day_of_month)`

##### Trigger
- When **`projected ≥ 1.10 × target`** (110% of target) and category is eligible (same N / small-category rules as §4.7.1).
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
| Field | Description |
|--------|-------------|
| `id` | UUID or monotonic |
| `amount` | Decimal (3 dp JOD) |
| `currency` | v1: always `JOD` when accepted |
| `merchant` | Raw display string |
| `normalized_merchant` | Normalized for dedup / display |
| `normalized_merchant_token` | Canonical key for `Rule` lookup |
| `category_id` | Nullable → Uncategorized |
| `date` | Transaction date |
| `time` | Time of day |
| `source` | `SMS` \| `Manual` |
| `confidence` | Float [0, 1] |
| `status` | `auto` \| `needs_review` \| `manual` \| `dismissed` |
| `is_refund` | Boolean |
| `raw_sms` | Nullable; full SMS body for SMS-sourced rows |
| `card_last4` | Nullable |
| `dedup_hash` | Nullable; stable key for pending/settled merge |
| `bank_template_id` | Nullable FK to `BankTemplate` |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

---

### Category
| Field | Description |
|--------|-------------|
| `id` | |
| `month` | `YYYY-MM` — scope |
| `name` | Free text |
| `monthly_target` | JOD, 3 dp |
| `excluded_from_spend` | Boolean |
| `created_at` | |

**Note:** `current_spend` is **derived** in queries: sum of applicable `Transaction` amounts for `(category_id, month)`.

---

### Rule
| Field | Description |
|--------|-------------|
| `id` | |
| `merchant_token` | Matches `normalized_merchant_token` |
| `category_id` | FK |
| `source` | `user_correction` \| `seed` |
| `created_at` | |

---

### BankTemplate
| Field | Description |
|--------|-------------|
| `id` | |
| `bank_id` | Identifier (e.g. issuer code) |
| `language` | e.g. `en`, `ar` |
| `regex` | Pattern + capture group contract |
| `version` | Integer for migrations |

---

### AlertEvent
| Field | Description |
|--------|-------------|
| `id` | |
| `category_id` | |
| `month` | `YYYY-MM` |
| `threshold` | `70` \| `85` \| `100` \| `predictive` |
| `sent_at` | Timestamp |

Enforces **one notification per (category, month, threshold)** for threshold types.

---

### Card (optional UX)
| Field | Description |
|--------|-------------|
| `card_last4` | Primary key or unique |
| `nickname` | Optional user label |

---

## 6. Technical Requirements

- **Platform:** **Android only (v1).** Min SDK aligned with architecture doc (e.g. 26+). **Jetpack Compose** UI recommended.
- **Currency:** JOD, **3 decimal places** throughout storage and UI.
- **Permissions:** `READ_SMS` / receive SMS as required by target SDK; runtime permission flow on onboarding.
- **Google Play:** SMS and Call Log permissions are **restricted**. The app must comply with [Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/9047303): declare narrow use (financial transaction parsing), in-app disclosure, and **no misuse** as default SMS handler unless product explicitly becomes the default SMS app (not required for `SMS_RECEIVED` listener pattern — follow current Play policy for your integration choice).
- **Privacy / parsing:** **Local-only** parsing and storage for v1; no server upload unless user opts into **anonymous metrics** (§9). **Cloud backup** (e.g. Google Drive AppFolder) is **out of scope until v2** (§12).
- **Storage:** SQLite via **Room**; optional **SQLCipher** or file encryption with Keystore-wrapped keys for sensitive fields.
- **Preferences:** DataStore for flags (quiet hours opt-out, backup opt-in).
- **Background:** **WorkManager** for rollover, summary, periodic predictive evaluation.

---

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

- Android app
- SMS parsing (**Arab Bank** only in v1 — template seed + golden corpus in §14)
- Regex + `BankTemplate` pipeline
- User-defined monthly categories with rollover prefill
- Rule-based categorization + user correction → rules
- Manual entry + shorthand
- Threshold + predictive budget alerts (with `AlertEvent` deduplication)
- Monthly summary push (1st @ 09:00)
- Onboarding + optional 60-day backfill

---

## 9. Success Metrics

| Metric | Target | Measurement (v1) |
|--------|--------|-------------------|
| Transactions manually edited | < 5% | On-device counter: `edits / total_transactions` per rolling 30 days |
| App opens | ≤ 2× / week | On-device: session count / week |
| Alert engagement | > 60% | Tap or dismiss on notification within 48h / alerts sent |
| Monthly retention | > 80% | Return in same calendar month+1 (local only) |

**Optional:** Opt-in **anonymous** telemetry batch (no raw SMS) if a backend is introduced later; v1 can use **local-only** aggregates shown in a debug or “privacy” screen for dogfooding.

---

## 10. Future Roadmap

- Bank API integration (replace SMS)
- ML-based categorization
- Spending trends visualization
- Export (CSV/XLSX)
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

| Topic | Decision |
|--------|-----------|
| **Bank(s) in v1** | **Arab Bank** only — drives `BankTemplate` seeds and QA golden SMS corpus (§14). |
| **SMS template languages** | **English only** at launch. Arabic SMS templates deferred (may follow for same bank). |
| **Cloud backup** | **Not in v1.** Pure local storage until **v2** (Google Drive AppFolder or equivalent then). Optional SAF/file export can still be evaluated independently of Drive. |
| **Income** | **Out of scope for v1** — no “monthly income” field; variance vs income is roadmap ([§10](Budget_Tracker_PRD.md)). |

### Frozen for v1 (confirmed)

The following constants are **locked for v1** implementation and QA:

| Constant | Value |
|----------|--------|
| SMS parse auto-accept | `confidence ≥ 0.85`; else `needs_review` |
| Dedup time window | **±5 minutes** |
| Dedup merchant match | Similarity **≥ 0.9** (with amount + card rules per §4.2.2) |
| Push suppression (“small” category) | **OR** gate: target **< 5%** of total monthly budget **or** target **< 30 JOD** |
| Predictive alert trigger | **Projected ≥ 110%** of monthly target ([§4.7.2](Budget_Tracker_PRD.md)) |
| Push eligibility scope | **Top 5** categories by `monthly_target` ([§4.7.1](Budget_Tracker_PRD.md)) |

---

## 13. Internationalization (SMS)

- **v1:** **English** SMS templates only (see §12).
- **Arabic** templates for Arab Bank (or others): deferred post-launch unless prioritized.
- App UI language can be Arabic/English independently of SMS template language.

---

## 14. Reference: Arab Bank v1 SMS sample

Use as the canonical golden string for parser tests and `BankTemplate` QA (`bank_id` e.g. `arab_bank`).

**Raw SMS (English):**

```text
A Trx using Card XXXX4259 from Talabat for JOD 12.410 on 04-Apr-2026 at 13:52 GMT+3. Available balance is JOD 6951.447.
```

**Expected parse (transaction line):**

| Field | Value |
|--------|--------|
| Amount | 12.410 JOD |
| Merchant | Talabat |
| Date | 2026-04-04 |
| Time | 13:52 |
| Card last 4 | 4259 |
| Currency | JOD |

**Note:** The trailing **available balance** clause is present for realism; v1 parser may **ignore** balance unless a future PRD adds `available_balance` to the model.
