package com.kreditnik.app.data

import androidx.room.*

/**
 * Data Access Object для сущности [Loan].
 * Определяет методы для взаимодействия с таблицей "loans" в базе данных.
 */
@Dao
interface LoanDao {

    /**
     * Вставляет или заменяет запись о кредите в базе данных.
     * @param loan Объект для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)

    /**
     * Обновляет существующую запись о кредите.
     * @param loan Объект с обновленными данными.
     */
    @Update
    suspend fun updateLoan(loan: Loan)

    /**
     * Удаляет запись о кредите из базы данных.
     * @param loan Объект для удаления.
     */
    @Delete
    suspend fun deleteLoan(loan: Loan)

    /**
     * Возвращает список всех кредитов, отсортированных по дате начала (от новых к старым).
     * @return Список объектов [Loan].
     */
    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    suspend fun getAllLoans(): List<Loan>

    /**
     * Находит и возвращает кредит по его уникальному идентификатору.
     * @param id Уникальный идентификатор кредита.
     * @return Объект [Loan] или null, если кредит не найден.
     */
    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): Loan?
}