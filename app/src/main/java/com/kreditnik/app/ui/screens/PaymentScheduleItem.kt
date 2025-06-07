package com.kreditnik.app.ui.screens

import java.time.LocalDate

data class PaymentScheduleItem(
    val monthNumber: Int,
    val paymentDate: LocalDate,
    val totalPayment: Double,
    val principalPart: Double,
    val interestPart: Double,
    val remainingPrincipal: Double
)
