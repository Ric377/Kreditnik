package com.kreditnik.app.data

class LoanRepository(
    private val loanDao: LoanDao,
    private val operationDao: OperationDao
) {

    suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(loan)
    }

    suspend fun insertOperation(operation: Operation) {
        operationDao.insertOperation(operation)
    }

    suspend fun getOperationsForLoan(loanId: Long): List<Operation> {
        return operationDao.getOperationsForLoan(loanId)
    }

    suspend fun getAllOperations(): List<Operation> {
        return operationDao.getAllOperations()
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

    suspend fun getLoanById(id: Long): Loan? {
        return loanDao.getLoanById(id)
    }

    suspend fun updateOperation(operation: Operation) {
        operationDao.updateOperation(operation)
    }

    suspend fun deleteOperation(operation: Operation) {
        operationDao.deleteOperation(operation)
    }
}
