package com.crosshyper.gidertakip

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.crosshyper.gidertakip.data.AppDatabase
import com.crosshyper.gidertakip.data.Expense
import com.crosshyper.gidertakip.data.ExpenseRepository
import com.crosshyper.gidertakip.ui.theme.GiderTakipTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())
        val viewModel: ExpenseViewModel by viewModels { 
            ExpenseViewModelFactory(application, repository) 
        }

        setContent {
            GiderTakipTheme {
                UltimateExpenseScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltimateExpenseScreen(viewModel: ExpenseViewModel) {
    val expenses by viewModel.filteredExpenses.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrBlank()) viewModel.processSmartInput(spokenText)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showDatePicker = false
                }) { Text("Tamam") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Header Section
            UltimateHeader(
                total = totalAmount ?: 0.0,
                date = selectedDate,
                mode = viewMode,
                onModeChange = { viewModel.setViewMode(it) },
                onDateClick = { showDatePicker = true }
            )

            // Search Bar (Animasyonlu)
            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    placeholder = { Text("Harcama ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { 
                            viewModel.setSearchQuery("")
                            showSearch = false 
                        }) { Icon(Icons.Default.Close, contentDescription = null) }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Quick Input Card
            QuickInputCard(
                inputText = inputText,
                onValueChange = { inputText = it },
                onAddClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.processSmartInput(inputText)
                        inputText = ""
                    }
                },
                onMicClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    }
                    speechLauncher.launch(intent)
                },
                onSearchToggle = { showSearch = !showSearch }
            )

            // Expenses List
            Text(
                text = if (searchQuery.isNotEmpty()) "Arama Sonuçları" else "Harcamalar",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold
            )

            if (expenses.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        ModernExpenseItem(expense, onDelete = { viewModel.deleteExpense(expense) })
                    }
                }
            }
        }
    }
}

@Composable
fun UltimateHeader(
    total: Double, 
    date: Long, 
    mode: ViewMode, 
    onModeChange: (ViewMode) -> Unit,
    onDateClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MMMM yyyy", Locale("tr"))
    val dateString = if (mode == ViewMode.DAY) {
        SimpleDateFormat("dd MMMM yyyy", Locale("tr")).format(Date(date))
    } else sdf.format(Date(date))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2D3436), Color(0xFF000000))))
            .padding(top = 24.dp, bottom = 40.dp, start = 24.dp, end = 24.dp)
    ) {
        Column {
            // View Mode Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(4.dp)
            ) {
                ViewMode.values().forEach { m ->
                    val selected = m == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable { onModeChange(m) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(m) {
                                ViewMode.DAY -> "Gün"
                                ViewMode.WEEK -> "Hafta"
                                ViewMode.MONTH -> "Ay"
                            },
                            color = if (selected) Color.Black else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onDateClick() }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateString,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${String.format(Locale.getDefault(), "%.2f", total)} TL",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickInputCard(
    inputText: String,
    onValueChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onMicClick: () -> Unit,
    onSearchToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = (-20).dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchToggle) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hızlı ekle...", color = Color.LightGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = Color(0xFFF1F3F5),
                    focusedContainerColor = Color(0xFFF1F3F5)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddClick() })
            )
            IconButton(onClick = onMicClick) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
            }
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.clip(CircleShape).background(Color.Black)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun ModernExpenseItem(expense: Expense, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF1F3F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = getCategoryIcon(expense.category), contentDescription = null, tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.title, fontWeight = FontWeight.Bold, color = Color(0xFF2D3436))
                if (!expense.note.isNullOrEmpty()) {
                    Text(text = expense.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${String.format(Locale.getDefault(), "%.2f", expense.amount)} TL",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE74C3C)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Harcama bulunamadı.", color = Color.Gray)
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "yemek" -> Icons.Default.Restaurant
        "market" -> Icons.Default.ShoppingCart
        "ulaşım" -> Icons.Default.DirectionsCar
        "kira" -> Icons.Default.Home
        "fatura" -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.ReceiptLong
    }
}
