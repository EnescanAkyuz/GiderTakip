package com.crosshyper.gidertakip.data.mapper

import com.crosshyper.gidertakip.data.local.entity.ExpenseEntity
import com.crosshyper.gidertakip.domain.model.Expense
import com.crosshyper.gidertakip.domain.model.TransactionType

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        title = title,
        amount = amount,
        category = category,
        cardName = cardName,
        date = date,
        note = note,
        transactionType = runCatching { TransactionType.valueOf(transactionType) }
            .getOrDefault(TransactionType.ONE_TIME),
        installmentCount = installmentCount,
        installmentRemaining = installmentRemaining,
        nextDueDate = nextDueDate,
        isRecurring = isRecurring
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        title = title,
        amount = amount,
        category = category,
        cardName = cardName,
        date = date,
        note = note,
        transactionType = transactionType.name,
        installmentCount = installmentCount,
        installmentRemaining = installmentRemaining,
        nextDueDate = nextDueDate,
        isRecurring = isRecurring
    )
}
