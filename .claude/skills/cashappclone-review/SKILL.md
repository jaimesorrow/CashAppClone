---
name: cashappclone-review
description: Reviews diffs in this repo (CashClone, a peer-to-peer payments app moving real money through Unit.co's banking-as-a-service API) against its actual money-movement, KYC-gating, and auth invariants. Use this instead of a generic code review for any change touching backend/.../routes/TransferRoutes.kt, WebhookRoutes.kt, AccountRoutes.kt, KycRoutes.kt, services/TransferService.kt, services/UserService.kt, unit/UnitClient.kt, plugins/Security.kt, db/Tables.kt, or the Android send/request screens and viewmodels that construct amountCents.
---

# CashClone-specific review checklist

This is a two-module Kotlin app (`android/` Compose client, `backend/` Ktor server) that moves
real money via Unit.co. The backend owns auth and all Unit calls; the Android app never talks to
Unit directly. Check a diff against the following, grounded in what's actually implemented today —
don't apply generic advice that doesn't match this codebase's real shape.

## Money representation
- Money is `amountCents: Long` everywhere on the backend (`Transfers.amountCents`,
  `SendMoneyRequest`/`RequestMoneyRequest`/`TransferResponse.amountCents`, `UnitClient.createBookPayment`).
  Reject any new code that introduces `Double`/`Float`/`BigDecimal` for an amount that will reach
  the DB, a DTO, or Unit's API.
- `SendScreen.kt` and `RequestScreen.kt` currently convert user input with
  `(amount.toDoubleOrNull() ?: 0.0).let { (it * 100).toLong() }` — a `Double`-based cents conversion
  that truncates instead of rounding (e.g. floating-point drift can turn `19.99` into `1998` cents
  instead of `1999`). This is a known landmine, not a template: don't let new amount-entry code
  copy this pattern, and if a diff touches this conversion, flag whether it's finally fixing it
  (round, or parse cents directly) rather than just relocating the bug.
- Amounts must be validated positive (`if (req.amountCents <= 0) throw ApiException(BadRequest, ...)`)
  server-side in `TransferRoutes.kt` — this must stay on the backend, not just client-side, since the
  Android app is not trusted.

## KYC gate before any Unit money movement
- `requireApprovedAccount(user)` in `TransferRoutes.kt` (throws `PreconditionFailed` if
  `user.unitAccountId == null`) is the only gate standing between a user and a live Unit payment.
  Any new route or code path that calls `unitClient.createBookPayment` or otherwise moves money
  must call this (or the equivalent `unitAccountId` check) for *every* account involved — both
  sender and recipient in `/transfers/send`, both payer and requester in `/transfers/{id}/fulfill`.
  A path that reads `user.unitAccountId!!` or skips this check is a bug, not a stylistic nit.
- KYC status transitions (`NOT_STARTED` → `PENDING` → `APPROVED`/`DENIED`) are driven only from
  `UserService.startKyc`/`approveKyc`/`setKycStatus`. New code should not set `kycStatus` directly
  via ad hoc DB writes elsewhere.

## Webhook trust boundary — the biggest real gap in this repo
- `POST /webhooks/unit` (`WebhookRoutes.kt`) is unauthenticated and has **no signature
  verification**, despite `Env.unitWebhookSecret` (`UNIT_WEBHOOK_SECRET`) already existing in
  `Env.kt` — it is defined but never read anywhere in the codebase. Right now, anyone who can reach
  this endpoint can POST a fabricated `payment.sent` or `application.approved` event and get
  `TransferService.markCompleted` or `UserService.approveKyc` to fire for an arbitrary
  `transferId`/`applicationId`. If a diff touches `WebhookRoutes.kt`, check whether it's adding the
  HMAC-SHA256 verification the comment on that file already calls out (compare `x-unit-signature`
  against `UNIT_WEBHOOK_SECRET` over the raw body) — and don't accept new webhook handlers that
  trust event payload fields (`transferId`, `applicationId`, amounts) without that verification in
  place first.
- Webhook handlers key updates off client-supplied `tags.transferId`/`applicationId` with no
  cross-check against the amount or accounts already stored on that transfer/user — a forged event
  for a real `transferId` could mark an unrelated transfer completed. Any new webhook event type
  should validate the event's stated amount/accounts against the stored `TransferRecord` before
  mutating status.

## Auth and ownership checks
- Every authenticated route must sit inside `authenticate("auth-jwt") { ... }` and derive the
  caller's identity via `call.requireUserId()` (`RouteUtils.kt`), never from a client-supplied body
  field. `TransferRoutes.kt` already does the right thing (e.g. `/transfers/{id}/fulfill` checks
  `transfer.fromUserId != me.id` → `Forbidden`); new routes that touch a `TransferRecord` or
  `UserRecord` by ID must add the equivalent ownership check rather than trusting the path/body ID.
- `JwtService` issues 30-day tokens with no revocation list; there's no logout-side invalidation.
  Don't assume a token can be invalidated server-side when reviewing any "log out"/"revoke" feature.

## Idempotency (not currently implemented — flag new gaps, don't let them multiply)
- `POST /transfers/send` and `/transfers/{id}/fulfill` call `unitClient.createBookPayment` with no
  idempotency key and no dedup against a client-retried request — a network retry (client timeout,
  double-tap) can create a second `TransferService.create` row and a second real Unit payment for
  the same intent. This is an existing gap, not a pattern to copy into new endpoints; if a diff adds
  another money-moving call to `UnitClient`, check whether it at least reuses `tags["transferId"]`
  the way the existing code does, and don't add a second unguarded retry path (e.g. client-side
  automatic retry logic) on top of it without addressing this.

## Data exposure
- `plugins/StatusPages.kt`'s catch-all `exception<Throwable>` handler returns `cause.message`
  directly to the HTTP client for any uncaught exception (DB errors, Unit API errors included).
  `TransferRoutes.kt` also forwards `"Payment provider error: ${e.message}"` from Unit straight to
  the app. Don't let a diff add exception paths that leak more (e.g. stack traces, SQL, raw Unit
  response bodies) through this same mechanism.
- `KycRoutes.kt`/`UnitClient.kt` handle raw SSN and DOB (`KycApplicationRequest.ssn`,
  `IndividualApplicationInput.ssn`). Check that new logging, error messages, or webhook echoes never
  include these fields — currently they only flow into the Unit API call body, not into logs or
  responses, and that must stay true.
