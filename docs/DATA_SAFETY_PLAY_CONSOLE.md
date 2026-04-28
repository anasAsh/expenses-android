# Play Console — Data safety (v1) — form answers

Use with [STORE_CHECKLIST.md](../STORE_CHECKLIST.md). Replace placeholders where noted.

- **Data collection:** The app may collect **no personal data for servers**; processing is on-device. SMS is read for **financial transaction parsing** only, as disclosed in app.
- **Data types (SMS, financial):** Declare **yes** for SMS (read) if the form asks; clarify **processed only on device**; **not** uploaded to a developer backend in v1.
- **Data sharing:** **No** third-party sharing in v1 (no analytics SDK in v1; local DataStore/metrics are on-device only).
- **Encryption:** In transit **N/A** (no network upload in v1). **Device backup** may apply per user/OS settings; local Room DB and exports are under user control.
- **Data deletion:** User can **clear app data** / uninstall. Optional **local database export** is a user-initiated file write (SAF).
- **Account:** **No** account in v1.

**Privacy policy URL:** Set `privacy_policy_url` in app strings to your live URL before release; keep it in sync with the Data safety form.
