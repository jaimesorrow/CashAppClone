package com.cashclone.backend.services

import com.cashclone.backend.db.Transfers
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class TransferRecord(
    val id: UUID,
    val fromUserId: UUID,
    val toUserId: UUID,
    val amountCents: Long,
    val note: String?,
    val type: String,
    val status: String,
    val unitPaymentId: String?,
    val createdAt: Instant,
)

private fun ResultRow.toTransferRecord() = TransferRecord(
    id = this[Transfers.id].value,
    fromUserId = this[Transfers.fromUserId].value,
    toUserId = this[Transfers.toUserId].value,
    amountCents = this[Transfers.amountCents],
    note = this[Transfers.note],
    type = this[Transfers.type],
    status = this[Transfers.status],
    unitPaymentId = this[Transfers.unitPaymentId],
    createdAt = this[Transfers.createdAt],
)

object TransferService {
    fun create(
        fromUserId: UUID,
        toUserId: UUID,
        amountCents: Long,
        note: String?,
        type: String,
        status: String,
        unitPaymentId: String? = null,
    ): TransferRecord = transaction {
        val now = Instant.now()
        val id = Transfers.insertAndGetId {
            it[Transfers.fromUserId] = fromUserId
            it[Transfers.toUserId] = toUserId
            it[Transfers.amountCents] = amountCents
            it[Transfers.note] = note
            it[Transfers.type] = type
            it[Transfers.status] = status
            it[Transfers.unitPaymentId] = unitPaymentId
            it[Transfers.createdAt] = now
            it[Transfers.updatedAt] = now
        }
        Transfers.selectAll().where { Transfers.id eq id }.first().toTransferRecord()
    }

    fun findById(id: UUID): TransferRecord? = transaction {
        Transfers.selectAll().where { Transfers.id eq id }.singleOrNull()?.toTransferRecord()
    }

    fun markCompleted(id: UUID, unitPaymentId: String?) = transaction {
        Transfers.update({ Transfers.id eq id }) {
            it[Transfers.status] = "COMPLETED"
            if (unitPaymentId != null) it[Transfers.unitPaymentId] = unitPaymentId
            it[Transfers.updatedAt] = Instant.now()
        }
    }

    fun markFailed(id: UUID) = transaction {
        Transfers.update({ Transfers.id eq id }) {
            it[Transfers.status] = "FAILED"
            it[Transfers.updatedAt] = Instant.now()
        }
    }

    fun historyFor(userId: UUID, limit: Int = 50): List<TransferRecord> = transaction {
        Transfers.selectAll()
            .where { (Transfers.fromUserId eq userId) or (Transfers.toUserId eq userId) }
            .orderBy(Transfers.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toTransferRecord() }
    }
}
