package com.crosshyper.gidertakip.domain.model

enum class DateFilter(val label: String) {
    TODAY("Bugun"),
    THIS_WEEK("Bu Hafta"),
    THIS_MONTH("Bu Ay"),
    ALL("Tumu")
}

enum class HomeTab(val label: String) {
    OVERVIEW("Ana Ekran"),
    TRANSACTIONS("Islemler"),
    CALENDAR("Takvim"),
    INSIGHTS("Analiz")
}

enum class UpcomingPaymentType {
    CARD_BILL,
    INSTALLMENT,
    SUBSCRIPTION
}

data class CategoryBreakdownItem(
    val category: String,
    val total: Double,
    val ratio: Float
)

data class UpcomingPayment(
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val type: UpcomingPaymentType,
    val cardName: String,
    val subtitle: String
)

data class CardSpendingSummary(
    val cardName: String,
    val monthSpend: Double,
    val totalSpend: Double,
    val nextDueDate: Long?,
    val dueDayOfMonth: Int?
)

data class InstallmentTrackingItem(
    val id: Int,
    val title: String,
    val amount: Double,
    val cardName: String,
    val remainingInstallments: Int,
    val totalInstallments: Int,
    val nextDueDate: Long?
)

data class SubscriptionTrackingItem(
    val id: Int,
    val title: String,
    val amount: Double,
    val cardName: String,
    val nextChargeDate: Long?
)

data class BudgetStatus(
    val category: String,
    val spent: Double,
    val limit: Double,
    val ratio: Float,
    val isExceeded: Boolean
)

data class MonthSummary(
    val currentMonthTotal: Double = 0.0,
    val previousMonthTotal: Double = 0.0,
    val difference: Double = 0.0,
    val comment: String = "Bu ay icin yeterli veri yok.",
    val topCategory: String = "Henuz yok"
)

data class CalendarDaySummary(
    val dayOfMonth: Int,
    val spent: Double,
    val transactionCount: Int,
    val duePaymentCount: Int,
    val isToday: Boolean
)

data class TransactionDayGroup(
    val label: String,
    val total: Double,
    val expenses: List<Expense>
)

data class FinanceHomeState(
    val monthTotal: Double = 0.0,
    val weekTotal: Double = 0.0,
    val todayTotal: Double = 0.0,
    val topCategory: CategoryBreakdownItem? = null,
    val upcomingPayments: List<UpcomingPayment> = emptyList(),
    val recentTransactions: List<Expense> = emptyList(),
    val filteredTransactions: List<Expense> = emptyList(),
    val groupedTransactions: List<TransactionDayGroup> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val cardSummaries: List<CardSpendingSummary> = emptyList(),
    val installmentItems: List<InstallmentTrackingItem> = emptyList(),
    val installmentMonthTotal: Double = 0.0,
    val subscriptionItems: List<SubscriptionTrackingItem> = emptyList(),
    val subscriptionMonthTotal: Double = 0.0,
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val monthSummary: MonthSummary = MonthSummary(),
    val calendarMonthMillis: Long = System.currentTimeMillis(),
    val calendarStartOffset: Int = 0,
    val daysInMonth: Int = 30,
    val calendarDays: List<CalendarDaySummary> = emptyList(),
    val alerts: List<String> = emptyList()
)
