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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Insights
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
import com.eyuel.smartspend.domain.model.TimeframePeriod
import com.eyuel.smartspend.ui.DashboardUiState
import com.eyuel.smartspend.ui.FormatUtils
import com.eyuel.smartspend.ui.TransactionFilter
import com.eyuel.smartspend.ui.TransactionListViewModel
import com.eyuel.smartspend.ui.components.*
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

    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F17),
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
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (hasSmsPermissions) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasSmsPermissions) "SMS Tracking Active" else "Permissions Required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hasSmsPermissions) Color(0xFF34D399) else Color(0xFFFBBF24)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F17))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0E1524),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger") },
                    label = { Text("Ledger", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF25344D),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Analytics") },
                    label = { Text("Insights", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF25344D),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    ) { innerPadding ->
        if (uiState.selectedTab == 0) {
            // TAB 0: LEDGER & REVIEW SCREEN
            LedgerScreen(
                uiState = uiState,
                hasSmsPermissions = hasSmsPermissions,
                viewModel = viewModel,
                onRequestPermissions = {
                    val perms = mutableListOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // TAB 1: ANALYTICS & INTELLIGENCE SCREEN
            AnalyticsScreen(
                uiState = uiState,
                onExportCsv = { viewModel.exportTransactionsCsv(context) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
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
}

@Composable
fun LedgerScreen(
    uiState: DashboardUiState,
    hasSmsPermissions: Boolean,
    viewModel: TransactionListViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (!hasSmsPermissions) {
            item {
                PermissionBanner(onRequestPermissions = onRequestPermissions)
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
                    if (hasSmsPermissions) viewModel.syncHistoricalSms()
                    else onRequestPermissions()
                }
            )
        }

        // Timeframe Selector: Today | This Week | This Month | All Time
        item {
            TimeframeSelector(
                activeTimeframe = uiState.activeTimeframe,
                onTimeframeSelected = { viewModel.setTimeframe(it) }
            )
        }

        // Category Spending Carousel (Tap-to-filter)
        if (uiState.categoryBreakdown.isNotEmpty()) {
            item {
                CategorySpendingCarousel(
                    categories = uiState.categoryBreakdown,
                    selectedCategory = uiState.selectedCategoryFilter,
                    onCategoryClick = { viewModel.toggleCategoryFilter(it) }
                )
            }
        }

        // Filter Bar (All, Needs Note (X), Expenses, Income)
        item {
            FilterBar(
                activeFilter = uiState.activeFilter,
                unreviewedCount = uiState.unreviewedCount,
                onFilterSelected = { viewModel.setFilter(it) }
            )
        }

        // Stream Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (uiState.activeFilter) {
                        TransactionFilter.ALL -> "Transaction Stream"
                        TransactionFilter.UNREVIEWED -> "Pending Review"
                        TransactionFilter.EXPENSES -> "Expenses"
                        TransactionFilter.INCOME -> "Income"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${uiState.transactions.size} records",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Empty State or Date-Grouped Transaction Stream
        if (uiState.transactions.isEmpty()) {
            item {
                EmptyTransactionsView(
                    filter = uiState.activeFilter,
                    timeframe = uiState.activeTimeframe,
                    hasPermissions = hasSmsPermissions,
                    onScanClick = {
                        if (hasSmsPermissions) viewModel.syncHistoricalSms()
                        else onRequestPermissions()
                    }
                )
            }
        } else {
            // Render Date-Grouped Stream
            uiState.groupedTransactions.forEach { group ->
                item(key = "header_${group.dayEpoch}") {
                    DateGroupHeader(
                        dateLabel = group.dateLabel,
                        dailySpent = group.dailySpent,
                        dailyIncome = group.dailyIncome
                    )
                }

                items(
                    items = group.transactions,
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
}

@Composable
fun AnalyticsScreen(
    uiState: DashboardUiState,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // 7-Day Canvas Spending Bar Chart
        item {
            WeeklySpendingBarChart(bars = uiState.weeklyChartBars)
        }

        // Smart Spending Insights Card
        uiState.insights?.let { insights ->
            item {
                SpendingInsightsCard(insights = insights)
            }
        }

        // Category Spending Detailed Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                border = BorderStroke(1.dp, Color(0xFF223048))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Distribution",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = uiState.activeTimeframe.displayName,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uiState.categoryBreakdown.isEmpty()) {
                        Text(
                            text = "No expenses recorded in ${uiState.activeTimeframe.displayName.lowercase()} yet.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.categoryBreakdown.forEach { item ->
                                val style = com.eyuel.smartspend.ui.CategoryVisuals.getStyle(item.category)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(style.containerColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = style.icon,
                                                    contentDescription = item.category,
                                                    tint = style.color,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.category,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }

                                        Text(
                                            text = "${FormatUtils.formatAmount(item.totalAmount)} ETB (${(item.percentage * 100).toInt()}%)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { item.percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = style.color,
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export Data Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                border = BorderStroke(1.dp, Color(0xFF223048))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Transaction Ledger",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Save as .csv spreadsheet for Excel or Google Sheets",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarView(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF131B2A),
        border = BorderStroke(1.dp, Color(0xFF1E2A3F))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by bank, note, or ref #", color = Color(0xFF64748B), fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
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
    timeframe: TimeframePeriod,
    hasPermissions: Boolean,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
        border = BorderStroke(1.dp, Color(0xFF1E2A3F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    filter == TransactionFilter.UNREVIEWED -> "All Caught Up!"
                    filter == TransactionFilter.EXPENSES -> "No Expenses ${timeframe.displayName}"
                    filter == TransactionFilter.INCOME -> "No Income ${timeframe.displayName}"
                    timeframe == TimeframePeriod.TODAY -> "No Transactions Today"
                    else -> "No Transactions ${timeframe.displayName}"
                },
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    filter == TransactionFilter.UNREVIEWED ->
                        "All transactions have descriptions and categories."
                    timeframe == TimeframePeriod.TODAY ->
                        "No expenses or income tracked yet today. Switch to 'This Week' or 'This Month' above to view previous records, or new transactions will appear as SMS arrive."
                    else ->
                        "Tap 'Scan SMS' to import existing banking messages, or wait for incoming bank alerts."
                },
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
            )

            if (filter != TransactionFilter.UNREVIEWED && timeframe != TimeframePeriod.TODAY) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (hasPermissions) "Scan Past SMS Now" else "Grant Permission & Scan")
                }
            }
        }
    }
}
