package com.kreditnik.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.data.Operation
import com.kreditnik.app.ui.screens.PaymentScheduleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    fun addLoan(loan: Loan) = viewModelScope.launch {
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
        var remaining = loan.principal
        var date = loan.startDate.plusMonths(1)
        val monthlyPay = monthlyPayment(loan.principal, loan.interestRate, loan.months)

        repeat(loan.months) { month ->
            val days = date.lengthOfMonth()
            val interest = remaining * (loan.interestRate / 100) / 365 * days
            val principalPart = monthlyPay - interest

            val isLast = remaining <= principalPart
            val totalPay = if (isLast) remaining + interest else monthlyPay
            val nextRemaining = if (isLast) 0.0 else remaining - principalPart

            schedule += PaymentScheduleItem(
                monthNumber = month + 1,
                paymentDate = date,
                totalPayment = totalPay,
                principalPart = if (isLast) remaining else principalPart,
                interestPart = interest,
                remainingPrincipal = nextRemaining
            )

            if (isLast) return schedule
            remaining = nextRemaining
            date = date.plusMonths(1)
        }
        return schedule
    }

    private fun monthlyPayment(principal: Double, annualRate: Double, months: Int): Double {
        val mRate = annualRate / 100 / 12
        return if (mRate == 0.0) principal / months
        else principal * mRate * (1 + mRate).pow(months) / ((1 + mRate).pow(months) - 1)
    }

    private fun Double.pow(n: Int) = Math.pow(this, n.toDouble())
}
