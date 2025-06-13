package com.kreditnik.app.data

class LoanRepository(
    private val loanDao: LoanDao,
    private val operationDao: OperationDao
) {

    suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(loan)
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

    suspend fun getAllLoans(): List<Loan> {
        return loanDao.getAllLoans()
    }

    suspend fun insertOperation(operation: Operation) {
        operationDao.insertOperation(operation)
    }

    suspend fun getOperationsForLoan(loanId: Long): List<Operation> {
        return operationDao.getOperationsForLoan(loanId)
    }

    suspend fun updateOperation(operation: Operation) {
        operationDao.updateOperation(operation)
    }

    suspend fun deleteOperation(operation: Operation) {
        operationDao.deleteOperation(operation)
    }

    suspend fun recalculateLoanAfterOperationUpdate(operation: Operation) {
        val loan = loanDao.getLoanById(operation.loanId) ?: return
        val allOperations = operationDao.getOperationsForLoan(loan.id)
        val newPrincipal = allOperations.sumOf { it.amount }
        loanDao.updateLoan(loan.copy(principal = newPrincipal))
    }
}
