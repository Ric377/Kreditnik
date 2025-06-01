package com.kreditnik.app.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let {
            LocalDate.parse(it, formatter)
        }
    }

    @TypeConverter
    fun fromLoanType(type: LoanType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toLoanType(name: String?): LoanType? {
        return name?.let {
            LoanType.valueOf(it)
        }
    }
}
