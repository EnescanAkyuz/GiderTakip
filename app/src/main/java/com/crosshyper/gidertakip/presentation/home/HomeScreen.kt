package com.crosshyper.gidertakip.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crosshyper.gidertakip.domain.model.DateFilter
import com.crosshyper.gidertakip.domain.model.Expense
import com.crosshyper.gidertakip.domain.model.FinanceHomeState
import com.crosshyper.gidertakip.domain.model.HomeTab

internal val AppCanvas = Color(0xFFF4F0E8)
internal val Panel = Color(0xFFFDFBF7)
internal val Ink = Color(0xFF18222F)
internal val MutedInk = Color(0xFF5C6470)
internal val Accent = Color(0xFF0D5C63)
internal val Warm = Color(0xFFE2A32D)
internal val Success = Color(0xFF1E7A4D)
internal val Danger = Color(0xFFBF3F32)

@Composable
fun FinanceHome(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val cards by viewModel.availableCards.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val budgetLimits by viewModel.budgetLimits.collectAsState()

    var activeTab by rememberSaveable { mutableStateOf(HomeTab.OVERVIEW) }
    var quickInput by rememberSaveable { mutableStateOf("") }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = AppCanvas,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Ink,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Islem Ekle") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppCanvas)
                .padding(innerPadding)
        ) {
            CompactTopBar(
                uiState = uiState,
                activeTab = activeTab,
                onAddClick = { showAddSheet = true }
            )
            HomeTabBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
            when (activeTab) {
                HomeTab.OVERVIEW -> OverviewTab(
                    uiState = uiState,
                    quickInput = quickInput,
                    onQuickInputChange = { quickInput = it },
                    onQuickAdd = {
                        if (quickInput.isNotBlank()) {
                            viewModel.processQuickInput(quickInput)
                            quickInput = ""
                        }
                    },
                    onDetailedAdd = { showAddSheet = true },
                    onDeleteExpense = viewModel::deleteExpense
                )

                HomeTab.TRANSACTIONS -> TransactionsTab(
                    uiState = uiState,
                    categories = categories,
                    cards = cards,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    selectedCard = selectedCard,
                    selectedDateFilter = selectedDateFilter,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onCategoryFilterChange = viewModel::setCategoryFilter,
                    onCardFilterChange = viewModel::setCardFilter,
                    onDateFilterChange = viewModel::setDateFilter,
                    onClearFilters = viewModel::clearFilters,
                    onDeleteExpense = viewModel::deleteExpense
                )

                HomeTab.CALENDAR -> CalendarTab(uiState = uiState)

                HomeTab.INSIGHTS -> InsightsTab(
                    uiState = uiState,
                    budgetLimits = budgetLimits,
                    onBudgetLimitChange = viewModel::updateBudgetLimit
                )
            }
        }
    }

    if (showAddSheet) {
        AddExpenseSheet(
            categories = categories,
            cards = cards,
            onDismiss = { showAddSheet = false },
            onSubmit = { title, amount, category, card, date, note, type, count, remaining, dueDate ->
                viewModel.addExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    cardName = card,
                    date = date,
                    note = note,
                    transactionType = type,
                    installmentCount = count,
                    installmentRemaining = remaining,
                    nextDueDate = dueDate
                )
                showAddSheet = false
            }
        )
    }
}

@Composable
fun CompactTopBar(
    uiState: FinanceHomeState,
    activeTab: HomeTab,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        color = Panel,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(activeTab.label, color = Ink, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                Text("Bu ay ${formatCurrency(uiState.monthTotal)}", color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
            SummaryPill(
                label = "Yaklasan",
                value = uiState.upcomingPayments.firstOrNull()?.let { formatDate(it.dueDate, "dd MMM") } ?: "Yok"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAddClick) { Text("Ekle") }
        }
    }
}

@Composable
fun HomeTabBar(activeTab: HomeTab, onTabSelected: (HomeTab) -> Unit) {
    TabRow(
        selectedTabIndex = HomeTab.entries.indexOf(activeTab),
        containerColor = AppCanvas
    ) {
        HomeTab.entries.forEach { tab ->
            Tab(
                selected = activeTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.label) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            HomeTab.OVERVIEW -> Icons.Default.GridView
                            HomeTab.TRANSACTIONS -> Icons.AutoMirrored.Filled.ReceiptLong
                            HomeTab.CALENDAR -> Icons.Default.CalendarMonth
                            HomeTab.INSIGHTS -> Icons.Default.Analytics
                        },
                        contentDescription = null
                    )
                }
            )
        }
    }
}
