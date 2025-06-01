package com.kreditnik.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoanViewModel(private val repository: LoanRepository) : ViewModel() {

    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> get() = _loans

    init {
        loadLoans()
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
}
