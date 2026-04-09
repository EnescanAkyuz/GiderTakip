package com.crosshyper.gidertakip.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crosshyper.gidertakip.domain.model.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OverviewTab(
    uiState: FinanceHomeState,
    quickInput: String,
    onQuickInputChange: (String) -> Unit,
    onQuickAdd: () -> Unit,
    onDetailedAdd: () -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroHeader(uiState) }
        item { QuickAddCard(quickInput, onQuickInputChange, onQuickAdd, onDetailedAdd) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Bugun", formatCurrency(uiState.todayTotal), Icons.Default.LocalFireDepartment, Warm)
                StatCard(Modifier.weight(1f), "Bu Hafta", formatCurrency(uiState.weekTotal), Icons.Default.Timeline, Accent)
            }
        }
        item { SectionTitle("Yaklasan Odemeler") }
        item { UpcomingPaymentsSection(uiState.upcomingPayments) }
        item { SectionTitle("Kartlar") }
        item { CardSummariesSection(uiState.cardSummaries) }
        item { SectionTitle("Taksit ve Abonelikler") }
        item { TrackingSection(uiState.installmentItems, uiState.subscriptionItems) }
        item { SectionTitle("Son Islemler") }
        item { RecentTransactionsSection(uiState.recentTransactions, onDeleteExpense) }
    }
}

@Composable
fun HeroHeader(uiState: FinanceHomeState) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF16324F), Color(0xFF0B1726))))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Paran nereye gidiyor, tek bakista gor", color = Color.White.copy(alpha = 0.82f))
            Text(formatCurrency(uiState.monthTotal), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeaderInfoCard(Modifier.weight(1f), "En Cok", uiState.topCategory?.category ?: "Veri yok", uiState.topCategory?.total?.let(::formatCurrency) ?: "-")
                HeaderInfoCard(Modifier.weight(1f), "Yaklasan", uiState.upcomingPayments.firstOrNull()?.title ?: "Odeme yok", uiState.upcomingPayments.firstOrNull()?.let { formatDate(it.dueDate, "dd MMM") } ?: "-")
            }
            uiState.alerts.take(2).forEach { alert ->
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.10f)) {
                    Text(alert, modifier = Modifier.padding(12.dp), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HeaderInfoCard(modifier: Modifier, label: String, value: String, subtitle: String) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.12f)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}

