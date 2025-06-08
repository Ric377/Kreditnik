package com.kreditnik.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import com.kreditnik.app.data.DayCountConvention

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: LoanType,
    val logo: String,
    val interestRate: Double,
    val startDate: LocalDate,
    val monthlyPaymentDay: Int,         // День месяца платежа для кредитов
    val principal: Double,
    val months: Int,                    // Срок кредита (в месяцах)

    // Для кредитных карт
    val gracePeriodDays: Int?,          // Льготный период в днях
    val mandatoryPaymentDay: Int?,      // Число обязательного платежа
    val gracePeriodEndDate: LocalDate?, // Дата окончания льготного периода

    // Для долгов
    val debtDueDate: LocalDate?,         // Дата возврата долга
    val dayCountConvention: DayCountConvention = DayCountConvention.SBER
)
