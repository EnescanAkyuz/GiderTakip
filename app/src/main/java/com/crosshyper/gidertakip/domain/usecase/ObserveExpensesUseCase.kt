package com.crosshyper.gidertakip.domain.usecase

import com.crosshyper.gidertakip.domain.model.Expense
import com.crosshyper.gidertakip.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class ObserveExpensesUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<Expense>> {
        return repository.observeExpenses()
    }
}
