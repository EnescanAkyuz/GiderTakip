package com.crosshyper.gidertakip.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crosshyper.gidertakip.domain.model.*
import com.crosshyper.gidertakip.ui.theme.GiderTakipTheme

private fun previewExpenses(now: Long): List<Expense> {
    return listOf(
        Expense(
            id = 1,
            title = "Migros Alisverisi",
            amount = 1240.0,
            category = "Market",
            cardName = "Akbank Platinum",
            date = now - 86_400_000L,
            note = "Haftalik mutfak",
            transactionType = TransactionType.ONE_TIME
        ),
        Expense(
            id = 2,
            title = "Spotify",
            amount = 60.0,
            category = "Eglence",
            cardName = "Enpara Kredi Karti",
            date = now - 172_800_000L,
            note = "Aylik uyelik",
            transactionType = TransactionType.SUBSCRIPTION,
            nextDueDate = now + 5 * 86_400_000L,
            isRecurring = true
        ),
        Expense(
            id = 3,
            title = "Telefon",
            amount = 2150.0,
            category = "Elektronik",
            cardName = "Yapi Kredi World",
            date = now - 259_200_000L,
            note = "3 / 12 taksit",
            transactionType = TransactionType.INSTALLMENT,
            installmentCount = 12,
            installmentRemaining = 9,
            nextDueDate = now + 10 * 86_400_000L
        )
    )
}

internal fun previewState(): FinanceHomeState {
    val now = System.currentTimeMillis()
    val expenses = previewExpenses(now)
    return FinanceHomeState(
        monthTotal = 8450.0,
        weekTotal = 2480.0,
        todayTotal = 320.0,
        topCategory = CategoryBreakdownItem("Market", 3200.0, 0.38f),
        upcomingPayments = listOf(
            UpcomingPayment("Akbank Platinum ekstresi", 4180.0, now + 3 * 86_400_000L, UpcomingPaymentType.CARD_BILL, "Akbank Platinum", "Son odeme gunu 12"),
            UpcomingPayment("Spotify", 60.0, now + 5 * 86_400_000L, UpcomingPaymentType.SUBSCRIPTION, "Enpara Kredi Karti", "Abonelik cekimi"),
            UpcomingPayment("Telefon", 2150.0, now + 10 * 86_400_000L, UpcomingPaymentType.INSTALLMENT, "Yapi Kredi World", "9 taksit kaldi")
        ),
        recentTransactions = expenses,
        filteredTransactions = expenses,
        groupedTransactions = listOf(
            TransactionDayGroup("Bugun", 320.0, listOf(expenses.first())),
            TransactionDayGroup("Dun", 2210.0, listOf(expenses[1], expenses[2]))
        ),
        categoryBreakdown = listOf(
            CategoryBreakdownItem("Market", 3200.0, 0.38f),
            CategoryBreakdownItem("Yemek", 2100.0, 0.25f),
            CategoryBreakdownItem("Faturalar", 1450.0, 0.17f)
        ),
        cardSummaries = listOf(
            CardSpendingSummary("Akbank Platinum", 4180.0, 12140.0, now + 3 * 86_400_000L, 12),
            CardSpendingSummary("Enpara Kredi Karti", 1260.0, 6840.0, now + 9 * 86_400_000L, 18),
            CardSpendingSummary("Yapi Kredi World", 3010.0, 9810.0, now + 15 * 86_400_000L, 24)
        ),
        installmentItems = listOf(
            InstallmentTrackingItem(3, "Telefon", 2150.0, "Yapi Kredi World", 9, 12, now + 10 * 86_400_000L)
        ),
        installmentMonthTotal = 2150.0,
        subscriptionItems = listOf(
            SubscriptionTrackingItem(2, "Spotify", 60.0, "Enpara Kredi Karti", now + 5 * 86_400_000L),
            SubscriptionTrackingItem(4, "Netflix", 190.0, "Akbank Platinum", now + 12 * 86_400_000L)
        ),
        subscriptionMonthTotal = 250.0,
        budgetStatuses = listOf(
            BudgetStatus("Market", 3200.0, 5000.0, 0.64f, false),
            BudgetStatus("Yemek", 2100.0, 1800.0, 1.16f, true),
            BudgetStatus("Ulasim", 740.0, 1400.0, 0.53f, false)
        ),
        monthSummary = MonthSummary(
            currentMonthTotal = 8450.0,
            previousMonthTotal = 7120.0,
            difference = 1330.0,
            comment = "Gecen aya gore harcama artti. En cok baski Market tarafinda.",
            topCategory = "Market"
        ),
        calendarMonthMillis = now,
        calendarStartOffset = 2,
        daysInMonth = 30,
        calendarDays = (1..30).map { day ->
            CalendarDaySummary(
                dayOfMonth = day,
                spent = when (day) {
                    3 -> 420.0
                    7 -> 1240.0
                    12 -> 320.0
                    18 -> 780.0
                    else -> 0.0
                },
                transactionCount = if (day in listOf(3, 7, 12, 18)) 1 else 0,
                duePaymentCount = if (day in listOf(12, 15, 22)) 1 else 0,
                isToday = day == 12
            )
        },
        alerts = listOf("Yemek butcesi asildi.", "Akbank ekstresi 3 gun icinde.", "Spotify cekimi bu hafta.")
    )
}

@Composable
private fun PreviewContainer(content: @Composable ColumnScope.() -> Unit) {
    GiderTakipTheme(darkTheme = false, dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppCanvas)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Preview(name = "Overview", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun OverviewTabPreview() {
    PreviewContainer {
        OverviewTab(
            uiState = previewState(),
            quickInput = "Kahve 95",
            onQuickInputChange = {},
            onQuickAdd = {},
            onDetailedAdd = {},
            onDeleteExpense = {}
        )
    }
}

@Preview(name = "Transactions", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun TransactionsTabPreview() {
    PreviewContainer {
        TransactionsTab(
            uiState = previewState(),
            categories = listOf("Market", "Yemek", "Ulasim", "Eglence"),
            cards = listOf("Akbank Platinum", "Enpara Kredi Karti", "Yapi Kredi World"),
            searchQuery = "",
            selectedCategory = null,
            selectedCard = null,
            selectedDateFilter = DateFilter.THIS_MONTH,
            onSearchQueryChange = {},
            onCategoryFilterChange = {},
            onCardFilterChange = {},
            onDateFilterChange = {},
            onClearFilters = {},
            onDeleteExpense = {}
        )
    }
}

@Preview(name = "Calendar", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun CalendarTabPreview() {
    PreviewContainer { CalendarTab(uiState = previewState()) }
}

@Preview(name = "Insights", showBackground = true, backgroundColor = 0xFFF4F0E8)
@Composable
private fun InsightsTabPreview() {
    PreviewContainer {
        InsightsTab(
            uiState = previewState(),
            budgetLimits = mapOf("Market" to 5000.0, "Yemek" to 1800.0, "Ulasim" to 1400.0),
            onBudgetLimitChange = { _, _ -> }
        )
    }
}

@Preview(name = "Add Expense Sheet", showBackground = true, backgroundColor = 0xFFF4F0E8, heightDp = 900)
@Composable
private fun AddExpenseSheetPreview() {
    GiderTakipTheme(darkTheme = false, dynamicColor = false) {
        AddExpenseSheet(
            categories = listOf("Market", "Yemek", "Ulasim", "Eglence"),
            cards = listOf("Nakit", "Akbank Platinum", "Enpara Kredi Karti"),
            onDismiss = {},
            onSubmit = { _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}
