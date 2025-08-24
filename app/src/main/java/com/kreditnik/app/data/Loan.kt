package com.kreditnik.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Представляет сущность кредита или долгового обязательства.
 * Хранится в таблице "loans" базы данных Room.
 *
 * @property id Уникальный идентификатор записи, генерируется автоматически.
 * @property name Пользовательское название кредита (например, "Ипотека в Сбере").
 * @property type Тип обязательства (кредит, кредитная карта, рассрочка, долг).
 * @property logo Зарезервированное поле для хранения идентификатора логотипа банка.
 * @property interestRate Годовая процентная ставка.
 * @property startDate Дата открытия кредита или возникновения долга.
 * @property monthlyPaymentDay Число месяца для регулярного платежа.
 * @property usesSberbankCalculation Флаг, указывающий на использование метода расчета процентов
 * с учетом фактического количества дней в году (365/366).
 * @property reminderEnabled Включены ли уведомления для данного кредита.
 * @property reminderDaysBefore За сколько дней до даты платежа отправлять напоминание.
 * @property reminderTime Время отправки напоминания в формате "ЧЧ:мм".
 * @property initialPrincipal Изначальная сумма основного долга. Неизменяемое значение.
 * @property principal Текущий остаток основного долга, который уменьшается с платежами.
 * @property months Общий срок кредита в месяцах.
 * @property monthlyPayment Сумма ежемесячного аннуитетного платежа.
 * @property accruedInterest Сумма начисленных, но еще не уплаченных процентов.
 * @property lastInterestCalculationDate Дата последнего выполненного расчета процентов.
 * @property gracePeriodDays Длительность льготного периода для кредитных карт (в днях).
 * @property mandatoryPaymentDay День месяца для внесения обязательного платежа по кредитной карте.
 * @property gracePeriodEndDate Дата окончания текущего льготного периода.
 * @property debtDueDate Крайний срок возврата для долговых обязательств.
 * @property dayCountConvention Устаревшее поле, заменено на [usesSberbankCalculation].
 */
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: LoanType,
    val logo: String,
    val interestRate: Double,
    val startDate: LocalDate,
    val monthlyPaymentDay: Int,
    val usesSberbankCalculation: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderDaysBefore: Int? = 1,
    val reminderTime: String? = "12:00",
    val initialPrincipal: Double,
    val principal: Double,
    val months: Int,
    val monthlyPayment: Double,
    val accruedInterest: Double = 0.0,
    val lastInterestCalculationDate: LocalDate = startDate,
    val gracePeriodDays: Int?,
    val mandatoryPaymentDay: Int?,
    val gracePeriodEndDate: LocalDate?,
    val debtDueDate: LocalDate?,
    val dayCountConvention: DayCountConvention = DayCountConvention.SBER
)