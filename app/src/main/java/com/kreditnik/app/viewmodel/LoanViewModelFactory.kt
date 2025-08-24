package com.kreditnik.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kreditnik.app.data.LoanRepository

/**
 * Фабрика для создания экземпляров [LoanViewModel].
 *
 * Необходима, поскольку [LoanViewModel] имеет конструктор с параметрами,
 * которые нужно передать при его создании (репозиторий и контекст).
 *
 * @property repository Репозиторий для доступа к данным.
 * @property appContext Контекст приложения.
 */
class LoanViewModelFactory(
    private val repository: LoanRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {

    /**
     * Создает новый экземпляр ViewModel.
     *
     * @param modelClass Класс ViewModel для создания.
     * @return Новый экземпляр ViewModel.
     * @throws IllegalArgumentException если передан неизвестный класс ViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoanViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}