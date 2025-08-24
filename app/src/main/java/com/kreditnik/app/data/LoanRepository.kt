package com.kreditnik.app.data

/**
 * Репозиторий для работы с данными о кредитах.
 * Абстрагирует доступ к источнику данных (в данном случае, [LoanDao])
 * от остальной части приложения, следуя принципам чистой архитектуры.
 *
 * @property loanDao Объект доступа к данным для выполнения операций с базой данных.
 */
class LoanRepository(private val loanDao: LoanDao) {

    /**
     * Вставляет запись о кредите через DAO.
     */
    suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(loan)
    }

    /**
     * Обновляет запись о кредите через DAO.
     */
    suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loan)
    }

    /**
     * Удаляет запись о кредите через DAO.
     */
    suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(loan)
    }

    /**
     * Получает кредит по ID через DAO.
     */
    suspend fun getLoanById(id: Long): Loan? {
        return loanDao.getLoanById(id)
    }

    /**
     * Получает все кредиты через DAO.
     */
    suspend fun getAllLoans(): List<Loan> {
        return loanDao.getAllLoans()
    }
}