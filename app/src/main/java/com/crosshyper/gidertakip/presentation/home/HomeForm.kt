package com.crosshyper.gidertakip.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crosshyper.gidertakip.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    categories: List<String>,
    cards: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        amount: Double,
        category: String,
        card: String,
        date: Long,
        note: String?,
        transactionType: TransactionType,
        installmentCount: Int,
        installmentRemaining: Int,
        nextDueDate: Long?
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(categories.firstOrNull() ?: "Diger") }
    var card by rememberSaveable { mutableStateOf(cards.firstOrNull() ?: "Nakit") }
    var date by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var transactionType by rememberSaveable { mutableStateOf(TransactionType.ONE_TIME) }
    var installmentCount by rememberSaveable { mutableStateOf("6") }
    var installmentRemaining by rememberSaveable { mutableStateOf("6") }
    var nextDueDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDueDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("Tamam") }
            }
        ) { DatePicker(state = state) }
    }

    if (showDueDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = nextDueDate ?: date)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    nextDueDate = state.selectedDateMillis
                    showDueDatePicker = false
                }) { Text("Tamam") }
            }
        ) { DatePicker(state = state) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Yeni Islem", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Ink)
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Baslik") })
            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } }, modifier = Modifier.fillMaxWidth(), label = { Text("Tutar") })

            FilterRow(
                title = "Islem Tipi",
                options = TransactionType.entries.map { it.name },
                selected = transactionType.name,
                onSelected = { selected -> transactionType = TransactionType.valueOf(selected) }
            )
            FilterRow("Kategori", categories, category, onSelected = { category = it })
            FilterRow("Kart / Hesap", cards, card, onSelected = { card = it })

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryPill(
                    modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                    label = "Islem Tarihi",
                    value = formatDate(date, "dd MMM yyyy")
                )
                SummaryPill(
                    modifier = Modifier.weight(1f).clickable {
                        if (transactionType != TransactionType.ONE_TIME) showDueDatePicker = true
                    },
                    label = if (transactionType == TransactionType.SUBSCRIPTION) "Cekim Gunu" else "Sonraki Odeme",
                    value = if (transactionType == TransactionType.ONE_TIME) {
                        "Gerekli Degil"
                    } else {
                        nextDueDate?.let { formatDate(it, "dd MMM yyyy") } ?: "Tarih Sec"
                    }
                )
            }

            if (transactionType == TransactionType.INSTALLMENT) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = installmentCount, onValueChange = { installmentCount = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f), label = { Text("Toplam Taksit") })
                    OutlinedTextField(value = installmentRemaining, onValueChange = { installmentRemaining = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f), label = { Text("Kalan Taksit") })
                }
            }

            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Not") })

            Button(
                onClick = {
                    val parsedAmount = amount.replace(",", ".").toDoubleOrNull() ?: return@Button
                    onSubmit(
                        title,
                        parsedAmount,
                        category,
                        card,
                        date,
                        note,
                        transactionType,
                        installmentCount.toIntOrNull() ?: 1,
                        installmentRemaining.toIntOrNull() ?: 0,
                        if (transactionType == TransactionType.ONE_TIME) null else nextDueDate
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kaydet")
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterRow(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = Ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = selected == option, onClick = { onSelected(option) }, label = { Text(option) })
            }
        }
    }
}
