package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddTransactionDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {

    private var initialAddTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Capture initial deep-link trigger if launched from widget
        if (intent?.data != null) {
            val uri = intent.data
            if (uri?.scheme == "budgetwise" && uri.host == "add_transaction") {
                initialAddTriggered = true
            }
        }

        setContent {
            MyApplicationTheme {
                val viewModel: BudgetViewModel = viewModel()
                
                // Track deep link when view model loaded
                LaunchedEffect(Unit) {
                    if (initialAddTriggered) {
                        viewModel.setShowLogDialog(true)
                        initialAddTriggered = false
                    }
                }

                BudgetWiseApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle subsequent widget click activations
        val uri = intent.data
        if (uri?.scheme == "budgetwise" && uri.host == "add_transaction") {
            setContent {
                MyApplicationTheme {
                    val viewModel: BudgetViewModel = viewModel()
                    LaunchedEffect(Unit) {
                        viewModel.setShowLogDialog(true)
                    }
                    BudgetWiseApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetWiseApp(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("dashboard") }
    val showAddDialog by viewModel.showLogDialog.collectAsState()

    val availableCategories = viewModel.availableCategories
    val availableSources = viewModel.availableSources

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)), // Brand Emerald Badge
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "BudgetWise",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    if (selectedTab == "sync") {
                        IconButton(onClick = { viewModel.clearSyncStatus() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear logs")
                        }
                    } else {
                        IconButton(onClick = { viewModel.setShowLogDialog(true) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Quick Add")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 8.dp
            ) {
                // Dashboard Tab
                NavigationBarItem(
                    selected = selectedTab == "dashboard",
                    onClick = { selectedTab = "dashboard" },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard navigation item") },
                    label = { Text("Overview") }
                )

                // Transactions Ledger Tab
                NavigationBarItem(
                    selected = selectedTab == "transactions",
                    onClick = { selectedTab = "transactions" },
                    icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Transactions navigation item") },
                    label = { Text("Ledger") }
                )

                // Statistical Charts Tab
                NavigationBarItem(
                    selected = selectedTab == "insights",
                    onClick = { selectedTab = "insights" },
                    icon = { Icon(imageVector = Icons.Default.PieChart, contentDescription = "Charts navigation item") },
                    label = { Text("Insights") }
                )

                // Sync Settings Tab
                NavigationBarItem(
                    selected = selectedTab == "sync",
                    onClick = { selectedTab = "sync" },
                    icon = { Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Secure Sync navigation item") },
                    label = { Text("Sync") }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.setShowLogDialog(true) },
                icon = { Icon(Icons.Default.Add, "Plus icon") },
                text = { Text("Log Transaction", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF2E7D32), // Emerald CTA
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            AnimatedContent(
                targetState = selectedTab,
                label = "routing_screens"
            ) { tab ->
                when (tab) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = { selectedTab = "transactions" }
                    )
                    "transactions" -> TransactionsScreen(
                        viewModel = viewModel
                    )
                    "insights" -> InsightsScreen(
                        viewModel = viewModel
                    )
                    "sync" -> SyncScreen(
                        viewModel = viewModel
                    )
                }
            }

            // Quick log transaction dialog popup
            if (showAddDialog) {
                AddTransactionDialog(
                    availableCategories = availableCategories,
                    availableSources = availableSources,
                    onDismiss = { viewModel.setShowLogDialog(false) },
                    onSave = { amount, type, category, source, notes, date ->
                        viewModel.addTransaction(amount, type, category, source, notes, date)
                        viewModel.setShowLogDialog(false)
                    }
                )
            }
        }
    }
}
