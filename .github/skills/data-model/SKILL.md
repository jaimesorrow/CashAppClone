---
name: data-model
description: Reference for CashClone's backend schema (Exposed/Postgres tables, service-layer query and mutation helpers) — Users and Transfers. Use when asked what's the structure of the data, how do I query users or transfers, entity diagram, what fields does a table have, or how to add a new column/lookup.
---

# CashClone data model

Schema lives in `backend/src/main/kotlin/com/cashclone/backend/db/Tables.kt` (Exposed `UUIDTable`s).
Row access goes through the service layer, not raw Exposed calls in routes.

## PRIMARY ENTITIES

**`Users`** (`db/Tables.kt`) — id: UUID (PK)
- `email: VARCHAR(255)` unique
- `passwordHash: VARCHAR(255)`
- `cashtag: VARCHAR(40)` unique, stored lowercase
- `fullName: VARCHAR(255)`
- `phone: VARCHAR(32)?` nullable
- `kycStatus: VARCHAR(32)` default `"NOT_STARTED"` — one of `NOT_STARTED`/`PENDING`/`APPROVED`/`DENIED`
- `unitApplicationId: VARCHAR(64)?`, `unitCustomerId: VARCHAR(64)?`, `unitAccountId: VARCHAR(64)?` — all nullable, set only via KYC flow
- `createdAt: timestamp`

**`Transfers`** (`db/Tables.kt`) — id: UUID (PK)
- `fromUserId: UUID` (FK → Users), `toUserId: UUID` (FK → Users)
- `amountCents: Long`
- `note: VARCHAR(280)?` nullable
- `type: VARCHAR(16)` — `SEND` or `REQUEST`
- `status: VARCHAR(16)` — `PENDING`, `COMPLETED`, `FAILED`, `DECLINED`
- `unitPaymentId: VARCHAR(64)?` nullable
- `createdAt`, `updatedAt: timestamp`

## RELATIONSHIPS
- `Transfers.fromUserId` / `Transfers.toUserId` both reference `Users.id`. A `SEND` transfer has
  `fromUserId` = sender, `toUserId` = recipient; a `REQUEST` transfer has `fromUserId` = the payer
  being asked to pay, `toUserId` = the requester (see `TransferRoutes.kt`'s `/transfers/request`).
  No FK cascade rules are defined beyond the reference itself.

## VALIDATION RULES (enforced in route/service code, not DB constraints)
- `amountCents > 0` — checked in `TransferRoutes.kt` before `TransferService.create`, not in the DB.
- `email`/`cashtag` are lowercased before every insert and lookup (`UserService.create`, `findByEmail`, `findByCashtag`).
- A user cannot send/request money to/from themselves (`recipient.id == me.id` check in routes, not in schema).
- Money movement requires `unitAccountId != null` on both parties (`requireApprovedAccount`) — not a DB constraint.

## QUERY PATTERNS (`services/UserService.kt`, `services/TransferService.kt`)
- `Users.selectAll().where { Users.email eq email.lowercase() }.singleOrNull()`
- `Users.selectAll().where { Users.cashtag like "$prefix%" }.limit(n)` — `searchByCashtagPrefix`
- `Transfers.selectAll().where { (Transfers.fromUserId eq userId) or (Transfers.toUserId eq userId) }.orderBy(Transfers.createdAt, SortOrder.DESC).limit(n)` — `historyFor`
- All queries wrapped in `transaction { }`; row→model mapping via private `ResultRow.toUserRecord()`/`toTransferRecord()` extensions.

## MUTATIONS
- `UserService.create/startKyc/approveKyc/setKycStatus` — `Users.insertAndGetId { }` / `Users.update({ Users.id eq id }) { }`.
- `TransferService.create/markCompleted/markFailed` — `Transfers.insertAndGetId { }` / `Transfers.update({ Transfers.id eq id }) { }`; `updatedAt` is bumped on every status mutation.
