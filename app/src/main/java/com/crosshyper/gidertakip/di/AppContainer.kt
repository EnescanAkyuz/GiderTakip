package com.crosshyper.gidertakip.di

import android.app.Application
import com.crosshyper.gidertakip.data.local.database.AppDatabase
import com.crosshyper.gidertakip.data.repository.ExpenseRepositoryImpl
import com.crosshyper.gidertakip.domain.usecase.AddExpenseUseCase
import com.crosshyper.gidertakip.domain.usecase.DeleteExpenseUseCase
import com.crosshyper.gidertakip.domain.usecase.ObserveExpensesUseCase
import com.crosshyper.gidertakip.presentation.home.HomeViewModelFactory

class AppContainer(application: Application) {
    private val database = AppDatabase.getDatabase(application.applicationContext)
    private val expenseRepository = ExpenseRepositoryImpl(database.expenseDao())
    private val observeExpensesUseCase = ObserveExpensesUseCase(expenseRepository)
    private val addExpenseUseCase = AddExpenseUseCase(expenseRepository)
    private val deleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository)

    fun provideHomeViewModelFactory(application: Application): HomeViewModelFactory {
        return HomeViewModelFactory(
            application = application,
            observeExpensesUseCase = observeExpensesUseCase,
            addExpenseUseCase = addExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase
        )
    }
}