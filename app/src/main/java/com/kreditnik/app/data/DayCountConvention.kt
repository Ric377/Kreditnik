package com.kreditnik.app.data

enum class DayCountConvention {
    SBER,    // Сбер-стиль: фактические дни, год = 365/366
    RETAIL   // Альфа/ВТБ/Совком-стиль: 30-дней в месяце, год = 360 дней
}
