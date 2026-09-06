---
name: async-fulfillment-guard-review
description: Reviews TransferRoutes.kt's POST /transfers/{id}/fulfill and the Android "Pay" button it backs for a specific replay gap — the request-fulfillment guard is keyed on a status value ("PENDING") that a real, non-exceptional Unit book-payment response never moves off of. Use this instead of, or alongside, cashappclone-review's generic idempotency note for any change touching the fulfill route's status guard, TransferService's status transitions, WebhookRoutes.kt's handlePaymentCompleted, or HistoryScreen.kt's canFulfill/"Pay" button.
---

# Async fulfillment guard review

CashClone's core promise is that money moves correctly **and only when it should**. The fulfill
flow — the one place a user actively triggers a real Unit payment against an *existing* record
rather than creating a new one — has a guard that looks like it prevents double-payment but does
not, for the most realistic failure mode of the Unit API. This is narrower and more specific than
cashappclone-review's existing "Idempotency" section (which is about a **new** `TransferService.create`
row from a client-side network retry/double-tap); this skill is about **replaying the same existing
row** through completely normal, error-free UI usage.

## The mechanism

`TransferRoutes.kt`'s `/transfers/{id}/fulfill` handler:

```kotlin
if (transfer.type != "REQUEST" || transfer.status != "PENDING") {
    throw ApiException(HttpStatusCode.Conflict, "Transfer is not a pending request")
}
...
val payment = unitClient.createBookPayment(..., tags = mapOf("transferId" to transfer.id.toString()))
if (payment.status.equals("Sent", ignoreCase = true)) {
    TransferService.markCompleted(transfer.id, payment.id)
}
call.respond((TransferService.findById(transfer.id) ?: transfer).toResponse(me.id, requester))
```

`TransferService` only has `create` / `findById` / `markCompleted` / `markFailed` / `historyFor` —
there is no intermediate status (no `PROCESSING`/`AWAITING_SETTLEMENT`) and no `else` branch here.
Unit's book payments can legitimately come back `PendingReview` (held for compliance/AML review)
without the call throwing — that is exactly why `WebhookRoutes.kt` already has async
`payment.clearing`/`payment.rejected` handlers waiting for later resolution. When that happens on
`/fulfill`:

- The transfer row is **not** marked completed (only `"Sent"` does that).
- It is also **not** marked failed (only the `catch` block does that, and nothing threw).
- `transfer.status` is therefore still `"PENDING"` — bit-for-bit the same value it had *before* the
  call — so the guard at the top of the handler (`transfer.status != "PENDING"` → `Conflict`)
  imposes no protection at all against calling this endpoint again for the same transfer.

On the Android side, `HistoryScreen.kt` derives the "Pay" button from that same field:

```kotlin
val canFulfill = transfer.type == "REQUEST" && transfer.status == "PENDING" && transfer.direction == "OUTGOING"
```

`HistoryViewModel.fulfill()` calls the endpoint then `load()`s the list again — since the returned
transfer is still `PENDING`, the row re-renders with the "Pay" button showing again, with no error,
no "processing" indicator, and no explanation. A user who does not realize their tap already sent a
real payment has every reason to tap "Pay" again.

Note also that reusing `tags["transferId"]` on the Unit call — the exact mitigation
cashappclone-review's idempotency note suggests — does **not** help here: Unit does not dedupe
`createBookPayment` calls by tag value, tags are informational metadata read back later by the
webhook handler, not an idempotency key sent to Unit's API.

## Concrete failure scenario

1. Alice requests $50 from Bob (`POST /transfers/request` → transfer `T`, `status = PENDING`).
2. Bob taps "Pay" in `HistoryScreen`. `/transfers/T/fulfill` calls Unit; Unit returns `PendingReview`
   (not `Sent`, no exception). `T` stays `PENDING`.
3. The response reaches Bob's phone unchanged; `HistoryViewModel.fulfill()` reloads history; `T` is
   still `PENDING` and outgoing, so the "Pay" button reappears exactly as before Bob's tap.
4. Bob, seeing what looks like an unfulfilled request, taps "Pay" again. The Conflict guard does not
   fire (`T.status` is still `"PENDING"`). A second real `createBookPayment` fires for the same $50,
   tagged with the same `transferId`.
5. Unit later approves and settles **both** book payments. Two `payment.sent` webhook events arrive
   for the same `tags.transferId = T` with two different `paymentId`s. `handlePaymentCompleted`
   calls `TransferService.markCompleted(T, paymentId)` unconditionally both times — the second call
   silently overwrites the first's `unitPaymentId`. The app ends up showing transfer `T` as a single
   `COMPLETED` $50 request, while Bob's Unit account was actually debited **twice** — $100 total —
   and nothing in the API response, the DB, or the UI ever surfaces that two payments happened.

## What to check in a diff

- Any change to the fulfill handler should move the row **out of `"PENDING"`** (a new status such as
  `PROCESSING`, or a DB-level compare-and-swap `UPDATE ... WHERE status = 'PENDING'` that only one
  concurrent/sequential call can win) *before* or atomically with the `unitClient.createBookPayment`
  call — not only after a `"Sent"` response comes back. A fix that only adds a mutex/lock around the
  existing status check without introducing a new status still leaves the same request re-payable
  after the lock releases, since the stored status is unchanged.
- `WebhookRoutes.kt`'s `handlePaymentCompleted`/`handlePaymentFailed` should not blindly
  `markCompleted`/`markFailed` a transfer that is already `COMPLETED` with a **different**
  `unitPaymentId` on file — that mismatch is precisely the signature of two real payments landing on
  one local record, and it is currently invisible (no log, no alert, no distinct status).
- `Tables.kt` documents a `DECLINED` status that no route ever sets — there is no "decline this
  request" action anywhere in the backend or the Android UI. Every unresolved `REQUEST` transfer
  stays fulfillable forever with no way for the payer to close it out except by paying it (or an
  operator manually editing the DB). If a diff adds a decline path, check that it also closes off the
  fulfill guard (declined requests must fail the same `status != "PENDING"` check fulfill uses).
- The same reasoning applies to any other endpoint added later that re-uses an existing
  `TransferRecord` id to trigger a Unit call gated only by that record's own `status` column.