@Composable
fun QuickAddCard(quickInput: String, onQuickInputChange: (String) -> Unit, onQuickAdd: () -> Unit, onDetailedAdd: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(22.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hizli Kayit", fontWeight = FontWeight.Bold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = quickInput,
                    onValueChange = onQuickInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Orn: Kahve 95") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onQuickAdd() })
                )
                Button(onClick = onQuickAdd) { Text("Ekle") }
            }
            TextButton(onClick = onDetailedAdd) { Text("Detayli kayit ac") }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, tone: Color) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tone)
            }
            Text(title, color = MutedInk)
            Text(value, color = Ink, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, color = Ink, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun UpcomingPaymentsSection(payments: List<UpcomingPayment>) {
    if (payments.isEmpty()) {
        EmptyPanel("Yaklasan odeme bulunmuyor.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        payments.forEach { payment ->
            ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(paymentTypeColor(payment.type).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(paymentTypeIcon(payment.type), contentDescription = null, tint = paymentTypeColor(payment.type))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(payment.title, fontWeight = FontWeight.Bold, color = Ink)
                        Text(payment.subtitle, color = MutedInk, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatCurrency(payment.amount), fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text(formatDate(payment.dueDate, "dd MMM"), color = MutedInk, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun CardSummariesSection(cardSummaries: List<CardSpendingSummary>) {
    val cards = cardSummaries.filter { it.monthSpend > 0.0 || it.totalSpend > 0.0 }
    if (cards.isEmpty()) {
        EmptyPanel("Kart harcamasi henuz yok.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.take(4).forEach { summary ->
            ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = cardColor(summary.cardName))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(summary.cardName, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                        Text(formatCurrency(summary.monthSpend), color = Ink, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryPill(Modifier.weight(1f), "Bu Ay", formatCurrency(summary.monthSpend))
                        SummaryPill(Modifier.weight(1f), "Toplam", formatCurrency(summary.totalSpend))
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingSection(installmentItems: List<InstallmentTrackingItem>, subscriptionItems: List<SubscriptionTrackingItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Taksitler", fontWeight = FontWeight.Bold, color = Ink)
                if (installmentItems.isEmpty()) Text("Aktif taksit yok.", color = MutedInk)
                installmentItems.take(3).forEach { item ->
                    TrackingRow(item.title, "${item.remainingInstallments}/${item.totalInstallments} kaldi", formatCurrency(item.amount))
                }
            }
        }
        ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Abonelikler", fontWeight = FontWeight.Bold, color = Ink)
                if (subscriptionItems.isEmpty()) Text("Aktif abonelik yok.", color = MutedInk)
                subscriptionItems.take(3).forEach { item ->
                    TrackingRow(item.title, item.cardName, formatCurrency(item.amount))
                }
            }
        }
    }
}

@Composable
fun TrackingRow(title: String, subtitle: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink)
            Text(subtitle, color = MutedInk, style = MaterialTheme.typography.bodySmall)
        }
        Text(amount, color = Ink, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun RecentTransactionsSection(expenses: List<Expense>, onDeleteExpense: (Expense) -> Unit) {
    if (expenses.isEmpty()) {
        EmptyPanel("Islem bulunmuyor.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        expenses.forEach { ExpenseRow(it, onDeleteExpense) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsTab(
    uiState: FinanceHomeState,
    categories: List<String>,
    cards: List<String>,
    searchQuery: String,
    selectedCategory: String?,
    selectedCard: String?,
    selectedDateFilter: DateFilter,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onCardFilterChange: (String?) -> Unit,
    onDateFilterChange: (DateFilter) -> Unit,
    onClearFilters: () -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Islem ara") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateFilter.entries.forEach { filter ->
                            FilterChip(selected = selectedDateFilter == filter, onClick = { onDateFilterChange(filter) }, label = { Text(filter.label) })
                        }
                    }
                    FilterGroup(options = categories, selected = selectedCategory, allLabel = "Tum Kategoriler", onSelected = onCategoryFilterChange)
                    FilterGroup(options = cards, selected = selectedCard, allLabel = "Tum Kartlar", onSelected = onCardFilterChange)
                    TextButton(onClick = onClearFilters) { Text("Filtreleri Temizle") }
                }
            }
        }
        if (uiState.groupedTransactions.isEmpty()) {
            item { EmptyPanel("Bu filtrelerle eslesen islem yok.") }
        } else {
            items(uiState.groupedTransactions) { group ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(group.label, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.ExtraBold)
                        Text(formatCurrency(group.total), color = MutedInk)
                    }
                    group.expenses.forEach { ExpenseRow(it, onDeleteExpense) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(options: List<String>, selected: String?, allLabel: String, onSelected: (String?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text(allLabel) })
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelected(option) }, label = { Text(option) })
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, onDeleteExpense: (Expense) -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor(expense.category).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(expense.category), contentDescription = null, tint = categoryColor(expense.category))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, color = Ink)
                Text("${expense.category} · ${expense.cardName}", color = MutedInk, style = MaterialTheme.typography.bodySmall)
                Text(formatDate(expense.date, "dd MMM yyyy"), color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(expense.amount), fontWeight = FontWeight.ExtraBold, color = Ink)
                TextButton(onClick = { onDeleteExpense(expense) }) { Text("Sil") }
            }
        }
    }
}

@Composable
fun CalendarTab(uiState: FinanceHomeState) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(formatDate(uiState.calendarMonthMillis, "MMMM yyyy"), fontWeight = FontWeight.ExtraBold, color = Ink)
                    CalendarGrid(uiState)
                }
            }
        }
        item { SectionTitle("Odeme Takvimi") }
        item { UpcomingPaymentsSection(uiState.upcomingPayments) }
    }
}

@Composable
fun CalendarGrid(uiState: FinanceHomeState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Pzt", "Sal", "Car", "Per", "Cum", "Cts", "Paz").forEach { Text(it, color = MutedInk, modifier = Modifier.width(40.dp)) }
        }
        val cells = buildList<CalendarDaySummary?> {
            repeat(uiState.calendarStartOffset) { add(null) }
            addAll(uiState.calendarDays)
            while (size % 7 != 0) add(null)
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    if (day == null) Spacer(modifier = Modifier.size(40.dp)) else CalendarDayCell(day)
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: CalendarDaySummary) {
    val background = when {
        day.isToday -> Ink
        day.spent > 0.0 -> Color(0xFFF1E7D4)
        else -> Color(0xFFF7F2EA)
    }
    val textColor = if (day.isToday) Color.White else Ink
    Column(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(day.dayOfMonth.toString(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (day.transactionCount > 0 || day.duePaymentCount > 0) {
            Text("${day.transactionCount}/${day.duePaymentCount}", color = textColor, fontSize = 8.sp)
        }
    }
}

@Composable
fun InsightsTab(uiState: FinanceHomeState, budgetLimits: Map<String, Double>, onBudgetLimitChange: (String, Double) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Gunluk", formatCurrency(uiState.todayTotal), Icons.Default.Timeline, Accent)
                StatCard(Modifier.weight(1f), "Aylik", formatCurrency(uiState.monthTotal), Icons.Default.AccountBalanceWallet, Success)
            }
        }
        item { SectionTitle("Kategori Dagilimi") }
        item { CategoryDistributionSection(uiState.categoryBreakdown) }
        item { SectionTitle("Ay Sonu Ozeti") }
        item { MonthSummarySection(uiState.monthSummary) }
        item { SectionTitle("Butce Ayarlari") }
        item { BudgetEditorSection(uiState.budgetStatuses, budgetLimits, onBudgetLimitChange) }
    }
}

