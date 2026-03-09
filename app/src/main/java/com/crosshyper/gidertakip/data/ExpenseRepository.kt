package com.crosshyper.gidertakip.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val totalExpense: Flow<Double?> = expenseDao.getTotalExpense()

    fun getExpensesByDate(start: Long, end: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesInRange(start, end)
    }

    fun getTotalExpenseByDate(start: Long, end: Long): Flow<Double?> {
        return expenseDao.getTotalInRange(start, end)
    }

    fun searchExpenses(query: String): Flow<List<Expense>> {
        return expenseDao.searchExpenses(query)
    }

    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun delete(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }
}
