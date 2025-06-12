package com.kreditnik.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.data.Operation
import com.kreditnik.app.ui.screens.PaymentScheduleItem
import com.kreditnik.app.data.DayCountConvention
import com.kreditnik.app.data.OperationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit
import java.time.LocalDateTime



class LoanViewModel(private val repository: LoanRepository) : ViewModel() {

    /* ---------- кредиты ---------- */

    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> get() = _loans

    /* ---------- операции ---------- */

    private val _operations = MutableStateFlow<List<Operation>>(emptyList())
    val operations: StateFlow<List<Operation>> get() = _operations

    /* ---------- init ---------- */

    init {
        loadLoans()
        loadOperations()
    }

    /* ---------- загрузка ---------- */

    fun loadLoans() = viewModelScope.launch {
        _loans.value = repository.getAllLoans()
    }


    fun loadOperations() = viewModelScope.launch {
        _operations.value = repository.getAllOperations()
    }

    /* ---------- работа с кредитами ---------- */

    suspend fun addLoan(loan: Loan) {
        repository.insertLoan(loan)
        loadLoans()
    }


    fun updateLoan(loan: Loan) = viewModelScope.launch {
        repository.updateLoan(loan)
        loadLoans()
    }

    fun deleteLoan(loan: Loan) = viewModelScope.launch {
        repository.deleteLoan(loan)
        loadLoans()
    }

    fun updateLoanPrincipal(loanId: Long, delta: Double) = viewModelScope.launch {
        repository.getLoanById(loanId)?.let { loan ->
            repository.updateLoan(loan.copy(principal = loan.principal + delta))
            loadLoans()
        }
    }

    fun getLoanNameById(loanId: Long): String =
        _loans.value.firstOrNull { it.id == loanId }?.name ?: "Неизвестный кредит"

    /* ---------- работа с операциями ---------- */

    fun addOperation(operation: Operation) = viewModelScope.launch {
        repository.insertOperation(operation)
        updateLoanPrincipal(operation.loanId, operation.amount)
        loadOperations()
    }

    /**
     * «Ежемесячный платёж» по аннуитету.
     */
    /**
     * «Ежемесячный платёж» по аннуитету.
     * Эта версия функции корректно разделяет платёж на проценты и основное тело долга.
     * Она обновляет остаток кредита (`principal`) только на сумму погашения основного долга,
     * что соответствует стандартной банковской логике.
     */
    fun payMonthly(loan: Loan) = viewModelScope.launch {
        // 1. Определяем, какой по счёту этот платёж, чтобы узнать оставшийся срок.
        val monthlyPaymentsMade = _operations.value.count {
            it.loanId == loan.id && it.type == OperationType.PAYMENT && it.description == "Ежемесячный платёж"
        }

        // Если все платежи уже сделаны, выходим.
        if (monthlyPaymentsMade >= loan.months) {
            return@launch
        }

        val remainingMonths = loan.months - monthlyPaymentsMade
        if (remainingMonths <= 0) return@launch

        // 2. Рассчитываем полный ежемесячный платёж.
        // Так как у нас нет исходной суммы кредита, мы рассчитываем платёж на основе
        // оставшегося срока и текущего остатка. Это компромисс из-за текущей модели данных.
        val totalMonthlyPayment = monthlyPayment(loan.principal, loan.interestRate, remainingMonths)

        // 3. Рассчитываем проценты за текущий период.
        // Формула приближена к стандартной банковской практике.
        val monthlyRate = loan.interestRate / 100 / 12
        val interestPart = loan.principal * monthlyRate

        // 4. Рассчитываем, какая часть платежа идёт на погашение основного долга.
        val principalPart = totalMonthlyPayment - interestPart

        // 5. Корректируем суммы для последнего платежа, чтобы долг закрылся в ноль.
        val finalPrincipalPart = if (remainingMonths == 1) loan.principal else principalPart.coerceAtLeast(0.0)
        val finalTotalPayment = if (remainingMonths == 1) loan.principal + interestPart else totalMonthlyPayment

        // 6. Создаём операцию в истории на *полную* сумму списания.
        repository.insertOperation(
            Operation(
                loanId      = loan.id,
                amount      = -finalTotalPayment, // Операция на всю сумму списания
                date        = LocalDateTime.now(),
                type        = OperationType.PAYMENT,
                description = "Ежемесячный платёж"
            )
        )

        // 7. Обновляем основной долг, вычитая ТОЛЬКО часть, идущую на его погашение.
        // Это ключевое исправление.
        val currentLoan = repository.getLoanById(loan.id) ?: return@launch
        val newPrincipal = (currentLoan.principal - finalPrincipalPart).coerceAtLeast(0.0)
        repository.updateLoan(currentLoan.copy(principal = newPrincipal))

        // 8. Обновляем состояние, чтобы UI отразил изменения.
        loadOperations()
        loadLoans()
    }

