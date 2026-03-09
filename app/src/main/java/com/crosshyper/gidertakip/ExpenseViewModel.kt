package com.crosshyper.gidertakip

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crosshyper.gidertakip.data.Expense
import com.crosshyper.gidertakip.data.ExpenseRepository
import com.crosshyper.gidertakip.widget.ExpenseWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class ViewMode { DAY, WEEK, MONTH }

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.DAY)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        _selectedDate, _viewMode, _searchQuery
    ) { date, mode, query ->
        Triple(date, mode, query)
    }.flatMapLatest { (date, mode, query) ->
        if (query.isNotEmpty()) {
            repository.searchExpenses(query)
        } else {
            val range = getRangeForMode(date, mode)
            repository.getExpensesByDate(range.first, range.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAmount: StateFlow<Double?> = combine(
        _selectedDate, _viewMode
    ) { date, mode ->
        Pair(date, mode)
    }.flatMapLatest { (date, mode) ->
        val range = getRangeForMode(date, mode)
        repository.getTotalExpenseByDate(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setDate(date: Long) { _selectedDate.value = date }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun addExpense(title: String, amount: Double, category: String = "Genel", date: Long = _selectedDate.value, note: String? = null, isRecurring: Boolean = false) {
        viewModelScope.launch {
            repository.insert(Expense(title = title, amount = amount, category = category, date = date, note = note, isRecurring = isRecurring))
            ExpenseWidget().updateAll(getApplication())
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.delete(expense)
            ExpenseWidget().updateAll(getApplication())
        }
    }

    fun processSmartInput(input: String) {
        val parts = input.trim().split(" ")
        if (parts.isEmpty()) return
        val amount = parts.last().replace(",", ".").toDoubleOrNull() ?: parts.first().replace(",", ".").toDoubleOrNull()
        val title = if (parts.last().replace(",", ".").toDoubleOrNull() != null) parts.dropLast(1).joinToString(" ") else parts.drop(1).joinToString(" ")
        if (amount != null) addExpense(title.ifEmpty { "Gider" }, amount)
    }

    private fun getRangeForMode(date: Long, mode: ViewMode): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        return when (mode) {
            ViewMode.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); Pair(start, cal.timeInMillis)
            }
            ViewMode.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek); cal.set(Calendar.HOUR_OF_DAY, 0); val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6); cal.set(Calendar.HOUR_OF_DAY, 23); Pair(start, cal.timeInMillis)
            }
            ViewMode.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); cal.set(Calendar.HOUR_OF_DAY, 23); Pair(start, cal.timeInMillis)
            }
        }
    }
}

class ExpenseViewModelFactory(private val application: Application, private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ExpenseViewModel(application, repository) as T
    }
}
