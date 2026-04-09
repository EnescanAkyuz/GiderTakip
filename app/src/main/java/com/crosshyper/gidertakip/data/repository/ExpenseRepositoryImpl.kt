package com.crosshyper.gidertakip.data.repository

import com.crosshyper.gidertakip.data.local.dao.ExpenseDao
import com.crosshyper.gidertakip.data.mapper.toDomain
import com.crosshyper.gidertakip.data.mapper.toEntity
import com.crosshyper.gidertakip.domain.model.Expense
import com.crosshyper.gidertakip.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun observeExpenses(): Flow<List<Expense>> {
        return expenseDao.observeAllExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense.toEntity())
    }
}
