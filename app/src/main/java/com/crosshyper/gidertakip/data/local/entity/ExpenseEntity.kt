package com.crosshyper.gidertakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val cardName: String,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val transactionType: String,
    val installmentCount: Int = 1,
    val installmentRemaining: Int = 0,
    val nextDueDate: Long? = null,
    val isRecurring: Boolean = false
)
