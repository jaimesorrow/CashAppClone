# CashClone

A Cash App–style peer-to-peer payments app: Kotlin end to end — a Jetpack
Compose Android client talking to a Ktor backend, which moves real money via
[Unit](https://unit.co)'s banking-as-a-service platform.

Unit (not this project) holds the money-transmitter licensing, banking
partnership, and KYC/AML compliance that real money movement legally
requires. This repo only orchestrates Unit's API and never talks to the
banking rails directly.

## Architecture

```
android/   Jetpack Compose app (Kotlin). Talks only to the backend below —
           never directly to Unit — so provider credentials never touch a
           device.
backend/   Ktor server (Kotlin). Owns user auth, app-level data (cashtags,
           transfer records), and all calls to Unit's API.
```

Money flow: a user signs up in the app → submits KYC info → the backend
opens a Unit individual application → once Unit approves it (via webhook)
the user gets a real deposit account → balances and transfers are then read
from / written to that Unit account through the backend.

## Backend setup

1. `cd backend`
2. Copy `.env.example` to `.env` and fill in:
   - A Postgres connection (`DB_URL`/`DB_USER`/`DB_PASSWORD`)
   - `JWT_SECRET` — any long random string for local dev
   - `UNIT_API_TOKEN` — from a free [Unit sandbox](https://unit.co) org
     (Dashboard → Developers → API Tokens). Sandbox base URL is already the
     default (`https://api.s.unit.sh`).
3. Point Unit's sandbox webhook (Dashboard → Developers → Webhooks) at
   `https://<your-tunnel>/webhooks/unit` (use `ngrok` or similar for local
   dev) so application-approval and payment events reach the backend.
4. Run it: `./gradlew run` (starts on `:8080`).

Going to production with real (non-sandbox) money movement requires
completing Unit's program/underwriting agreement — that's a business step
with Unit, not a code change.

## Android setup

1. Open `android/` in Android Studio (Koala+) or build from the CLI:
   `./gradlew :app:assembleDebug`.
2. The app points at `http://10.0.2.2:8080` by default (the Android
   emulator's alias for the host machine's `localhost`) — see
   `API_BASE_URL` in `android/app/build.gradle.kts`. Point it at your
   backend's real address for a physical device.
3. Run the `app` configuration on an emulator or device.

## What's implemented

- Signup / login (JWT-based)
- KYC application submission and status polling, backed by Unit
  individual applications
- Live balance lookup from the user's Unit deposit account
- Cashtag search
- Send money (instant book payment between two Unit accounts)
- Request money (pending record; the payer fulfills it, triggering the
  actual payment)
- Transaction history
- Unit webhook handling for application approval/denial and payment
  completion/failure

## What's not implemented (by design, for this scaffold)

- Debit card issuing/spending (Unit supports this; not wired up here)
- Direct deposit / ACH funding into the account
- Push notifications
- Webhook signature verification (documented in `WebhookRoutes.kt`, must be
  added before production)
