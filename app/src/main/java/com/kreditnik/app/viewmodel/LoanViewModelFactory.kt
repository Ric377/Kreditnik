package com.kreditnik.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kreditnik.app.data.LoanRepository

class LoanViewModelFactory(private val repository: LoanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
