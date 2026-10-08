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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartSpendTheme {
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

    val pagerState = rememberPagerState(
        initialPage = uiState.selectedTab,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    // Sync BottomNav -> Pager
    LaunchedEffect(uiState.selectedTab) {
        if (pagerState.currentPage != uiState.selectedTab) {
            pagerState.animateScrollToPage(uiState.selectedTab)
        }
    }
    var isSplashShowing by remember { mutableStateOf(true) }
    var isBalanceVisible by remember { mutableStateOf(false) }

    // Sync Pager -> BottomNav
    LaunchedEffect(pagerState.currentPage) {
        if (uiState.selectedTab != pagerState.currentPage) {
            viewModel.setSelectedTab(pagerState.currentPage)
        }
    }

    if (isSplashShowing) {
        com.eyuel.smartspend.ui.components.SplashScreen(
            onSplashComplete = { isSplashShowing = false }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SmartSpendBottomNavBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> 
                    viewModel.setSelectedTab(tab)
                    coroutineScope.launch { pagerState.animateScrollToPage(tab) }
                },
                unreviewedCount = uiState.unreviewedCount
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) { page ->
            when (page) {
                0 -> {
                    // TAB 0: LEDGER & REVIEW SCREEN
                    LedgerScreen(
                        uiState = uiState,
                        isBalanceVisible = isBalanceVisible,
                        onToggleVisibility = { isBalanceVisible = !isBalanceVisible },
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    // TAB 1: ANALYTICS & INTELLIGENCE SCREEN
                    AnalyticsScreen(
                        uiState = uiState,
                        isBalanceVisible = isBalanceVisible,
                        onToggleVisibility = { isBalanceVisible = !isBalanceVisible },
                        onExportCsv = { viewModel.exportTransactionsCsv(context) },
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
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
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerScreen(
    uiState: DashboardUiState,
    isBalanceVisible: Boolean,
    onToggleVisibility: () -> Unit,
    hasSmsPermissions: Boolean,
    viewModel: TransactionListViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        if (!hasSmsPermissions) {
            item {
                PermissionBanner(onRequestPermissions = onRequestPermissions)
            }
        }

        item(key = "wallet_header") {
            com.eyuel.smartspend.ui.components.PremiumWalletHeader(
                totalBalance = uiState.ledgerTotalBalance,
                isBalanceVisible = isBalanceVisible,
                onToggleVisibility = onToggleVisibility,
                isSyncing = uiState.isSyncing,
                onSyncClick = {
                    if (hasSmsPermissions) viewModel.syncHistoricalSms()
                    else onRequestPermissions()
                },
                modifier = Modifier.animateItem()
            )
        }

        item(key = "filter_tabs") {
            com.eyuel.smartspend.ui.components.AnimatedFilterTabs(
                activeFilter = uiState.activeFilter,
                unreviewedCount = uiState.unreviewedCount,
                onFilterSelected = { viewModel.setFilter(it) },
                modifier = Modifier.padding(bottom = 8.dp).animateItem()
            )
        }

        // Empty State or Date-Grouped Transaction Stream
        if (uiState.transactions.isEmpty()) {
            item(key = "empty_state") {
                EmptyTransactionsView(
                    filter = uiState.activeFilter,
                    timeframe = uiState.activeTimeframe,
                    hasPermissions = hasSmsPermissions,
                    onScanClick = {
                        if (hasSmsPermissions) viewModel.syncHistoricalSms()
                        else onRequestPermissions()
                    },
                    modifier = Modifier.animateItem()
                )
            }
        } else {
            // Render Date-Grouped Stream
            uiState.groupedTransactions.forEach { group ->
                stickyHeader(key = "header_${group.dayEpoch}") {
                    DateGroupHeader(
                        dateLabel = group.dateLabel,
                        dailySpent = group.dailySpent,
                        dailyIncome = group.dailyIncome,
                        modifier = Modifier.animateItem()
                    )
                }

                items(
                    items = group.transactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onClick = { viewModel.openEdit(transaction) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnalyticsScreen(
    uiState: DashboardUiState,
    isBalanceVisible: Boolean,
    onToggleVisibility: () -> Unit,
    viewModel: TransactionListViewModel,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Timeframe Selector
        stickyHeader {
            Surface(
                color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
            ) {
                TimeframeSelector(
                    activeTimeframe = uiState.activeTimeframe,
                    onTimeframeSelected = { viewModel.setTimeframe(it) }
                )
            }
        }

        // 2. High-Level Net Cashflow Summary Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Net Cashflow",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val net = uiState.totalIncome - uiState.totalExpense
                val netColor = if (net >= 0) Color(0xFF34D399) else Color(0xFFFB7185)
                Text(
                    text = if (isBalanceVisible) "${if (net > 0) "+" else ""}${FormatUtils.formatAmount(net)} ETB" else "••••••",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = netColor,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleVisibility
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), 
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Income", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceVisible) FormatUtils.formatAmount(uiState.totalIncome) else "••••••",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onToggleVisibility
                            )
                        )
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.outline)) // Divider
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expenses", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceVisible) FormatUtils.formatAmount(uiState.totalExpense) else "••••••",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onToggleVisibility
                            )
                        )
                    }
                }
            }
        }

        // 3. Trend Chart (Weekly Spending)
        item {
            WeeklySpendingBarChart(bars = uiState.weeklyChartBars)
        }

        // 4. Smart AI Insights
        uiState.insights?.let { insights ->
            item {
                SpendingInsightsCard(insights = insights)
            }
        }

        // 5. Category Breakdown (Polished)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Distribution",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = uiState.activeTimeframe.displayName,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.categoryBreakdown.isEmpty()) {
                        Text(
                            text = "No expenses recorded in ${uiState.activeTimeframe.displayName.lowercase()} yet.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(style.color.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = style.icon,
                                                    contentDescription = item.category,
                                                    tint = style.color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = item.category,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${FormatUtils.formatAmount(item.totalAmount)} ETB",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${(item.percentage * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { item.percentage.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = style.color,
                                        trackColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.analyticsGroupedTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "Transactions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            uiState.analyticsGroupedTransactions.forEach { group ->
                stickyHeader(key = "analytics_header_${group.dayEpoch}") {
                    DateGroupHeader(
                        dateLabel = group.dateLabel,
                        dailySpent = group.dailySpent,
                        dailyIncome = group.dailyIncome,
                        modifier = Modifier.animateItem()
                    )
                }

                items(
                    items = group.transactions,
                    key = { "analytics_${it.id}" }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onClick = { viewModel.openEdit(transaction) },
                        modifier = Modifier.animateItem()
                    )
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
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by bank, note, or ref #", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f))
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
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
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
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
