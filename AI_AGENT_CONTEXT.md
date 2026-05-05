# AI agent context — Expenses (Android)

Purpose-built handoff for another coding agent. Canonical project rules also live in [`CLAUDE.md`](CLAUDE.md); deep design lives in [`ARCHITECTURE.md`](ARCHITECTURE.md) and [`Budget_Tracker_PRD.md`](Budget_Tracker_PRD.md).

## Product

- **App display name:** `Expenses` (`res/values/strings.xml` → `app_name`).
- **Package / namespace:** `com.anasexpenses.budget` (unchanged).

## Build (JDK 17, Android SDK)

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --tests "*.RegexBankSmsParserTest"
```

See `CLAUDE.md` for release build and full test targets.

## Architecture (short)

| Layer | Location | Notes |
|--------|-----------|--------|
| UI | `app/src/main/java/.../ui/` | Compose, Hilt ViewModels, `StateFlow`. Shell: Home, Transactions, Settings; onboarding gate in `RootViewModel`. |
| Domain | `.../domain/` | Pure Kotlin: `JodMoney`, `BudgetRollup`, `DedupMatcher`/`DedupHash`, `MerchantNormalizer`, `BudgetCycle`, parsers’ helpers. |
| Data | `.../data/` | Room (`BudgetDatabase`), `TransactionRepository`, `CategoryRepository`, `UserPreferencesRepository` (DataStore), seeds (`BudgetSeed`). |
| Integration | `.../sms/`, `.../alerts/`, `.../work/` | SMS receiver → ingest; alerts via `BudgetAlertCoordinator`; periodic work. |

**Money:** amounts in **`amountMilliJod`** — thousandths of JOD (`JodMoney`); do not use raw `Double` for money.

**Budget month:** `UserPreferencesRepository.selectedMonth` (`YearMonth`) + `budgetCycleStartDay` → epoch-day range via `BudgetCycle.epochDayRangeInclusive`. Transaction lists filter by **date falling in that range**, not calendar month alone.

## SMS ingestion (as implemented)

**Live `SMS_RECEIVED`:** [`SmsTransactionReceiver`](app/src/main/java/com/anasexpenses/budget/sms/SmsTransactionReceiver.kt)

- **`Telephony.Sms.Intents.getMessagesFromIntent`** assembles multipart bodies (`SmsIntentReader`).
- **No body pre-filter** on the receiver path (everything is passed to the repository). Parsing is cheap on non-match.
- **Requires `RECEIVE_SMS`:** Android does not deliver this broadcast if permission is denied. Onboarding does not enforce it; **`SmsBankPermissionBanner`** on Home prompts for SMS + **`READ_SMS`**.
- `goAsync()` + `@ApplicationScope` coroutine + `finally { pendingResult.finish() }`; ingest wrapped in **try/catch** + **`BudgetLog`** on failure (no raw SMS logged).

**Repository:** [`TransactionRepository.ingestSmsBodies`](app/src/main/java/com/anasexpenses/budget/data/TransactionRepository.kt)

- **`normalizeBankSmsBody`:** trim, `\r→\n`, strip `\u200e\u200f\u200c\u200d\uFEFF`, NBSP/thin NBSP → space.
- **Parse order (`parseArabBankSms`):**
  1. If body contains **`كليك`** → **`RegexBankSmsParser.parseArabClickCredit`** first (Arab Bank Arabic Click variants).
  2. Else DB **`BankTemplateEntity`** regex (`arab_bank`, `en`) if present.
  3. Shipped English regex **`BudgetSeed.ARAB_BANK_TRX_EN_REGEX`**.
  4. **`parseArabClickCredit`** again (covers edge cases).

**Arabic Click:** [`RegexBankSmsParser.kt`](app/src/main/java/com/anasexpenses/budget/sms/parser/RegexBankSmsParser.kt) — multiple patterns (with/without balance line, relaxed `كليك`…`بمبلغ`…`JOD`…`*NNNN`**). Confidence **0.82** (< auto threshold unless a rule assigns category).

**English card SMS:** seeded regex in `BudgetSeed`; DB template can override/shadow until fallback runs.

**Inbox backfill / filter:** [`ArabBankSmsFilter`](app/src/main/java/com/anasexpenses/budget/sms/ArabBankSmsFilter.kt) — still used for **`SmsInboxBackfill`** heuristic only (narrower than receiver).

## Manual transactions

- UI: **`TransactionsScreen`** — merchant + amount (JOD) + category; shows **budget month** line.
- **`TransactionRepository.insertManualEntry`** / clamp **`dateEpochDay`** into **`BudgetCycle.epochDayRangeInclusive(selectedMonth, cycleStart)`** so rows appear under the viewed month.

## Dedup / visibility gotchas

- **`DedupMatcher`:** ±5 min, amount-day match, card + merchant similarity — duplicate ingest is skipped silently.
- **`dedup_hash`** updates when editing merchant/amount/date in **`TransactionEditViewModel`**.
- A parsed SMS may **not appear** under the selected Home/Transactions month if **SMS date epoch** falls **outside** the current **budget cycle window** — change month picker or interpret as data, not parsing failure.

## Strings / locales

- Default: `res/values/strings.xml`; Arabic: `res/values-ar/strings.xml`.
- Avoid editing `README.md`/PRD unless the user asks (per house rules).

## When changing Room

Schema is versioned with **destructive fallback** unless you add migrations — wipes user data if bumped carelessly.

---

*Last curated for agent handoffs; reconcile with git if this drifts.*
