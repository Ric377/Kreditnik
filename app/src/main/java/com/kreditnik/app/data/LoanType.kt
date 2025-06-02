package com.kreditnik.app.data

enum class LoanType(val displayName: String) {
    CREDIT("Кредит"),
    CARD("Кредитная карта"),
    INSTALLMENT("Рассрочка"),
    DEBT("Долг")
}
