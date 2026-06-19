package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val source: String, // "Cash", "Bank", "Credit Card", etc.
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
