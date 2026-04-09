package com.crosshyper.gidertakip.domain.repository

import com.crosshyper.gidertakip.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
}
