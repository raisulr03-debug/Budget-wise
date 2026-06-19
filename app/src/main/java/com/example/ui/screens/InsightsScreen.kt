package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChartColors
import com.example.ui.components.DonutExpenseChart
import com.example.ui.viewmodel.BudgetViewModel

@Composable
fun InsightsScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val categoryShares by viewModel.categoryDistribution.collectAsState()
    val metrics by viewModel.financialMetrics.collectAsState()

    val totalExpenses = metrics.totalExpenses

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // High level Expense classification
        item {
            DonutExpenseChart(shares = categoryShares, totalExpenses = totalExpenses)
        }

        // Details Item Panel Header
        if (categoryShares.isNotEmpty()) {
            item {
                Text(
                    text = "Category Ranking",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            itemsIndexed(categoryShares) { index, share ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        ChartColors[index % ChartColors.size],
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = share.category,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format("%.2f", share.amount)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.1f", share.percentage)}% of expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Budget Advice Block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Smart Financial Advice",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val majorExpense = categoryShares.firstOrNull()
                    val savingRatio = if (metrics.totalIncome > 0) (metrics.netBalance / metrics.totalIncome * 100) else 0.0

                    val advice = remember(majorExpense, savingRatio) {
                        val messages = mutableListOf<String>()
                        
                        // Condition-based tips
                        if (majorExpense != null && majorExpense.percentage > 35) {
                            messages.add("You're spending a significant portion (${String.format("%.1f", majorExpense.percentage)}%) on ${majorExpense.category}. Try setting a weekly cap to control this category.")
                        }

                        if (savingRatio < 10.0 && metrics.totalIncome > 0.0) {
                            messages.add("Your net savings rate is currently sitting low at ${String.format("%.1f", savingRatio)}%. Professional budget advisers suggest a target savings rate of 20% of your earnings.")
                        } else if (savingRatio >= 20.0) {
                            messages.add("Excellent! Your net savings rate is ${String.format("%.1f", savingRatio)}%, exceeding the healthy financial marker of 20%. Keep it up!")
                        }

                        if (metrics.totalExpenses > metrics.totalIncome && metrics.totalIncome > 0.0) {
                            messages.add("Critical Alert: Expenses are surpassing your earnings. Try checking non-essential shopping or entertainment subscriptions to prevent negative balances.")
                        }

                        if (messages.isEmpty()) {
                            messages.add("Log more transaction categories or earnings details to generate advanced monthly insights.")
                            messages.add("Consistent daily Logging helps BudgetWise understand your habits and suggest budget optimizations.")
                        }
                        messages
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        advice.forEach { tip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
