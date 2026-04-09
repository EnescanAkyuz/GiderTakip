package com.crosshyper.gidertakip.domain.usecase

import com.crosshyper.gidertakip.domain.model.Expense
import com.crosshyper.gidertakip.domain.repository.ExpenseRepository

class DeleteExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense) {
        repository.deleteExpense(expense)
    }
}
