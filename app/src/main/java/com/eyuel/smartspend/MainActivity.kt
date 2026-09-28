package com.eyuel.smartspend

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyuel.smartspend.ui.DashboardUiState
import com.eyuel.smartspend.ui.TransactionFilter
import com.eyuel.smartspend.ui.TransactionListViewModel
import com.eyuel.smartspend.ui.components.AnalyticsBottomSheet
import com.eyuel.smartspend.ui.components.EditTransactionBottomSheet
import com.eyuel.smartspend.ui.components.FilterBar
import com.eyuel.smartspend.ui.components.FinancialSummaryCard
import com.eyuel.smartspend.ui.components.TransactionItemCard
import com.eyuel.smartspend.ui.theme.IncomeGreen
import com.eyuel.smartspend.ui.theme.SmartSpendTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartSpendTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TransactionListViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // SMS & Notification Permission Handling
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false
        hasSmsPermissions = receiveGranted && readGranted

        if (hasSmsPermissions) {
            viewModel.syncHistoricalSms()
        }
    }

    // Show sync message toast/snackbar
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SmartSpend",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (hasSmsPermissions) IncomeGreen else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasSmsPermissions) "SMS Tracking Active" else "Permissions Required",
                                fontSize = 12.sp,
                                color = if (hasSmsPermissions) IncomeGreen else Color(0xFFF59E0B)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setAnalyticsVisible(true) }) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Spending Analytics",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Permission Banner (if not granted yet)
            if (!hasSmsPermissions) {
                item {
                    PermissionBanner(
                        onRequestPermissions = {
                            val perms = mutableListOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    )
                }
            }

            // Financial Summary Card
            item {
                FinancialSummaryCard(
                    totalBalance = uiState.totalBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    isSyncing = uiState.isSyncing,
                    onSyncClick = {
                        if (hasSmsPermissions) {
                            viewModel.syncHistoricalSms()
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                        }
                    }
                )
            }

            // Search Bar
            item {
                SearchBarView(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )
            }

            // Filter Tabs (All, Needs Note, Expenses, Income)
            item {
                FilterBar(
                    activeFilter = uiState.activeFilter,
                    unreviewedCount = uiState.unreviewedCount,
                    onFilterSelected = { viewModel.setFilter(it) }
                )
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (uiState.activeFilter) {
                            TransactionFilter.ALL -> "All Transactions"
                            TransactionFilter.UNREVIEWED -> "Pending Review"
                            TransactionFilter.EXPENSES -> "Expenses"
                            TransactionFilter.INCOME -> "Income"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "${uiState.transactions.size} records",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Empty State
            if (uiState.transactions.isEmpty()) {
                item {
                    EmptyTransactionsView(
                        filter = uiState.activeFilter,
                        hasPermissions = hasSmsPermissions,
                        onScanClick = {
                            if (hasSmsPermissions) viewModel.syncHistoricalSms()
                            else permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                        }
                    )
                }
            } else {
                // Transaction List
                items(
                    items = uiState.transactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onClick = { viewModel.openEdit(transaction) }
                    )
                }
            }
        }
    }

    // Modal BottomSheet for Editing Description and Category
    uiState.editingTransaction?.let { transaction ->
        EditTransactionBottomSheet(
            transaction = transaction,
            onDismiss = { viewModel.closeEdit() },
            onSave = { id, desc, cat ->
                viewModel.saveDescriptionAndCategory(id, desc, cat)
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
            }
        )
    }

    // Modal BottomSheet for Analytics & Category Breakdown
    if (uiState.showAnalytics) {
        AnalyticsBottomSheet(
            transactions = uiState.rawAllTransactions,
            onDismiss = { viewModel.setAnalyticsVisible(false) },
            onExportCsv = { viewModel.exportTransactionsCsv(context) }
        )
    }
}

@Composable
fun SearchBarView(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by bank, description, or ref #", color = Color(0xFF64748B), fontSize = 13.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFF334155),
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF1E293B)
        ),
        singleLine = true
    )
}

@Composable
fun PermissionBanner(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C2D12).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Enable SMS Tracking",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Allow reading bank SMS to auto-track expenses.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
            ) {
                Text("Allow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EmptyTransactionsView(
    filter: TransactionFilter,
    hasPermissions: Boolean,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = when (filter) {
                    TransactionFilter.ALL -> "No Transactions Yet"
                    TransactionFilter.UNREVIEWED -> "All Caught Up!"
                    TransactionFilter.EXPENSES -> "No Expenses Recorded"
                    TransactionFilter.INCOME -> "No Income Recorded"
                },
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (filter == TransactionFilter.UNREVIEWED) {
                    "All your transactions have descriptions and categories."
                } else {
                    "Tap 'Scan Past SMS' to import existing banking messages from your inbox, or wait for incoming bank alerts."
                },
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (filter != TransactionFilter.UNREVIEWED) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (hasPermissions) "Scan Past SMS Now" else "Grant Permission & Scan")
                }
            }
        }
    }
}
