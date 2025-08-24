package com.kreditnik.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreditnik.app.data.Loan
import com.kreditnik.app.data.LoanRepository
import com.kreditnik.app.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ViewModel для управления данными о кредитах.
 * Отвечает за взаимодействие с репозиторием, обработку бизнес-логики
 * и предоставление данных для UI-слоя.
 *
 * @property repository Репозиторий для доступа к данным о кредитах.
 * @property appContext Контекст приложения, необходимый для работы с уведомлениями.
 */
class LoanViewModel(
    private val repository: LoanRepository,
    private val appContext: Context
) : ViewModel() {

    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> get() = _loans

    init {
        loadLoans()
    }

    /**
     * Загружает все кредиты из репозитория, выполняет для каждого
     * актуализацию начисленных процентов и обновляет [StateFlow] для UI.
     */
    fun loadLoans() = viewModelScope.launch {
        val currentLoans = repository.getAllLoans()
        val updatedLoansForUi = mutableListOf<Loan>()

        for (loan in currentLoans) {
            val loanAfterAccrual = calculateAndAccrueInterestAndSave(loan)
            updatedLoansForUi.add(loanAfterAccrual)
        }
        _loans.value = updatedLoansForUi
    }

    /**
     * Добавляет новый кредит в базу данных и планирует напоминание, если необходимо.
     */
    suspend fun addLoan(loan: Loan) {
        repository.insertLoan(loan)
        if (loan.reminderEnabled) {
            NotificationHelper.scheduleLoanReminder(appContext, loan)
        }
        loadLoans()
    }

    /**
     * Обновляет существующий кредит и перепланирует напоминание.
     */
    fun updateLoan(loan: Loan) = viewModelScope.launch {
        repository.updateLoan(loan)
        NotificationHelper.cancelLoanReminder(appContext, loan)
        if (loan.reminderEnabled) {
            NotificationHelper.scheduleLoanReminder(appContext, loan)
        }
        loadLoans()
    }

    /**
     * Удаляет кредит из базы данных.
     */
    fun deleteLoan(loan: Loan) = viewModelScope.launch {
        repository.deleteLoan(loan)
        loadLoans()
    }

    /**
     * Обрабатывает финансовую операцию (платеж или увеличение долга).
     * Сначала погашаются начисленные проценты, затем остаток суммы идет
     * на погашение основного долга.
     *
     * @param loanId ID кредита для обновления.
     * @param delta Сумма операции. Отрицательное значение для платежа,
     * положительное для увеличения основного долга.
     */
    fun updateLoanPrincipal(loanId: Long, delta: Double) = viewModelScope.launch {
        repository.getLoanById(loanId)?.let { currentLoanFromDb ->
            val loanWithAccruedInterest = calculateAndAccrueInterestAndSave(currentLoanFromDb)

            var newPrincipal = loanWithAccruedInterest.principal
            var newAccruedInterest = loanWithAccruedInterest.accruedInterest

            if (delta < 0) { // Платеж
                var paymentAmount = -delta

                if (newAccruedInterest > 0) {
                    val interestPaid = minOf(paymentAmount, newAccruedInterest)
                    newAccruedInterest -= interestPaid
                    paymentAmount -= interestPaid
                }

                if (paymentAmount > 0) {
                    val principalPaid = minOf(paymentAmount, newPrincipal)
                    newPrincipal -= principalPaid
                }

            } else { // Увеличение долга
                newPrincipal += delta
            }

            repository.updateLoan(
                currentLoanFromDb.copy(
                    principal = newPrincipal,
                    accruedInterest = newAccruedInterest,
                    lastInterestCalculationDate = LocalDate.now()
                )
            )

            loadLoans()
        }
    }

    /**
     * Рассчитывает и начисляет ежедневные проценты, а затем сохраняет
     * обновленный кредит в базу данных.
     *
     * Эта функция корректно обрабатывает два типа расчета: стандартный и
     * "по методу Сбера", а также учитывает високосные годы для точного
     * определения дневной процентной ставки.
     *
     * @param loan Кредит, для которого нужно произвести расчет.
     * @return Обновленный объект Loan с актуальными процентами и датой расчета.
     */
    private suspend fun calculateAndAccrueInterestAndSave(loan: Loan): Loan {
        val today = LocalDate.now()

        if (loan.lastInterestCalculationDate.isAfter(today)) {
            // Обработка перевода системного времени назад
            val daysToGoBack = ChronoUnit.DAYS.between(today, loan.lastInterestCalculationDate)
            var interestToDeduct = 0.0
            for (i in 0 until daysToGoBack) {
                val date = today.plusDays(i)
                val daysInYear = if (date.isLeapYear) 366.0 else 365.0
                val dailyRate = loan.interestRate / 100.0 / daysInYear
                interestToDeduct += loan.principal * dailyRate
            }
            val newAccruedInterest = (loan.accruedInterest - interestToDeduct).coerceAtLeast(0.0)
            val updatedLoan = loan.copy(
                accruedInterest = newAccruedInterest,
                lastInterestCalculationDate = today
            )
            repository.updateLoan(updatedLoan)
            return updatedLoan

        } else if (loan.lastInterestCalculationDate.isBefore(today)) {
            // Начисление процентов за прошедшие дни
            val endDate = if (loan.usesSberbankCalculation) {
                today.minusDays(1) // Метод Сбера: расчет по вчерашний день включительно
            } else {
                today // Стандартный метод: расчет по сегодняшний день включительно
            }

            val daysBetween = ChronoUnit.DAYS.between(loan.lastInterestCalculationDate, endDate)

            if (daysBetween >= 0) {
                var interestForPeriod = 0.0
                for (i in 0..daysBetween) {
                    val currentDate = loan.lastInterestCalculationDate.plusDays(i)
                    val daysInYear = if (currentDate.isLeapYear) 366.0 else 365.0
                    val dailyRate = loan.interestRate / 100.0 / daysInYear
                    interestForPeriod += loan.principal * dailyRate
                }

                val updatedLoan = loan.copy(
                    accruedInterest = loan.accruedInterest + interestForPeriod,
                    lastInterestCalculationDate = today
                )
                repository.updateLoan(updatedLoan)
                return updatedLoan
            }
        }
        // Если дата расчета актуальна, возвращаем исходный объект
        return loan
    }
}
