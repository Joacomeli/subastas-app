package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.PasswordResetCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PasswordResetCodeRepository : JpaRepository<PasswordResetCode, Long> {
    fun findTopByEmailAndUsedFalseOrderByExpiryDateDesc(email: String): Optional<PasswordResetCode>
    fun findAllByEmailAndUsedFalse(email: String): List<PasswordResetCode>
}
