package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Transaction
import com.example.data.repository.SyncState
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    
    // UI input filter flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val typeFilter = _typeFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL") // "ALL" or specific category
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _sourceFilter = MutableStateFlow("ALL") // "ALL" or specific source
    val sourceFilter = _sourceFilter.asStateFlow()

    // Interactive selections
    private val _showLogDialog = MutableStateFlow(false)
    val showLogDialog = _showLogDialog.asStateFlow()

    // Sync state flows
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("budgetwise_prefs", Context.MODE_PRIVATE)
    
    private val _cloudSyncUrl = MutableStateFlow(sharedPrefs.getString("sync_url", "https://api.budgetwise.app/v1/sync") ?: "")
    val cloudSyncUrl = _cloudSyncUrl.asStateFlow()

    private val _cloudPassphrase = MutableStateFlow(sharedPrefs.getString("sync_passphrase", "SecureBudgetKey123") ?: "")
    val cloudPassphrase = _cloudPassphrase.asStateFlow()

    // Device ID for sync metadata
    val deviceId: String = try {
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
    } catch (e: Exception) {
        UUID.randomUUID().toString()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao, application)
    }

    // Expose complete raw list
    val transactionsStream = repository.allTransactions

    // Filtered transaction list
    val filteredTransactions = combine(
        transactionsStream,
        _searchQuery,
        _typeFilter,
        _categoryFilter,
        _sourceFilter
    ) { rawList, query, type, cat, src ->
        rawList.filter { tx ->
            val matchesQuery = tx.notes.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.source.contains(query, ignoreCase = true)
            
            val matchesType = type == "ALL" || tx.type == type
            val matchesCategory = cat == "ALL" || tx.category == cat
            val matchesSource = src == "ALL" || tx.source == src
            
            matchesQuery && matchesType && matchesCategory && matchesSource
        }
    } .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Metrics State
    val financialMetrics = transactionsStream.map { list ->
        var income = 0.0
        var expenses = 0.0
        list.forEach { tx ->
            if (tx.type == "INCOME") {
                income += tx.amount
            } else {
                expenses += tx.amount
            }
        }
        val balance = income - expenses
        val currentMonthExpenses = list.filter { isCurrentMonth(it.date) && it.type == "EXPENSE" }.sumOf { it.amount }
        val currentMonthIncome = list.filter { isCurrentMonth(it.date) && it.type == "INCOME" }.sumOf { it.amount }
        
        Metrics(
            totalIncome = income,
            totalExpenses = expenses,
            netBalance = balance,
            currentMonthExpenses = currentMonthExpenses,
            currentMonthIncome = currentMonthIncome
        )
    } .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Metrics())

    // Category distribution for charts
    val categoryDistribution = transactionsStream.map { list ->
        val expenseGroup = list.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val total = expenseGroup.values.sum()
        expenseGroup.map { (cat, amount) ->
            CategoryShare(
                category = cat,
                amount = amount,
                percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }
    } .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly trends (past 6 months) for charts
    val monthlyTrend = transactionsStream.map { list ->
        val cal = Calendar.getInstance()
        val trends = mutableListOf<MonthlyTrendItem>()
        
        // Loop past 6 months
        for (i in 5 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.MONTH, -i)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) // 0-11
            
            val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
            
            val monthIncome = list.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
                tCal.get(Calendar.YEAR) == year && tCal.get(Calendar.MONTH) == month && it.type == "INCOME"
            }.sumOf { it.amount }

            val monthExpense = list.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
                tCal.get(Calendar.YEAR) == year && tCal.get(Calendar.MONTH) == month && it.type == "EXPENSE"
            }.sumOf { it.amount }

            trends.add(MonthlyTrendItem(monthLabel = monthName, income = monthIncome, expense = monthExpense))
        }
        trends
    } .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Available categories and sources in the system for filter dropdowns
    val availableCategories = listOf(
        "Salary", "Freelance", "Investments", "Gifts", // Income categories
        "Food", "Rent", "Shopping", "Entertainment", "Utilities", "Transport", "Healthcare", "Travel", "Education", "Other" // Expense categories
    )

    val availableSources = listOf(
        "Cash", "Bank Account", "Credit Card", "Mobile Wallet", "Other"
    )

    // Actions
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setTypeFilter(type: String) { _typeFilter.value = type }
    fun setCategoryFilter(category: String) { _categoryFilter.value = category }
    fun setSourceFilter(source: String) { _sourceFilter.value = source }
    
    fun setShowLogDialog(show: Boolean) { _showLogDialog.value = show }

    fun addTransaction(amount: Double, type: String, category: String, source: String, notes: String, date: Long) {
        viewModelScope.launch {
            val tx = Transaction(
                amount = amount,
                type = type,
                category = category,
                source = source,
                notes = notes,
                date = date,
                isSynced = false
            )
            repository.insert(tx)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun updateSyncParams(url: String, key: String) {
        _cloudSyncUrl.value = url
        _cloudPassphrase.value = key
        sharedPrefs.edit()
            .putString("sync_url", url)
            .putString("sync_passphrase", key)
            .apply()
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = repository.performCloudSync(
                syncUrl = _cloudSyncUrl.value,
                syncPasswordKey = _cloudPassphrase.value,
                deviceId = deviceId
            )
            _syncState.value = result
        }
    }

    fun clearSyncStatus() {
        _syncState.value = SyncState.Idle
    }

    private fun isCurrentMonth(timestamp: Long): Boolean {
        val calNow = Calendar.getInstance()
        val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calNow.get(Calendar.YEAR) == calTx.get(Calendar.YEAR) &&
                calNow.get(Calendar.MONTH) == calTx.get(Calendar.MONTH)
    }

    data class Metrics(
        val totalIncome: Double = 0.0,
        val totalExpenses: Double = 0.0,
        val netBalance: Double = 0.0,
        val currentMonthExpenses: Double = 0.0,
        val currentMonthIncome: Double = 0.0
    )

    data class CategoryShare(
        val category: String,
        val amount: Double,
        val percentage: Float
    )

    data class MonthlyTrendItem(
        val monthLabel: String,
        val income: Double,
        val expense: Double
    )
}
