package com.kreditnik.app.data

import androidx.room.*

@Dao
interface OperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: Operation)

    @Query("SELECT * FROM operations WHERE loanId = :loanId ORDER BY date DESC")
    suspend fun getOperationsForLoan(loanId: Long): List<Operation>

    @Query("SELECT * FROM operations ORDER BY date DESC")
    suspend fun getAllOperations(): List<Operation>

    @Update
    suspend fun updateOperation(operation: Operation)

    @Delete
    suspend fun deleteOperation(operation: Operation)

}
