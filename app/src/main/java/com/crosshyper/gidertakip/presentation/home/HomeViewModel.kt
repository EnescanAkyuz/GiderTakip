package com.crosshyper.gidertakip.presentation.home

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crosshyper.gidertakip.domain.model.*
import com.crosshyper.gidertakip.domain.usecase.AddExpenseUseCase
import com.crosshyper.gidertakip.domain.usecase.DeleteExpenseUseCase
import com.crosshyper.gidertakip.domain.usecase.ObserveExpensesUseCase
import com.crosshyper.gidertakip.widget.ExpenseWidget
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

private data class FilterSnapshot(
    val query: String,
    val category: String?,
    val card: String?,
    val dateFilter: DateFilter,
    val budgets: Map<String, Double>
)

class HomeViewModel(
    application: Application,
    observeExpensesUseCase: ObserveExpensesUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : AndroidViewModel(application) {

    private val budgetPrefs =
        application.getSharedPreferences("finance_budgets", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedCard = MutableStateFlow<String?>(null)
    val selectedCard: StateFlow<String?> = _selectedCard.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow(DateFilter.THIS_MONTH)
    val selectedDateFilter: StateFlow<DateFilter> = _selectedDateFilter.asStateFlow()

    private val _budgetLimits = MutableStateFlow(loadBudgetLimits())
    val budgetLimits: StateFlow<Map<String, Double>> = _budgetLimits.asStateFlow()

    private val allExpenses = observeExpensesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val availableCategories: StateFlow<List<String>> = allExpenses
        .map { expenses ->
            (FinanceCatalog.categories.map { it.name } + expenses.map { it.category })
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceCatalog.categories.map { it.name })

    val availableCards: StateFlow<List<String>> = allExpenses
        .map { expenses ->
            (FinanceCatalog.cards.map { it.name } + expenses.map { it.cardName })
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceCatalog.cards.map { it.name })

    private val filterSnapshot = combine(
        _searchQuery,
        _selectedCategory,
        _selectedCard,
        _selectedDateFilter,
        _budgetLimits
    ) { query, category, card, dateFilter, budgets ->
        FilterSnapshot(query, category, card, dateFilter, budgets)
    }

    val uiState: StateFlow<FinanceHomeState> = combine(
        allExpenses,
        filterSnapshot
    ) { expenses, filters ->
        buildHomeState(
            expenses = expenses,
            searchQuery = filters.query,
            selectedCategory = filters.category,
            selectedCard = filters.card,
            dateFilter = filters.dateFilter,
            budgets = filters.budgets
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceHomeState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setCardFilter(card: String?) {
        _selectedCard.value = card
    }

    fun setDateFilter(filter: DateFilter) {
        _selectedDateFilter.value = filter
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedCard.value = null
        _selectedDateFilter.value = DateFilter.THIS_MONTH
    }

    fun updateBudgetLimit(category: String, limit: Double) {
        val sanitized = limit.coerceAtLeast(0.0)
        _budgetLimits.update { it + (category to sanitized) }
        budgetPrefs.edit().putFloat(category, sanitized.toFloat()).apply()
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        cardName: String,
        date: Long,
        note: String?,
        transactionType: TransactionType,
        installmentCount: Int,
        installmentRemaining: Int,
        nextDueDate: Long?
    ) {
        if (title.isBlank() || amount <= 0.0) return

        viewModelScope.launch {
            addExpenseUseCase(
                Expense(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    cardName = cardName,
                    date = date,
                    note = note?.trim()?.takeIf { it.isNotBlank() },
                    transactionType = transactionType,
                    installmentCount = installmentCount.coerceAtLeast(1),
                    installmentRemaining = installmentRemaining.coerceAtLeast(0),
                    nextDueDate = nextDueDate,
                    isRecurring = transactionType == TransactionType.SUBSCRIPTION
                )
            )
            ExpenseWidget().updateAll(getApplication())
        }
    }

    fun processQuickInput(input: String) {
        val tokens = input.trim().split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return

        val amountToken = tokens.lastOrNull()?.replace(",", ".")?.toDoubleOrNull()
            ?: tokens.firstOrNull()?.replace(",", ".")?.toDoubleOrNull()
            ?: return

        val titleTokens = if (tokens.last().replace(",", ".").toDoubleOrNull() != null) {
            tokens.dropLast(1)
        } else {
            tokens.drop(1)
        }

        addExpense(
            title = titleTokens.joinToString(" ").ifBlank { "Hizli islem" },
            amount = amountToken,
            category = "Diger",
            cardName = "Nakit",
            date = System.currentTimeMillis(),
            note = null,
            transactionType = TransactionType.ONE_TIME,
            installmentCount = 1,
            installmentRemaining = 0,
            nextDueDate = null
        )
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            deleteExpenseUseCase(expense)
            ExpenseWidget().updateAll(getApplication())
        }
    }

    private fun loadBudgetLimits(): Map<String, Double> {
        val defaults = FinanceCatalog.defaultBudgetLimits().toMutableMap()
        defaults.keys.forEach { category ->
            if (budgetPrefs.contains(category)) {
                defaults[category] = budgetPrefs.getFloat(category, defaults.getValue(category).toFloat()).toDouble()
            }
        }
        return defaults
    }

    private fun buildHomeState(
        expenses: List<Expense>,
        searchQuery: String,
        selectedCategory: String?,
        selectedCard: String?,
        dateFilter: DateFilter,
        budgets: Map<String, Double>
    ): FinanceHomeState {
        val now = System.currentTimeMillis()
        val sortedExpenses = expenses.sortedByDescending { it.date }
        val monthExpenses = sortedExpenses.filter { isInCurrentMonth(it.date, now) }
        val weekExpenses = sortedExpenses.filter { isInCurrentWeek(it.date, now) }
        val todayExpenses = sortedExpenses.filter { isToday(it.date, now) }
        val previousMonthExpenses = sortedExpenses.filter { isInPreviousMonth(it.date, now) }

        val monthTotal = monthExpenses.sumOf { it.amount }
        val weekTotal = weekExpenses.sumOf { it.amount }
        val todayTotal = todayExpenses.sumOf { it.amount }
        val previousMonthTotal = previousMonthExpenses.sumOf { it.amount }

        val categoryBreakdown = buildCategoryBreakdown(monthExpenses, monthTotal)
        val topCategory = categoryBreakdown.firstOrNull()
        val cardSummaries = buildCardSummaries(sortedExpenses, monthExpenses, now)
        val upcomingPayments = buildUpcomingPayments(sortedExpenses, cardSummaries, now)
        val installmentItems = buildInstallmentItems(sortedExpenses)
        val subscriptionItems = buildSubscriptionItems(sortedExpenses)
        val budgetStatuses = buildBudgetStatuses(monthExpenses, budgets)
        val filteredTransactions = filterTransactions(
            sortedExpenses,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            selectedCard = selectedCard,
            dateFilter = dateFilter,
            now = now
        )
        val groupedTransactions = groupTransactions(filteredTransactions)
        val monthSummary = buildMonthSummary(
            currentMonthTotal = monthTotal,
            previousMonthTotal = previousMonthTotal,
            topCategory = topCategory?.category ?: "Henuz veri yok"
        )
        val calendarMonthMillis = startOfMonth(now)
        val calendarDays = buildCalendarDays(
            monthExpenses = monthExpenses,
            upcomingPayments = upcomingPayments,
            now = now
        )

        return FinanceHomeState(
            monthTotal = monthTotal,
            weekTotal = weekTotal,
            todayTotal = todayTotal,
            topCategory = topCategory,
            upcomingPayments = upcomingPayments,
            recentTransactions = sortedExpenses.take(5),
            filteredTransactions = filteredTransactions,
            groupedTransactions = groupedTransactions,
            categoryBreakdown = categoryBreakdown,
            cardSummaries = cardSummaries,
            installmentItems = installmentItems,
            installmentMonthTotal = installmentItems
                .filter { it.nextDueDate?.let { due -> isInCurrentMonth(due, now) } == true }
                .sumOf { it.amount },
            subscriptionItems = subscriptionItems,
            subscriptionMonthTotal = subscriptionItems
                .filter { it.nextChargeDate?.let { due -> isInCurrentMonth(due, now) } == true }
                .sumOf { it.amount },
            budgetStatuses = budgetStatuses,
            monthSummary = monthSummary,
            calendarMonthMillis = calendarMonthMillis,
            calendarStartOffset = calculateMonthStartOffset(calendarMonthMillis),
            daysInMonth = getDaysInMonth(calendarMonthMillis),
            calendarDays = calendarDays,
            alerts = buildAlerts(budgetStatuses, upcomingPayments, monthSummary)
        )
    }

    private fun buildCategoryBreakdown(
        expenses: List<Expense>,
        monthTotal: Double
    ): List<CategoryBreakdownItem> {
        return expenses
            .groupBy { it.category }
            .map { (category, items) ->
                val total = items.sumOf { it.amount }
                CategoryBreakdownItem(
                    category = category,
                    total = total,
                    ratio = if (monthTotal > 0.0) (total / monthTotal).toFloat() else 0f
                )
            }
            .sortedByDescending { it.total }
    }

    private fun buildCardSummaries(
        allExpenses: List<Expense>,
        monthExpenses: List<Expense>,
        now: Long
    ): List<CardSpendingSummary> {
        val cardNames = (FinanceCatalog.cards.map { it.name } + allExpenses.map { it.cardName })
            .distinct()
            .sorted()

        return cardNames.map { cardName ->
            val monthSpend = monthExpenses.filter { it.cardName == cardName }.sumOf { it.amount }
            val totalSpend = allExpenses.filter { it.cardName == cardName }.sumOf { it.amount }
            val dueDay = FinanceCatalog.dueDayForCard(cardName)
            CardSpendingSummary(
                cardName = cardName,
                monthSpend = monthSpend,
                totalSpend = totalSpend,
                nextDueDate = dueDay?.let { nextOccurrenceOfDay(now, it) },
                dueDayOfMonth = dueDay
            )
        }.sortedByDescending { it.monthSpend }
    }

    private fun buildUpcomingPayments(
        expenses: List<Expense>,
        cardSummaries: List<CardSpendingSummary>,
        now: Long
    ): List<UpcomingPayment> {
        val expensePayments = expenses.mapNotNull { expense ->
            val dueDate = expense.nextDueDate ?: return@mapNotNull null
            if (dueDate < startOfDay(now)) return@mapNotNull null

            when (expense.transactionType) {
                TransactionType.INSTALLMENT -> UpcomingPayment(
                    title = expense.title,
                    amount = expense.amount,
                    dueDate = dueDate,
                    type = UpcomingPaymentType.INSTALLMENT,
                    cardName = expense.cardName,
                    subtitle = "${expense.installmentRemaining} taksit kaldi"
                )

                TransactionType.SUBSCRIPTION -> UpcomingPayment(
                    title = expense.title,
                    amount = expense.amount,
                    dueDate = dueDate,
                    type = UpcomingPaymentType.SUBSCRIPTION,
                    cardName = expense.cardName,
                    subtitle = "Abonelik cekimi"
                )

                TransactionType.ONE_TIME -> null
            }
        }

        val cardBills = cardSummaries.mapNotNull { summary ->
            val dueDate = summary.nextDueDate ?: return@mapNotNull null
            if (summary.monthSpend <= 0.0) return@mapNotNull null
            UpcomingPayment(
                title = "${summary.cardName} ekstresi",
                amount = summary.monthSpend,
                dueDate = dueDate,
                type = UpcomingPaymentType.CARD_BILL,
                cardName = summary.cardName,
                subtitle = "Son odeme gunu ${summary.dueDayOfMonth}"
            )
        }

        return (expensePayments + cardBills)
            .filter { it.dueDate <= now + 1000L * 60 * 60 * 24 * 45 }
            .sortedBy { it.dueDate }
            .take(8)
    }

    private fun buildInstallmentItems(expenses: List<Expense>): List<InstallmentTrackingItem> {
        return expenses
            .filter { it.transactionType == TransactionType.INSTALLMENT }
            .map {
                InstallmentTrackingItem(
                    id = it.id,
                    title = it.title,
                    amount = it.amount,
                    cardName = it.cardName,
                    remainingInstallments = it.installmentRemaining,
                    totalInstallments = it.installmentCount,
                    nextDueDate = it.nextDueDate
                )
            }
            .sortedBy { it.nextDueDate ?: Long.MAX_VALUE }
    }

    private fun buildSubscriptionItems(expenses: List<Expense>): List<SubscriptionTrackingItem> {
        return expenses
            .filter { it.transactionType == TransactionType.SUBSCRIPTION }
            .map {
                SubscriptionTrackingItem(
                    id = it.id,
                    title = it.title,
                    amount = it.amount,
                    cardName = it.cardName,
                    nextChargeDate = it.nextDueDate
                )
            }
            .sortedBy { it.nextChargeDate ?: Long.MAX_VALUE }
    }

    private fun buildBudgetStatuses(
        monthExpenses: List<Expense>,
        budgets: Map<String, Double>
    ): List<BudgetStatus> {
        return budgets.map { (category, limit) ->
            val spent = monthExpenses.filter { it.category == category }.sumOf { it.amount }
            val ratio = if (limit > 0.0) (spent / limit).toFloat() else 0f
            BudgetStatus(
                category = category,
                spent = spent,
                limit = limit,
                ratio = ratio,
                isExceeded = spent > limit && limit > 0.0
            )
        }.sortedByDescending { it.spent }
    }

    private fun filterTransactions(
        expenses: List<Expense>,
        searchQuery: String,
        selectedCategory: String?,
        selectedCard: String?,
        dateFilter: DateFilter,
        now: Long
    ): List<Expense> {
        val query = searchQuery.trim().lowercase(Locale("tr", "TR"))
        return expenses.filter { expense ->
            val matchesQuery = query.isBlank() || listOf(
                expense.title,
                expense.category,
                expense.cardName,
                expense.note.orEmpty()
            ).any { it.lowercase(Locale("tr", "TR")).contains(query) }

            val matchesCategory = selectedCategory == null || expense.category == selectedCategory
            val matchesCard = selectedCard == null || expense.cardName == selectedCard
            val matchesDate = when (dateFilter) {
                DateFilter.TODAY -> isToday(expense.date, now)
                DateFilter.THIS_WEEK -> isInCurrentWeek(expense.date, now)
                DateFilter.THIS_MONTH -> isInCurrentMonth(expense.date, now)
                DateFilter.ALL -> true
            }

            matchesQuery && matchesCategory && matchesCard && matchesDate
        }
    }

    private fun groupTransactions(expenses: List<Expense>): List<TransactionDayGroup> {
        val formatter = SimpleDateFormat("dd MMMM EEEE", Locale("tr", "TR"))
        return expenses
            .groupBy { startOfDay(it.date) }
            .toSortedMap(compareByDescending { it })
            .map { (dayStart, items) ->
                TransactionDayGroup(
                    label = formatter.format(Date(dayStart)),
                    total = items.sumOf { it.amount },
                    expenses = items.sortedByDescending { it.date }
                )
            }
    }

    private fun buildMonthSummary(
        currentMonthTotal: Double,
        previousMonthTotal: Double,
        topCategory: String
    ): MonthSummary {
        val difference = currentMonthTotal - previousMonthTotal
        val comment = when {
            currentMonthTotal == 0.0 -> "Bu ay henuz islem yok. Ilk kayitlarla analiz ekrani dolmaya baslayacak."
            previousMonthTotal == 0.0 -> "Ilk karsilastirma donemi olusuyor. En buyuk harcama alani: $topCategory."
            difference > 0.0 -> "Gecen aya gore ${difference.absoluteValue.formatMoney()} daha fazla harcama var. Ana baski: $topCategory."
            difference < 0.0 -> "Gecen aya gore ${difference.absoluteValue.formatMoney()} daha kontrollu gidiyorsun. En yuksek kategori yine $topCategory."
            else -> "Gecen ayla ayni seviyedesin. Harcama agirligi en cok $topCategory tarafinda."
        }

        return MonthSummary(
            currentMonthTotal = currentMonthTotal,
            previousMonthTotal = previousMonthTotal,
            difference = difference,
            comment = comment,
            topCategory = topCategory
        )
    }

    private fun buildCalendarDays(
        monthExpenses: List<Expense>,
        upcomingPayments: List<UpcomingPayment>,
        now: Long
    ): List<CalendarDaySummary> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        return (1..daysInMonth).map { day ->
            val dayExpenses = monthExpenses.filter {
                val expenseCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
                expenseCalendar.get(Calendar.DAY_OF_MONTH) == day
            }
            val dayPayments = upcomingPayments.filter {
                val paymentCalendar = Calendar.getInstance().apply { timeInMillis = it.dueDate }
                paymentCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                    isInCurrentMonth(it.dueDate, now)
            }

            CalendarDaySummary(
                dayOfMonth = day,
                spent = dayExpenses.sumOf { it.amount },
                transactionCount = dayExpenses.size,
                duePaymentCount = dayPayments.size,
                isToday = day == today
            )
        }
    }

    private fun buildAlerts(
        budgetStatuses: List<BudgetStatus>,
        upcomingPayments: List<UpcomingPayment>,
        monthSummary: MonthSummary
    ): List<String> {
        val alerts = mutableListOf<String>()

        budgetStatuses
            .filter { it.isExceeded }
            .take(2)
            .forEach { alerts += "${it.category} butcesi asildi." }

        upcomingPayments
            .filter { daysUntil(it.dueDate) in 0..3 }
            .take(2)
            .forEach { alerts += "${it.title} odemesi ${daysUntil(it.dueDate)} gun icinde." }

        alerts += monthSummary.comment
        return alerts.distinct().take(4)
    }

    private fun isToday(timestamp: Long, now: Long): Boolean {
        return timestamp in startOfDay(now)..endOfDay(now)
    }

    private fun isInCurrentWeek(timestamp: Long, now: Long): Boolean {
        return timestamp in startOfWeek(now)..endOfWeek(now)
    }

    private fun isInCurrentMonth(timestamp: Long, now: Long): Boolean {
        val current = Calendar.getInstance().apply { timeInMillis = now }
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return current.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            current.get(Calendar.MONTH) == target.get(Calendar.MONTH)
    }

    private fun isInPreviousMonth(timestamp: Long, now: Long): Boolean {
        val previous = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -1)
        }
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return previous.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            previous.get(Calendar.MONTH) == target.get(Calendar.MONTH)
    }

    private fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun startOfWeek(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = timestamp
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfWeek(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfWeek(timestamp)
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun startOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calculateMonthStartOffset(monthStartMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = monthStartMillis }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return (dayOfWeek - Calendar.MONDAY + 7) % 7
    }

    private fun getDaysInMonth(monthStartMillis: Long): Int {
        return Calendar.getInstance().apply { timeInMillis = monthStartMillis }
            .getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun nextOccurrenceOfDay(now: Long, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            val safeDay = dayOfMonth.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.DAY_OF_MONTH, safeDay)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < now) {
                add(Calendar.MONTH, 1)
                val nextSafeDay = dayOfMonth.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.DAY_OF_MONTH, nextSafeDay)
            }
        }
        return calendar.timeInMillis
    }

    private fun daysUntil(timestamp: Long): Int {
        val diff = startOfDay(timestamp) - startOfDay(System.currentTimeMillis())
        return (diff / (1000L * 60 * 60 * 24)).toInt()
    }

    private fun Double.formatMoney(): String {
        return String.format(Locale("tr", "TR"), "%,.0f TL", this)
    }
}

class HomeViewModelFactory(
    private val application: Application,
    private val observeExpensesUseCase: ObserveExpensesUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            application = application,
            observeExpensesUseCase = observeExpensesUseCase,
            addExpenseUseCase = addExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase
        ) as T
    }
}