@Composable
fun CategoryDistributionSection(items: List<CategoryBreakdownItem>) {
    if (items.isEmpty()) {
        EmptyPanel("Bu ay icin dagilim verisi yok.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.take(6).forEach { item ->
            ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.category, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Ink)
                        Text(formatCurrency(item.total), color = Ink, fontWeight = FontWeight.ExtraBold)
                    }
                    LinearProgressIndicator(progress = { item.ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = categoryColor(item.category), trackColor = Color(0xFFE8E0D4))
                }
            }
        }
    }
}

@Composable
fun MonthSummarySection(summary: MonthSummary) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryPill(Modifier.weight(1f), "Bu Ay", formatCurrency(summary.currentMonthTotal))
                SummaryPill(Modifier.weight(1f), "Gecen Ay", formatCurrency(summary.previousMonthTotal))
            }
            SummaryPill(label = "Fark", value = formatCurrency(kotlin.math.abs(summary.difference)))
            Text(summary.comment, color = Ink)
        }
    }
}

@Composable
fun BudgetEditorSection(statuses: List<BudgetStatus>, budgetLimits: Map<String, Double>, onBudgetLimitChange: (String, Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        statuses.forEach { status ->
            BudgetEditorRow(status, budgetLimits[status.category] ?: status.limit, onBudgetLimitChange)
        }
    }
}

@Composable
fun BudgetEditorRow(status: BudgetStatus, initialLimit: Double, onBudgetLimitChange: (String, Double) -> Unit) {
    var input by rememberSaveable(status.category) { mutableStateOf(initialLimit.toInt().toString()) }
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(status.category, fontWeight = FontWeight.Bold, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Aylik Limit") }
                )
                Button(onClick = {
                    input.replace(",", ".").toDoubleOrNull()?.let {
                        onBudgetLimitChange(status.category, it)
                    }
                }) { Text("Kaydet") }
            }
        }
    }
}

@Composable
fun SummaryPill(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1ECE4))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = MutedInk, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Ink, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyPanel(message: String) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Panel)) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(message, color = MutedInk)
        }
    }
}

fun categoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale("tr", "TR"))) {
        "market" -> Icons.Default.AccountBalanceWallet
        "yemek" -> Icons.Default.LocalFireDepartment
        else -> Icons.Default.Analytics
    }
}

fun categoryColor(category: String): Color {
    return when (category.lowercase(Locale("tr", "TR"))) {
        "market" -> Color(0xFF2A9D8F)
        "yemek" -> Color(0xFFE76F51)
        "ulasim" -> Color(0xFF457B9D)
        "faturalar" -> Color(0xFFF4A261)
        "ev" -> Color(0xFF6D597A)
        "saglik" -> Color(0xFF2B9348)
        "eglence" -> Color(0xFFD62828)
        "egitim" -> Color(0xFF577590)
        else -> Accent
    }
}

fun cardColor(card: String): Color {
    return when {
        card.contains("Akbank", ignoreCase = true) -> Color(0xFFCF2E2E)
        card.contains("Enpara", ignoreCase = true) -> Color(0xFF6C3FB3)
        card.contains("Yapi Kredi", ignoreCase = true) -> Color(0xFF0057B8)
        card.contains("Garanti", ignoreCase = true) -> Color(0xFF1C8C5E)
        else -> Accent
    }
}

fun paymentTypeIcon(type: UpcomingPaymentType): ImageVector {
    return when (type) {
        UpcomingPaymentType.CARD_BILL -> Icons.Default.CreditCard
        UpcomingPaymentType.INSTALLMENT -> Icons.Default.Timeline
        UpcomingPaymentType.SUBSCRIPTION -> Icons.Default.Subscriptions
    }
}

fun paymentTypeColor(type: UpcomingPaymentType): Color {
    return when (type) {
        UpcomingPaymentType.CARD_BILL -> Accent
        UpcomingPaymentType.INSTALLMENT -> Warm
        UpcomingPaymentType.SUBSCRIPTION -> Success
    }
}

fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("tr", "TR")).apply { maximumFractionDigits = 0 }.format(amount)
}

fun formatDate(timestamp: Long, pattern: String): String {
    return SimpleDateFormat(pattern, Locale("tr", "TR")).format(Date(timestamp))
}
