package com.kreditnik.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "operations")
data class Operation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,                     // Какому кредиту относится операция
    val amount: Double,                   // Сумма операции
    val date: LocalDateTime,              // Дата операции
    val type: OperationType,              // Тип операции (платеж, пополнение и т.д.)
    val description: String? = null       // Описание (опционально)
)
