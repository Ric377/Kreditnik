// LoanViewModel.kt
package com.kreditnik.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import android.util.Log
import android.content.Context
import com.kreditnik.app.util.NotificationHelper


class LoanViewModel(
    private val repository: LoanRepository,
    private val appContext: Context
) : ViewModel() {


    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> get() = _loans

    init {
        loadLoans()
    }

    fun loadLoans() = viewModelScope.launch {
        val currentLoans = repository.getAllLoans()
        val updatedLoansForUi = mutableListOf<Loan>() // Список для обновления UI

        for (loan in currentLoans) {
            // !!! ИСПРАВЛЕНИЕ ЗДЕСЬ !!!
            // Вызываем функцию, которая рассчитает проценты И СОХРАНИТ их в БД.
            val loanAfterAccrual = calculateAndAccrueInterestAndSave(loan)
            updatedLoansForUi.add(loanAfterAccrual)
        }
        _loans.value = updatedLoansForUi // Обновляем UI после всех пересчетов и сохранений
    }

    suspend fun addLoan(loan: Loan) {
        repository.insertLoan(loan)
        loadLoans()
    }

    fun updateLoan(loan: Loan) = viewModelScope.launch {
        repository.updateLoan(loan)
        loadLoans()

        if (loan.reminderEnabled) {
            NotificationHelper.scheduleLoanReminder(appContext, loan)
        }
    }



    fun deleteLoan(loan: Loan) = viewModelScope.launch {
        repository.deleteLoan(loan)
        loadLoans()
    }

    // Эта функция обновляет как principal (тело кредита), так и accruedInterest
    fun updateLoanPrincipal(loanId: Long, delta: Double) = viewModelScope.launch {
        repository.getLoanById(loanId)?.let { currentLoanFromDb ->
            // --- ШАГ 1: Актуализация процентов до применения транзакции ---
            // Сначала убедимся, что проценты для этого кредита актуальны на текущий день
            // Эта функция возвращает новую копию Loan с актуальными процентами и датой начисления
            // !!! ИСПРАВЛЕНИЕ ЗДЕСЬ !!!
            // Используем calculateAndAccrueInterestAndSave, чтобы проценты были актуальны и СОХРАНЕНЫ перед платежом
            val loanWithAccruedInterest = calculateAndAccrueInterestAndSave(currentLoanFromDb)

            // Переменные для новых значений principal и accruedInterest
            var newPrincipal = loanWithAccruedInterest.principal // Текущий остаток основного долга (тело кредита)
            var newAccruedInterest = loanWithAccruedInterest.accruedInterest
            var amountToProcess = delta // Сумма, которую мы применяем: >0 для добавления, <0 для платежа

            Log.d("LoanViewModel", "--- Начало операции (ID: ${currentLoanFromDb.id}) ---")
            Log.d("LoanViewModel", "Старый Principal (тело кредита): ${currentLoanFromDb.principal}, Старые Проценты: ${currentLoanFromDb.accruedInterest}")
            Log.d("LoanViewModel", "Principal после начисления: $newPrincipal, Проценты после начисления: $newAccruedInterest")
            Log.d("LoanViewModel", "Сумма операции (delta): $delta")

            if (amountToProcess < 0) { // Это платеж (delta отрицательная)
                var paymentAmount = -amountToProcess // Преобразуем в положительное значение платежа

                Log.d("LoanViewModel", "Обработка платежа. Сумма платежа: $paymentAmount")

                // --- ШАГ 2: Погашение начисленных процентов ---
                if (newAccruedInterest > 0) {
                    val interestPaid = minOf(paymentAmount, newAccruedInterest)
                    newAccruedInterest -= interestPaid
                    paymentAmount -= interestPaid // Уменьшаем остаток платежа
                    Log.d("LoanViewModel", "   > Погашено процентов: $interestPaid. Новые Проценты: $newAccruedInterest. Остаток платежа: $paymentAmount")
                } else {
                    Log.d("LoanViewModel", "   > Начисленных процентов нет или 0.")
                }

                // --- ШАГ 3: Погашение тела кредита (principal) ---
                if (paymentAmount > 0) { // Если после погашения процентов еще остались деньги
                    val principalPaid = minOf(paymentAmount, newPrincipal) // Нельзя погасить больше, чем текущий principal
                    newPrincipal -= principalPaid // Уменьшаем ТЕЛО КРЕДИТА (principal)
                    paymentAmount -= principalPaid // Уменьшаем остаток платежа (может быть 0)
                    Log.d("LoanViewModel", "   > Погашено тела кредита: $principalPaid. Новый Principal (тело кредита): $newPrincipal. Остаток платежа: $paymentAmount")
                } else {
                    Log.d("LoanViewModel", "   > Недостаточно средств для погашения тела кредита.")
                }

            } else { // Это добавление долга (delta положительная)
                newPrincipal += amountToProcess // Добавляем сумму напрямую к основному долгу (телу кредита)
                Log.d("LoanViewModel", "   > Добавлено к телу долга: $amountToProcess. Новый Principal (тело кредита): $newPrincipal")
            }

            // --- ШАГ 4: Сохранение обновленного кредита в базе данных ---
            // Важно: initialPrincipal не меняется. Мы обновляем только principal и accruedInterest.
            repository.updateLoan(currentLoanFromDb.copy( // Используем .copy от исходного объекта из БД
                principal = newPrincipal, // Обновляем текущий остаток основного долга
                accruedInterest = newAccruedInterest, // Обновляем начисленные проценты
                lastInterestCalculationDate = LocalDate.now() // Обновляем дату последнего начисления после транзакции
            ))
            Log.d("LoanViewModel", "--- Операция завершена. Сохраненный Principal: $newPrincipal, Сохраненные Проценты: $newAccruedInterest ---")

            loadLoans() // Перезагружаем все кредиты, чтобы UI обновился с новыми данными
        } ?: run {
            Log.e("LoanViewModel", "Кредит с ID $loanId не найден для обновления principal.")
        }
    }

    /**
     * Приватная функция для расчета и начисления ежедневных процентов,
     * а также сохранения обновленного кредита в базу данных.
     * Возвращает НОВЫЙ объект Loan с актуализированными accruedInterest и lastInterestCalculationDate.
     * СОХРАНЯЕТ В БАЗУ ДАННЫХ.
     */
    private suspend fun calculateAndAccrueInterestAndSave(loan: Loan): Loan {
        val today = LocalDate.now()
        var updatedLoan = loan

        // Случай 1: Дата была переведена НАЗАД
        if (loan.lastInterestCalculationDate.isAfter(today)) {
            val daysToGoBack = ChronoUnit.DAYS.between(today, loan.lastInterestCalculationDate).toInt()
            if (daysToGoBack > 0) {
                val dailyRate = loan.interestRate / 100.0 / 365.0
                val interestToDeduct = loan.principal * dailyRate * daysToGoBack

                val newAccruedInterest = (loan.accruedInterest - interestToDeduct).coerceAtLeast(0.0) // Проценты не могут быть отрицательными

                updatedLoan = loan.copy(
                    accruedInterest = newAccruedInterest,
                    lastInterestCalculationDate = today
                )
                repository.updateLoan(updatedLoan)
                Log.d("LoanViewModel", "DEBUG: Отмена процентов для ID: ${loan.id}, Дней: $daysToGoBack, Сумма отмены: $interestToDeduct. Новые проценты: ${updatedLoan.accruedInterest}")
            }
        }
        // Случай 2: Дата была переведена ВПЕРЕД или наступил новый день
        else if (loan.lastInterestCalculationDate.isBefore(today)) {
            val daysToAccrue = ChronoUnit.DAYS.between(loan.lastInterestCalculationDate, today).toInt()
            if (daysToAccrue > 0) {
                val dailyRate = loan.interestRate / 100.0 / 365.0
                val interestForPeriod = loan.principal * dailyRate * daysToAccrue

                updatedLoan = loan.copy(
                    accruedInterest = loan.accruedInterest + interestForPeriod,
                    lastInterestCalculationDate = today
                )
                repository.updateLoan(updatedLoan)
                Log.d("LoanViewModel", "DEBUG: Начисление процентов для ID: ${loan.id}, Дней: $daysToAccrue, Сумма процентов: $interestForPeriod. Новые проценты: ${updatedLoan.accruedInterest}")
            }
        }
        // Случай 3: lastInterestCalculationDate равна today (ничего не делать)
        return updatedLoan
    }
}