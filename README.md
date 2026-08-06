# Paygo SMS Assistant

An Android app that runs on a phone holding a specific SIM, **reads incoming
SMS**, extracts payment details from mobile-money / bank messages, **auto-replies
via SMS and WhatsApp**, groups ordinary messages by sender/payee, **forwards**
everything to another number and WhatsApp, and stores it all in an on-device
database you can **export and download**.

Built for the Tanzanian payment ecosystem out of the box: **M-Pesa, Mixx by Yas
(Tigo Pesa), HaloPesa, T-Pesa, Airtel Money, Selcom, CRDB and NMB** — and fully
**trainable** so you can add or fix any provider yourself.

---

## What it does

| Requirement | How it's implemented |
|---|---|
| Read SMS received on the phone's SIM | `SmsReceiver` (`SMS_RECEIVED` broadcast) → `SmsProcessingService` |
| Extract amount, sender name, sender number, provider | `PaymentParser` driven by editable regex **read rules** |
| Support M-Pesa, Yas/Mixx, HaloPesa, T-Pesa, Selcom, CRDB, NMB … | `DefaultRules` seed set; add more from the UI |
| Train what to read and what to reply | **Training** screen: read rules, reply rules, and a live parser test |
| Reply by SMS and WhatsApp | `SmsSender` + `WhatsAppSender` (3 delivery modes) |
| Group normal messages by sender/payee | `ContactGroupEntity`, keyed by counterparty number/name |
| Forward notifications to another number + WhatsApp | `MessageProcessor.forward()` using the settings targets |
| Database for all information | Room (`PaygoDatabase`): messages, transactions, groups, rules, event log |
| Send & download the data | `DataExporter` → CSV / JSON shared via `FileProvider` |

## Screens

- **Home** – master on/off switch, auto-reply switch, live counters (SMS
  captured, payments, total received) and a feed of every reply/forward.
- **Payments** – the parsed transaction ledger.
- **Contacts** – conversations grouped by payee/sender with running totals; tap
  to see the full message history.
- **Training** – three tabs:
  - *Read rules*: per-provider regexes (sender, body, amount, name, number,
    reference, balance).
  - *Reply rules*: templates with placeholders (`{name} {amount} {currency}
    {provider} {reference} {number} {balance} {date}`), filtered by trigger
    (payment / normal / keyword), amount range or provider, sent over SMS and/or
    WhatsApp.
  - *Test*: paste a sample SMS and see exactly what the current rules extract.
- **Settings** – forwarding targets, WhatsApp delivery mode, and export buttons.

## Architecture

```
SmsReceiver ──▶ SmsProcessingService ──▶ MessageProcessor
                                            │
             ┌──────────────────────────────┼───────────────────────────┐
             ▼              ▼                ▼               ▼            ▼
        PaymentParser   Room DB        SmsSender      WhatsAppSender   Forwarder
        (read rules)  (store/group)  (auto-reply)  (intent/a11y/API)  (fan-out)
```

- **Language/UI:** Kotlin, Jetpack Compose (Material 3), Navigation Compose.
- **Storage:** Room + DataStore (settings).
- **Background:** a short-lived foreground service processes each message.
- **DI:** a small manual `ServiceLocator`.

## WhatsApp delivery — three modes (Settings ▸ WhatsApp delivery)

WhatsApp does not offer an open on-device "send message" API, so pick the mode
that fits your setup:

1. **INTENT** – opens WhatsApp with the message pre-filled; you tap send.
2. **ACCESSIBILITY** – the bundled accessibility service taps *send* for you.
   Enable **"Paygo WhatsApp Auto-Reply"** under *Android Settings ▸
   Accessibility*. Fully hands-free on-device.
3. **CLOUD_API** – the official **Meta WhatsApp Cloud API** (needs a Business
   phone-number id + token). The most reliable and Terms-of-Service-compliant
   path; recommended if you run a business line.

## Build & run

Requirements: Android Studio (Koala+), JDK 17, Android SDK 34.

```bash
./gradlew assembleDebug        # build the APK
./gradlew test                 # run the parser unit tests
```

Install the APK on the phone that holds the SIM, open the app, grant the SMS /
notification permissions it requests, then flip the **master switch** on Home.

### Recommended device setup for reliable background operation
- Grant **RECEIVE_SMS / READ_SMS / SEND_SMS** and **notifications**.
- Disable **battery optimisation** for the app (so it keeps receiving SMS).
- (Optional) enable the **Accessibility** service for WhatsApp auto-send.
- Keep the app installed on the dedicated "line" phone.

## Tuning a provider (training)

Real SMS wording changes over time. If a message isn't parsed:

1. Copy the SMS text into **Training ▸ Test** and run it.
2. If nothing matches, open the provider's **Read rule** and adjust the
   `sender` / `body` / `amount` regexes until Test shows the right values.
3. Add a brand-new provider with the **+ Read rule** button.

Amounts are normalised for `Tsh`/`TZS`, thousands separators and decimals.

## Notes & limitations

- Launching WhatsApp from the background is restricted on newer Android
  versions; **CLOUD_API** is the most robust for unattended automation.
- The app stores message content **only in its local database**; exports are
  written to app-private storage and shared only when you choose.
- Use responsibly and in line with local law, your mobile operator's terms, and
  WhatsApp's Terms of Service (especially for automated messaging).

## Tests

`app/src/test/java/com/greenleaf/paygo/parser/PaymentParserTest.kt` covers amount
normalisation and end-to-end parsing of a sample M-Pesa message.