    /**
     * Досрочное погашение сверх аннуитета.
     */
    fun prepay(loan: Loan, extra: Double) = viewModelScope.launch {
        repository.insertOperation(
            Operation(
                loanId      = loan.id,
                amount      = -extra,
                date        = LocalDateTime.now(),
                type        = OperationType.PAYMENT,
                description = "Досрочное погашение"
            )
        )
        loadOperations()
    }


    fun updateOperation(operation: Operation) {
        viewModelScope.launch {
            repository.updateOperation(operation)
            repository.recalculateLoanAfterOperationUpdate(operation)
            loadOperations()
            loadLoans()
        }
    }




    fun deleteOperation(operation: Operation) = viewModelScope.launch {
        repository.deleteOperation(operation)
        updateLoanPrincipal(operation.loanId, -operation.amount)
        loadOperations()
    }

    /* ---------- график платежей ---------- */
    fun calculatePaymentSchedule(loan: Loan): List<PaymentScheduleItem> {
        val schedule = mutableListOf<PaymentScheduleItem>()
        var remaining = BigDecimal(loan.principal.toString()).setScale(2, RoundingMode.HALF_EVEN)
        var date = loan.startDate

        // Функция для вычисления даты следующего платежа
        val paymentDay = if (loan.monthlyPaymentDay == 0) 28 else loan.monthlyPaymentDay
        fun nextPaymentDate(d: LocalDate) =
            d.plusMonths(1).withDayOfMonth(minOf(paymentDay, d.plusMonths(1).lengthOfMonth()))

        // Фиксированный аннуитетный платёж, уже округлён до копеек
        val rawPay = monthlyPayment(loan.principal, loan.interestRate, loan.months)
        val monthlyPay = BigDecimal(rawPay.toString()).setScale(2, RoundingMode.HALF_EVEN)

        repeat(loan.months) { i ->
            val nextDate = nextPaymentDate(date)

            // считаем дни в периоде и базу "дней в году" в зависимости от конвенции
            val daysBetween = if (loan.dayCountConvention == DayCountConvention.RETAIL) 30
            else ChronoUnit.DAYS.between(date, nextDate).toInt()
            val daysInYear  = if (loan.dayCountConvention == DayCountConvention.RETAIL) 360
            else if (date.isLeapYear) 366 else 365

            // годовая ставка как BigDecimal
            val annualRate = BigDecimal(loan.interestRate.toString())
                .divide(BigDecimal("100"), 10, RoundingMode.HALF_EVEN)

            // проценты за период
            val interest = remaining
                .multiply(annualRate)
                .multiply(BigDecimal(daysBetween.toString()))
                .divide(BigDecimal(daysInYear.toString()), 10, RoundingMode.HALF_EVEN)
            val interestRounded = interest.setScale(2, RoundingMode.HALF_EVEN)

            if (loan.dayCountConvention == DayCountConvention.RETAIL) {
                // === 30/360: фиксированный платёж каждый месяц ===
                val principalPart = monthlyPay.subtract(interestRounded)
                val totalPay     = monthlyPay
                val rawNext      = remaining.subtract(principalPart)
                val nextRem      = if (i == loan.months - 1) BigDecimal.ZERO
                else rawNext.setScale(2, RoundingMode.HALF_EVEN)

                schedule += PaymentScheduleItem(
                    monthNumber        = i + 1,
                    paymentDate        = nextDate,
                    totalPayment       = totalPay.toDouble(),
                    principalPart      = principalPart.toDouble(),
                    interestPart       = interestRounded.toDouble(),
                    remainingPrincipal = nextRem.toDouble()
                )
                if (i == loan.months - 1) return schedule

                remaining = nextRem
                date = nextDate

            } else {
                // === Actual/Actual: последний платёж "добивка" по остатку ===
                val isLast        = i == loan.months - 1
                val principalPart = if (isLast) remaining else monthlyPay.subtract(interestRounded)
                val totalPay      = if (isLast) principalPart.add(interestRounded) else monthlyPay
                val nextRem       = if (isLast) BigDecimal.ZERO
                else remaining.subtract(principalPart).setScale(2, RoundingMode.HALF_EVEN)

                schedule += PaymentScheduleItem(
                    monthNumber        = i + 1,
                    paymentDate        = nextDate,
                    totalPayment       = totalPay.toDouble(),
                    principalPart      = principalPart.toDouble(),
                    interestPart       = interestRounded.toDouble(),
                    remainingPrincipal = nextRem.toDouble()
                )
                if (isLast) return schedule

                remaining = nextRem
                date = nextDate
            }
        }

        return schedule
    }




    private fun monthlyPayment(principal: Double, annualRate: Double, months: Int): Double {
        val mRate = annualRate / 100 / 12
        val rawPayment = if (mRate == 0.0) principal / months
        else principal * mRate * (1 + mRate).pow(months) / ((1 + mRate).pow(months) - 1)
        return kotlin.math.round(rawPayment * 100) / 100
    }

    private fun Double.pow(n: Int) = Math.pow(this, n.toDouble())
}
