// Loan.kt
package com.kreditnik.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: LoanType,
    val logo: String,
    val interestRate: Double,
    val startDate: LocalDate,
    val monthlyPaymentDay: Int,         // День месяца платежа для кредитов
    val usesSberbankCalculation: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderDaysBefore: Int? = 1,
    val reminderTime: String? = "12:00",



    /**
     * Изначальная сумма кредита. Не меняется.
     * Используется для расчета статичного графика платежей.
     */
    val initialPrincipal: Double,

    /**
     * Текущий остаток основного долга. Уменьшается с каждым платежом.
     */
    val principal: Double,

    val months: Int,                    // Срок кредита (в месяцах)
    val monthlyPayment: Double,         // Ежемесячный платеж

    // НОВЫЕ ПОЛЯ для ежедневных процентов (НЕ МЕНЯЮТ ДИЗАЙН НАПРЯМУЮ)
    val accruedInterest: Double = 0.0, // Начисленные, но еще не оплаченные проценты
    val lastInterestCalculationDate: LocalDate = startDate, // Дата последнего начисления процентов

    // Для кредитных карт
    val gracePeriodDays: Int?,          // Льготный период в днях
    val mandatoryPaymentDay: Int?,      // Число обязательного платежа
    val gracePeriodEndDate: LocalDate?, // Дата окончания льготного периода

    // Для долгов
    val debtDueDate: LocalDate?,         // Дата возврата долга
    val dayCountConvention: DayCountConvention = DayCountConvention.SBER
)