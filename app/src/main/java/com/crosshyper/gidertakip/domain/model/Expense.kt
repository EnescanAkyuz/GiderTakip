package com.crosshyper.gidertakip.domain.model

enum class TransactionType {
    ONE_TIME,
    INSTALLMENT,
    SUBSCRIPTION
}

data class Expense(
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val cardName: String,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val transactionType: TransactionType = TransactionType.ONE_TIME,
    val installmentCount: Int = 1,
    val installmentRemaining: Int = 0,
    val nextDueDate: Long? = null,
    val isRecurring: Boolean = false
)
