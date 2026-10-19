package com.eyuel.smartspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.CashflowEngine
import com.eyuel.smartspend.ui.CategoryVisuals
import com.eyuel.smartspend.ui.DateGroupingUtils
import com.eyuel.smartspend.ui.FormatUtils
import com.eyuel.smartspend.ui.model.CashflowBucket
import com.eyuel.smartspend.ui.model.CashflowGranularity
import com.eyuel.smartspend.ui.model.DateGroupedTransactions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CashflowExplorerModal(
    allTransactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    initialGranularity: CashflowGranularity = CashflowGranularity.DAILY
) {
    var selectedGranularity by remember(initialGranularity) { mutableStateOf(initialGranularity) }

    val buckets = remember(allTransactions, selectedGranularity) {
        CashflowEngine.buildBuckets(allTransactions, selectedGranularity)
    }

    var selectedBucketId by remember(selectedGranularity, buckets) {
        mutableStateOf(buckets.lastOrNull()?.id)
    }

    // Keep selected bucket in sync when buckets list changes
    val selectedBucket = remember(buckets, selectedBucketId) {
        buckets.firstOrNull { it.id == selectedBucketId } ?: buckets.lastOrNull()
    }

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val filteredTransactions = remember(selectedBucket?.transactions, selectedCategoryFilter) {
        val txs = selectedBucket?.transactions ?: emptyList()
        if (selectedCategoryFilter != null) {
            txs.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        } else {
            txs
        }
    }

    val dateGroupedTransactions = remember(filteredTransactions) {
        filteredTransactions
            .groupBy { DateGroupingUtils.getDayStartMillis(it.timestamp) }
            .map { (dayStart, txs) ->
                val daySpent = txs
                    .filter { it.type == TransactionType.DEBIT && it.category != ExpenseCategory.INTERNAL_TRANSFER }
                    .sumOf { it.amount }
                val dayIncome = txs
                    .filter { it.type == TransactionType.CREDIT && it.category != ExpenseCategory.INTERNAL_TRANSFER }
                    .sumOf { it.amount }
                DateGroupedTransactions(
                    dateLabel = DateGroupingUtils.getDateLabel(dayStart),
                    dayEpoch = dayStart,
                    dailySpent = daySpent,
                    dailyIncome = dayIncome,
                    transactions = txs.sortedByDescending { it.timestamp }
                )
            }
            .sortedByDescending { it.dayEpoch }
    }

    val categoryInspectionStats = remember(selectedBucket, selectedCategoryFilter) {
        val cat = selectedCategoryFilter ?: return@remember null
        val catTxs = selectedBucket?.transactions?.filter {
            it.category.equals(cat, ignoreCase = true)
        } ?: emptyList()
        val totalSpent = catTxs.filter { it.type == TransactionType.DEBIT && it.category != ExpenseCategory.INTERNAL_TRANSFER }.sumOf { it.amount }
        val totalIncome = catTxs.filter { it.type == TransactionType.CREDIT && it.category != ExpenseCategory.INTERNAL_TRANSFER }.sumOf { it.amount }
        val count = catTxs.size
        val avg = if (count > 0) (if (totalSpent > 0) totalSpent else totalIncome) / count else 0.0
        val maxAmount = catTxs.maxOfOrNull { it.amount } ?: 0.0
        val periodExpense = selectedBucket?.expense ?: 0.0
        val percentage = if (periodExpense > 0 && totalSpent > 0) (totalSpent / periodExpense).toFloat() else 0f
        CategoryInspectionStats(
            category = cat,
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            count = count,
            avg = avg,
            maxAmount = maxAmount,
            percentage = percentage
        )
    }

    val lazyRowState = rememberLazyListState(
        initialFirstVisibleItemIndex = (buckets.size - 1).coerceAtLeast(0)
    )

    // Auto-scroll to latest period on granularity change
    LaunchedEffect(selectedGranularity, buckets.size) {
        if (buckets.isNotEmpty()) {
            val lastIdx = buckets.size - 1
            selectedBucketId = buckets[lastIdx].id
            lazyRowState.scrollToItem(lastIdx)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cashflow Explorer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Income vs Expense Dual Analyzer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF2AABEE).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF2AABEE).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = selectedGranularity.title,
                            color = Color(0xFF2AABEE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Granularity Switcher Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CashflowGranularity.entries.forEach { gran ->
                        val isSelected = gran == selectedGranularity
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF2AABEE) else Color.Transparent)
                                .clickable { selectedGranularity = gran }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = gran.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Dual-Bar Timeline Chart
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Chart Legend
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Inflow vs Outflow Timeline",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF34D399))
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFB7185))
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Horizontal Timeline LazyRow
                                LazyRow(
                                    state = lazyRowState,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    items(buckets, key = { it.id }) { bucket ->
                                        val isSelected = bucket.id == selectedBucket?.id

                                        // Direct comparison between Income and Expense for this specific period
                                        val periodMax = maxOf(bucket.income, bucket.expense)
                                        val dominantScale = 0.90f
                                        val incomeHeightFactor = if (periodMax > 0 && bucket.income > 0) {
                                            ((bucket.income / periodMax).toFloat() * dominantScale).coerceIn(0.06f, 1f)
                                        } else 0f

                                        val expenseHeightFactor = if (periodMax > 0 && bucket.expense > 0) {
                                            ((bucket.expense / periodMax).toFloat() * dominantScale).coerceIn(0.06f, 1f)
                                        } else 0f

                                        val animIncomeRatio by animateFloatAsState(
                                            targetValue = incomeHeightFactor,
                                            animationSpec = spring(stiffness = 300f),
                                            label = "income_bar"
                                        )
                                        val animExpenseRatio by animateFloatAsState(
                                            targetValue = expenseHeightFactor,
                                            animationSpec = spring(stiffness = 300f),
                                            label = "expense_bar"
                                        )

                                        Surface(
                                            onClick = { selectedBucketId = bucket.id },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Color(0xFF2AABEE).copy(alpha = 0.12f) else MaterialTheme.colorScheme.background,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF2AABEE) else MaterialTheme.colorScheme.outline
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .width(62.dp)
                                                    .padding(vertical = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = bucket.primaryLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFF2AABEE) else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (bucket.secondaryLabel.isNotBlank()) {
                                                    Text(
                                                        text = bucket.secondaryLabel,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Dual Bar Container
                                                Box(
                                                    modifier = Modifier
                                                        .width(44.dp)
                                                        .height(110.dp),
                                                    contentAlignment = Alignment.BottomCenter
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxSize(),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.Bottom
                                                    ) {
                                                        // Income Bar (Emerald)
                                                        Box(
                                                            modifier = Modifier
                                                                .width(12.dp)
                                                                .fillMaxHeight(fraction = if (bucket.income > 0) animIncomeRatio.coerceAtLeast(0.06f) else 0.04f)
                                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                                .background(
                                                                    if (bucket.income > 0) Color(0xFF34D399)
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                                                )
                                                        )

                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        // Expense Bar (Rose)
                                                        Box(
                                                            modifier = Modifier
                                                                .width(12.dp)
                                                                .fillMaxHeight(fraction = if (bucket.expense > 0) animExpenseRatio.coerceAtLeast(0.06f) else 0.04f)
                                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                                .background(
                                                                    if (bucket.expense > 0) Color(0xFFFB7185)
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                                                )
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Mini Net Indicator
                                                val netColor = when {
                                                    bucket.income > bucket.expense -> Color(0xFF34D399)
                                                    bucket.expense > bucket.income -> Color(0xFFFB7185)
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(netColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Selected Period Summary Hero Card
                    selectedBucket?.let { bucket ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = bucket.fullPeriodTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val netColor = if (bucket.net >= 0) Color(0xFF34D399) else Color(0xFFFB7185)
                                    val netPrefix = if (bucket.net > 0) "+" else ""
                                    Text(
                                        text = "$netPrefix${FormatUtils.formatAmount(bucket.net)} ETB",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp,
                                        color = netColor
                                    )
                                    Text(
                                        text = if (bucket.net >= 0) "Net Surplus" else "Net Deficit",
                                        fontSize = 12.sp,
                                        color = netColor.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // 3 Columns: Inflow | Outflow | Savings Rate
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Inflow
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = Color(0xFF34D399),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Inflow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${FormatUtils.formatAmount(bucket.income)} ETB",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(28.dp)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )

                                        // Outflow
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFB7185),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Outflow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${FormatUtils.formatAmount(bucket.expense)} ETB",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(28.dp)
                                                .background(MaterialTheme.colorScheme.outline)
                                        )

                                        // Savings Rate
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Savings,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2AABEE),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Saved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (bucket.income > 0) "${bucket.savingsRate.toInt()}%" else "0%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF2AABEE)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Category Breakdown for Selected Period (if any expenses)
                        if (bucket.categoryBreakdown.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Spending by Category",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (selectedCategoryFilter != null) {
                                                Surface(
                                                    onClick = { selectedCategoryFilter = null },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Clear Filter",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF38BDF8),
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            tint = Color(0xFF38BDF8),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = "Tap to filter",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF38BDF8),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            bucket.categoryBreakdown.forEach { item ->
                                                val isSelected = selectedCategoryFilter.equals(item.category, ignoreCase = true)
                                                val style = CategoryVisuals.getStyle(item.category)

                                                Surface(
                                                    onClick = {
                                                        selectedCategoryFilter = if (isSelected) null else item.category
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) style.color.copy(alpha = 0.12f) else Color.Transparent,
                                                    border = if (isSelected) BorderStroke(1.5.dp, style.color) else null,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(26.dp)
                                                                        .clip(RoundedCornerShape(7.dp))
                                                                        .background(style.color.copy(alpha = 0.18f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = style.icon,
                                                                        contentDescription = item.category,
                                                                        tint = style.color,
                                                                        modifier = Modifier.size(15.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = item.category,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                if (isSelected) {
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Surface(
                                                                        shape = CircleShape,
                                                                        color = style.color
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = "Selected",
                                                                            tint = Color.Black,
                                                                            modifier = Modifier
                                                                                .padding(2.dp)
                                                                                .size(10.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Text(
                                                                text = "${FormatUtils.formatAmount(item.amount)} ETB (${(item.percentage * 100).toInt()}%)",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (isSelected) style.color else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        LinearProgressIndicator(
                                                            progress = { item.percentage.coerceIn(0f, 1f) },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(5.dp)
                                                                .clip(RoundedCornerShape(3.dp)),
                                                            color = style.color,
                                                            trackColor = MaterialTheme.colorScheme.background
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Category Inspection Card (if a category is selected)
                        categoryInspectionStats?.let { stats ->
                            item {
                                val style = CategoryVisuals.getStyle(stats.category)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, style.color.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(style.color.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = style.icon,
                                                        contentDescription = stats.category,
                                                        tint = style.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = stats.category,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${bucket.primaryLabel} • ${bucket.secondaryLabel}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (stats.totalSpent > 0) "-${FormatUtils.formatAmount(stats.totalSpent)} ETB"
                                                       else "+${FormatUtils.formatAmount(stats.totalIncome)} ETB",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (stats.totalSpent > 0) Color(0xFFFB7185) else Color(0xFF34D399)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${stats.count} ${if (stats.count == 1) "tx" else "txs"}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Avg: ${FormatUtils.formatAmount(stats.avg)} ETB",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Max: ${FormatUtils.formatAmount(stats.maxAmount)} ETB",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (stats.percentage > 0) {
                                                Text(
                                                    text = "${(stats.percentage * 100).toInt()}% share",
                                                    fontSize = 11.sp,
                                                    color = style.color,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Header for Transactions List
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedCategoryFilter != null) {
                                        "$selectedCategoryFilter (${filteredTransactions.size})"
                                    } else {
                                        "Transactions (${filteredTransactions.size})"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to edit",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2AABEE),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (dateGroupedTransactions.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (selectedCategoryFilter != null) {
                                                "No $selectedCategoryFilter transactions in this period."
                                            } else {
                                                "No transactions recorded in this period."
                                            },
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        if (selectedCategoryFilter != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(onClick = { selectedCategoryFilter = null }) {
                                                Text("Show All Categories", color = Color(0xFF38BDF8), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            dateGroupedTransactions.forEach { group ->
                                stickyHeader(key = "header_${group.dayEpoch}_${selectedCategoryFilter ?: "all"}") {
                                    DateGroupHeader(
                                        dateLabel = group.dateLabel,
                                        dailySpent = group.dailySpent,
                                        dailyIncome = group.dailyIncome
                                    )
                                }

                                items(group.transactions, key = { it.id }) { tx ->
                                    TransactionItemCard(
                                        transaction = tx,
                                        onClick = { onTransactionClick(tx) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CategoryInspectionStats(
    val category: String,
    val totalSpent: Double,
    val totalIncome: Double,
    val count: Int,
    val avg: Double,
    val maxAmount: Double,
    val percentage: Float
)
