# Paygo SMS Assistant

An Android app that runs on a phone holding a specific SIM, **reads incoming
SMS**, extracts payment details from mobile-money / bank messages, **auto-replies
via SMS and WhatsApp**, groups ordinary messages by sender/payee, **forwards**
everything to another number and WhatsApp, and stores it all in an on-device
database you can **export and download**.

Built for the Tanzanian payment ecosystem out of the box: **Selcom "Lipa Kwa
Simu" vending**, **M-Pesa, Mixx by Yas (Tigo Pesa), HaloPesa, T-Pesa, Airtel
Money, CRDB and NMB** — and fully **trainable** so you can add or fix any
provider yourself. It also reads the free-form messages customers send and
**captures key info** (name, location, amount, system size, phone).

---

## What it does

| Requirement | How it's implemented |
|---|---|
| Read SMS received on the phone's SIM | `SmsReceiver` (`SMS_RECEIVED` broadcast) → `SmsProcessingService` |
| Extract amount, sender name, sender number, provider | `PaymentParser` driven by editable regex **read rules** |
| Support M-Pesa, Yas/Mixx, HaloPesa, T-Pesa, Selcom, CRDB, NMB … | `DefaultRules` seed set; add more from the UI |
| Train what to read and what to reply | **Training** screen: read rules, reply rules, and a live parser test |
| Reply by SMS and WhatsApp | `SmsSender` + `WhatsAppSender` (3 delivery modes) |
| Capture key info from free-form customer messages | `InfoExtractor` + trainable **capture rules** (name, location, amount, system size, phone) |
| Give each customer an **sms-cust-id** and track them | `CustomerEntity` + `CustomerId`; assigned on payment, quoted back by the customer, linked on reply |
| Group normal messages by sender/payee | `ContactGroupEntity`, keyed by counterparty number/name |
| Forward notifications to another number + WhatsApp | `MessageProcessor.forward()` using the settings targets |
| Database for all information | Room (`PaygoDatabase`): messages, transactions, groups, rules, event log |
| Send & download the data | `DataExporter` → CSV / JSON shared via `FileProvider` |

## Screens

- **Home** – master on/off switch, auto-reply switch, live counters (SMS
  captured, payments, total received) and a feed of every reply/forward.
- **Payments** – the parsed transaction ledger.
- **Customers** – two tabs: a *Customers* ledger keyed by sms-cust-id (name,
  product, location, total paid, phone) and raw *Conversations* grouped by
  sender with running totals; tap either to see the full message history.
- **Training** – four tabs:
  - *Read*: per-provider payment regexes (sender, body, amount, provider, name,
    number, reference, balance). The provider can be captured from the body, so
    one Lipa Kwa Simu rule reports `airtelmoney` when the funding wallet is named
    and falls back to `selcom` for direct number payments.
  - *Reply*: templates with placeholders (`{name} {amount} {currency}
    {provider} {reference} {number} {balance} {date}`), filtered by trigger
    (payment / normal / keyword), amount range or provider, sent over SMS and/or
    WhatsApp.
  - *Capture*: extractors that pull key fields out of free-form customer
    messages — each rule fills one field (`name`, `location`, `amount`,
    `system_size`, `phone`) via a regex.
  - *Test*: paste any sample message; see both the payment parse **and** the
    captured info.
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

## Hands-free auto-reply (default)

Everything is sent automatically on-device — no manual taps:

- **Normal SMS** is sent straight through the mobile operator with
  `SmsManager` (`SmsSender`). This is natively automatic; nothing extra to set up.
- **WhatsApp** uses the bundled **accessibility service** (the default mode):
  Paygo opens the pre-filled chat and taps *send* for you.

To turn on WhatsApp auto-send, open **Settings ▸ WhatsApp delivery** and tap
**Enable** — that jumps to *Android Settings ▸ Accessibility*, where you switch
on **"Paygo WhatsApp Auto-Reply"**. The Settings screen then shows a live
**Auto-send service: ON/OFF** status. The default payment reply goes out over
**both SMS and WhatsApp**.

### Other WhatsApp delivery modes (Settings ▸ WhatsApp delivery)

WhatsApp has no open on-device "send message" API, so two alternatives exist:

- **INTENT** – opens WhatsApp with the message pre-filled; you tap send (no
  accessibility service needed).
- **CLOUD_API** – the official **Meta WhatsApp Cloud API** (needs a Business
  phone-number id + token). The most robust path for fully unattended operation.

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

## Customer IDs (sms-cust-id)

To tie a customer's later texts back to their payment, the app can hand each
payer a short id and ask them to quote it:

1. **A payment arrives** → the payer (keyed by phone number) is assigned an
   **sms-cust-id** like `CUST-0001` (idempotent — repeat payments keep the same
   id). The confirmation reply includes `{custid}` and asks them to reply with
   it plus their name, location and product type.
2. **The customer replies** by normal SMS quoting `CUST-0001` (spacing/casing
   tolerant). The app detects the id, links the message to that customer, and
   fills in **name, location, product type** from the captured info. Replies
   from the paying number are linked even without the id.
3. The **Customers** tab lists everyone by **name, sms-cust-id, product type,
   location and amount paid**, with per-customer message history. Export it as
   `customers.csv`.

For the cases automation can't catch (a customer who texts from a different
number without quoting the id), the **Conversations** tab shows an **Assign to
customer** button on any unlinked message, and each customer's detail has an
**Edit details** action to correct name / location / product by hand.

Configure it under **Settings ▸ Customer tracking** (toggle + id prefix). The
default payment reply already contains `{custid}`; edit it under
**Training ▸ Reply**.

## Capturing info from customers' own messages

Besides the operator's payment SMS, customers often text their own details
("Nimelipa 80,000, jina Asha Mjape, nipo Mbeya, system 200W, 0712254863").
The **Capture** rules extract whatever fields are present — missing fields are
simply skipped. Captured info is shown under each message in **Contacts** and
included in the messages CSV / JSON export. Tune or add fields from
**Training ▸ Capture**, and verify with **Training ▸ Test**.

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
