package com.kreditnik.app.data

class LoanRepository(private val loanDao: LoanDao) {

    suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(loan)
    }

    suspend fun getAllLoans(): List<Loan> {
        return loanDao.getAllLoans()
    }

    suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(loan)
    }
}
