package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val search by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.typeFilter.collectAsState()
    val selectedCategory by viewModel.categoryFilter.collectAsState()
    val selectedSource by viewModel.sourceFilter.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()

    var showCategoryFilterDialog by remember { mutableStateOf(false) }
    var showSourceFilterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Input Bar Block
        OutlinedTextField(
            value = search,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search description, category, source...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = OutlinedTextFieldDefaults.colors()
        )

        // Type filter quick chips (ALL, EXPENSE, INCOME)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("ALL" to "All", "EXPENSE" to "Expenses", "INCOME" to "Earnings").forEach { (value, label) ->
                FilterChip(
                    selected = selectedType == value,
                    onClick = { viewModel.setTypeFilter(value) },
                    label = { Text(label, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (value == "INCOME") Color(0xFF2E7D32).copy(alpha = 0.2f)
                        else if (value == "EXPENSE") Color(0xFFC62828).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = if (value == "INCOME") Color(0xFF2E7D32)
                        else if (value == "EXPENSE") Color(0xFFC62828)
                        else MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Dropdowns Trigger Row: Category and Source Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Filter Trigger Card
            InputChip(
                selected = selectedCategory != "ALL",
                onClick = { showCategoryFilterDialog = true },
                label = { Text("Category: ${if (selectedCategory == "ALL") "All" else selectedCategory}") },
                trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )

            // Source Filter Trigger Card
            InputChip(
                selected = selectedSource != "ALL",
                onClick = { showSourceFilterDialog = true },
                label = { Text("Source: ${if (selectedSource == "ALL") "All" else selectedSource}") },
                trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
        }

        // Active filters bar summary if any is active
        if (selectedCategory != "ALL" || selectedSource != "ALL") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Active Filters:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                AssistChip(
                    onClick = {
                        viewModel.setCategoryFilter("ALL")
                        viewModel.setSourceFilter("ALL")
                    },
                    label = { Text("Reset All", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(12.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid Content Panel list
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Search details empty state",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matching transactions found.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try refining your search query, selecting another category, or changing the income/expense filters.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRowItem(transaction = tx, onDelete = { viewModel.deleteTransaction(tx) })
                }
            }
        }
    }

    // Category Choice Dialog
    if (showCategoryFilterDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryFilterDialog = false },
            title = { Text("Select Category Filter") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // ALL Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setCategoryFilter("ALL")
                                showCategoryFilterDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCategory == "ALL", onClick = {
                            viewModel.setCategoryFilter("ALL")
                            showCategoryFilterDialog = false
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All Categories")
                    }

                    Divider()

                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(viewModel.availableCategories) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setCategoryFilter(cat)
                                        showCategoryFilterDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedCategory == cat, onClick = {
                                    viewModel.setCategoryFilter(cat)
                                    showCategoryFilterDialog = false
                                })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Source Choice Dialog
    if (showSourceFilterDialog) {
        AlertDialog(
            onDismissRequest = { showSourceFilterDialog = false },
            title = { Text("Select Payment Source Filter") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSourceFilter("ALL")
                                showSourceFilterDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedSource == "ALL", onClick = {
                            viewModel.setSourceFilter("ALL")
                            showSourceFilterDialog = false
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All Sources")
                    }

                    Divider()

                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(viewModel.availableSources) { src ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSourceFilter(src)
                                        showSourceFilterDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedSource == src, onClick = {
                                    viewModel.setSourceFilter(src)
                                    showSourceFilterDialog = false
                                })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(src)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
