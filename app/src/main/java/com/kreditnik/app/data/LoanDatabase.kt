package com.kreditnik.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Абстрактный класс базы данных приложения, построенный на Room.
 *
 * @property entities Список сущностей, которые будут преобразованы в таблицы.
 * @property version Версия схемы базы данных. Должна увеличиваться при изменении схемы.
 * @property exportSchema Флаг, отключающий экспорт схемы в JSON-файл.
 */
@Database(entities = [Loan::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LoanDatabase : RoomDatabase() {

    /**
     * Предоставляет доступ к Data Access Object (DAO) для работы с таблицей кредитов.
     */
    abstract fun loanDao(): LoanDao
}