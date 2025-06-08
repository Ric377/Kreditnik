package com.kreditnik.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.data.Operation
import com.kreditnik.app.ui.screens.PaymentScheduleItem
import com.kreditnik.app.data.DayCountConvention
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit


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

    private fun loadLoans() = viewModelScope.launch {
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

    fun updateOperation(operation: Operation) = viewModelScope.launch {
        repository.updateOperation(operation)
        loadOperations()
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

        val paymentDay = if (loan.monthlyPaymentDay == 0) 28 else loan.monthlyPaymentDay
        fun nextPaymentDate(d: LocalDate) =
            d.plusMonths(1).withDayOfMonth(minOf(paymentDay, d.plusMonths(1).lengthOfMonth()))

        // Ежемесячный аннуитетный платёж (уже округлён)
        val rawPay = monthlyPayment(loan.principal, loan.interestRate, loan.months)
        val monthlyPay = BigDecimal(rawPay.toString()).setScale(2, RoundingMode.HALF_EVEN)

        repeat(loan.months) { i ->
            val nextDate = nextPaymentDate(date)
            val daysBetween = if (loan.dayCountConvention == DayCountConvention.RETAIL)
                30
            else
                ChronoUnit.DAYS.between(date, nextDate).toInt()

            val daysInYear = if (loan.dayCountConvention == DayCountConvention.RETAIL)
                360
            else
                if (date.isLeapYear) 366 else 365



            // 1) годовая ставка в виде BigDecimal / 100
            val annualRate = BigDecimal(loan.interestRate.toString())
                .divide(BigDecimal("100"), 10, RoundingMode.HALF_EVEN)

            // 2) проценты за период (по дням)
            val interest = remaining
                .multiply(annualRate)
                .multiply(BigDecimal(daysBetween.toString()))
                .divide(BigDecimal(daysInYear.toString()), 10, RoundingMode.HALF_EVEN)
            val interestRounded = interest.setScale(2, RoundingMode.HALF_EVEN)

            // Признак последнего месяца
            val isLast = i == loan.months - 1

            // 3) основная часть = monthlyPay – проценты (или весь остаток в последний месяц)
            val principalPart = if (isLast) remaining else monthlyPay.subtract(interestRounded)

            // 4) итоговый платёж
            val totalPay = if (isLast) principalPart.add(interestRounded) else monthlyPay

            // 5) новый остаток
            val nextRemaining = if (isLast) BigDecimal.ZERO
            else remaining.subtract(principalPart).setScale(2, RoundingMode.HALF_EVEN)

            schedule += PaymentScheduleItem(
                monthNumber = i + 1,
                paymentDate = nextDate,
                totalPayment = totalPay.toDouble(),
                principalPart = principalPart.toDouble(),
                interestPart = interestRounded.toDouble(),
                remainingPrincipal = nextRemaining.toDouble()
            )

            if (isLast) return schedule
            remaining = nextRemaining
            date = nextDate
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
