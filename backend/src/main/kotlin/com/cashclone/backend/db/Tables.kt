package com.cashclone.backend.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val cashtag = varchar("cashtag", 40).uniqueIndex()
    val fullName = varchar("full_name", 255)
    val phone = varchar("phone", 32).nullable()
    val kycStatus = varchar("kyc_status", 32).default("NOT_STARTED")
    val unitApplicationId = varchar("unit_application_id", 64).nullable()
    val unitCustomerId = varchar("unit_customer_id", 64).nullable()
    val unitAccountId = varchar("unit_account_id", 64).nullable()
    val createdAt = timestamp("created_at")
}

object Transfers : UUIDTable("transfers") {
    val fromUserId = reference("from_user_id", Users)
    val toUserId = reference("to_user_id", Users)
    val amountCents = long("amount_cents")
    val note = varchar("note", 280).nullable()
    val type = varchar("type", 16) // SEND or REQUEST
    val status = varchar("status", 16) // PENDING, COMPLETED, FAILED, DECLINED
    val unitPaymentId = varchar("unit_payment_id", 64).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
