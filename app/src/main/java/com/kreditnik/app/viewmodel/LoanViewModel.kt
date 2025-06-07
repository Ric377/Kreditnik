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

    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> get() = _loans

    init {
        loadLoans()
    }

    private val _operations = MutableStateFlow<List<Operation>>(emptyList())
    val operations: StateFlow<List<Operation>> get() = _operations

    fun loadOperations() {
        viewModelScope.launch {
            _operations.value = repository.getAllOperations()
        }
    }

    fun addOperation(operation: Operation) {
        viewModelScope.launch {
            repository.insertOperation(operation)
            loadOperations()
        }
    }

    private fun loadLoans() {
        viewModelScope.launch {
            _loans.value = repository.getAllLoans()
        }
    }

    fun addLoan(loan: Loan) {
        viewModelScope.launch {
            repository.insertLoan(loan)
            loadLoans()
        }
    }

    fun updateLoan(loan: Loan) {
        viewModelScope.launch {
            repository.updateLoan(loan)
            loadLoans()
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            loadLoans()
        }
    }

    fun updateLoanPrincipal(loanId: Long, delta: Double) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId)
            if (loan != null) {
                val updatedLoan = loan.copy(principal = loan.principal + delta)
                repository.updateLoan(updatedLoan)
                loadLoans() // чтобы обновился список кредитов
            }
        }
    }

    fun getLoanNameById(loanId: Long): String {
        return _loans.value.firstOrNull { it.id == loanId }?.name ?: "Неизвестный кредит"
    }

    fun calculatePaymentSchedule(loan: Loan): List<PaymentScheduleItem> {
        val schedule = mutableListOf<PaymentScheduleItem>()

        val principal = loan.principal
        val months = loan.months
        val annualRate = loan.interestRate

        val monthlyPayment = calculateMonthlyPayment(principal, annualRate, months)

        var remainingPrincipal = principal
        var paymentDate = loan.startDate.plusMonths(1)

        for (i in 1..months) {
            val daysInMonth = paymentDate.lengthOfMonth()

            val monthlyInterest = remainingPrincipal * (annualRate / 100) / 365 * daysInMonth
            val principalPayment = monthlyPayment - monthlyInterest

            if (remainingPrincipal < principalPayment) {
                // Последний платёж, если остался небольшой остаток
                schedule.add(
                    PaymentScheduleItem(
                        monthNumber = i,
                        paymentDate = paymentDate,
                        totalPayment = remainingPrincipal + monthlyInterest,
                        principalPart = remainingPrincipal,
                        interestPart = monthlyInterest,
                        remainingPrincipal = 0.0
                    )
                )
                break
            } else {
                schedule.add(
                    PaymentScheduleItem(
                        monthNumber = i,
                        paymentDate = paymentDate,
                        totalPayment = monthlyPayment,
                        principalPart = principalPayment,
                        interestPart = monthlyInterest,
                        remainingPrincipal = remainingPrincipal - principalPayment
                    )
                )
                remainingPrincipal -= principalPayment
            }

            paymentDate = paymentDate.plusMonths(1)
        }

        return schedule
    }

    private fun calculateMonthlyPayment(principal: Double, annualRate: Double, months: Int): Double {
        val monthlyRate = (annualRate / 100) / 12
        return if (monthlyRate == 0.0) {
            principal / months
        } else {
            principal * (monthlyRate * Math.pow(1 + monthlyRate, months.toDouble())) /
                    (Math.pow(1 + monthlyRate, months.toDouble()) - 1)
        }
    }
}
