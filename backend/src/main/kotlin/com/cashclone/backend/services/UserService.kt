package com.cashclone.backend.services

import com.cashclone.backend.db.Users
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class UserRecord(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val cashtag: String,
    val fullName: String,
    val phone: String?,
    val kycStatus: String,
    val unitApplicationId: String?,
    val unitCustomerId: String?,
    val unitAccountId: String?,
)

private fun ResultRow.toUserRecord() = UserRecord(
    id = this[Users.id].value,
    email = this[Users.email],
    passwordHash = this[Users.passwordHash],
    cashtag = this[Users.cashtag],
    fullName = this[Users.fullName],
    phone = this[Users.phone],
    kycStatus = this[Users.kycStatus],
    unitApplicationId = this[Users.unitApplicationId],
    unitCustomerId = this[Users.unitCustomerId],
    unitAccountId = this[Users.unitAccountId],
)

object UserService {
    fun create(email: String, passwordHash: String, cashtag: String, fullName: String, phone: String?): UserRecord =
        transaction {
            val id = Users.insertAndGetId {
                it[Users.email] = email.lowercase()
                it[Users.passwordHash] = passwordHash
                it[Users.cashtag] = cashtag.lowercase()
                it[Users.fullName] = fullName
                it[Users.phone] = phone
                it[Users.kycStatus] = "NOT_STARTED"
                it[Users.createdAt] = Instant.now()
            }
            Users.selectAll().where { Users.id eq id }.first().toUserRecord()
        }

    fun findByEmail(email: String): UserRecord? = transaction {
        Users.selectAll().where { Users.email eq email.lowercase() }.singleOrNull()?.toUserRecord()
    }

    fun findByCashtag(cashtag: String): UserRecord? = transaction {
        Users.selectAll().where { Users.cashtag eq cashtag.lowercase() }.singleOrNull()?.toUserRecord()
    }

    fun findById(id: UUID): UserRecord? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.toUserRecord()
    }

    fun searchByCashtagPrefix(prefix: String, limit: Int = 10): List<UserRecord> = transaction {
        Users.selectAll()
            .where { Users.cashtag like "${prefix.lowercase()}%" }
            .limit(limit)
            .map { it.toUserRecord() }
    }

    fun startKyc(userId: UUID, applicationId: String) = transaction {
        Users.update({ Users.id eq userId }) {
            it[unitApplicationId] = applicationId
            it[kycStatus] = "PENDING"
        }
    }

    fun approveKyc(userId: UUID, customerId: String, accountId: String) = transaction {
        Users.update({ Users.id eq userId }) {
            it[unitCustomerId] = customerId
            it[unitAccountId] = accountId
            it[kycStatus] = "APPROVED"
        }
    }

    fun setKycStatus(userId: UUID, status: String) = transaction {
        Users.update({ Users.id eq userId }) {
            it[kycStatus] = status
        }
    }

    fun findByUnitApplicationId(applicationId: String): UserRecord? = transaction {
        Users.selectAll().where { Users.unitApplicationId eq applicationId }.singleOrNull()?.toUserRecord()
    }
}
