package com.cashclone.backend.services

import org.mindrot.jbcrypt.BCrypt

object PasswordService {
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))

    fun verify(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)
}
