package com.eyuel.smartspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.ui.FormatUtils
import com.eyuel.smartspend.ui.model.ChartGranularity
import com.eyuel.smartspend.ui.model.DailySpendingBar
import com.eyuel.smartspend.ui.model.SpendingChartBar
import com.eyuel.smartspend.ui.model.SpendingChartData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeeklySpendingBarChart(
    chartData: SpendingChartData,
    onGranularitySelected: (ChartGranularity) -> Unit,
    onLoadMoreHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (chartData.bars.isEmpty()) return

    // Track tapped bar for detailed inspection
    var selectedBarId by remember(chartData.granularity) { mutableStateOf<String?>(null) }

    val lazyListState = rememberLazyListState()
    var previousBarCount by remember(chartData.granularity) { mutableIntStateOf(chartData.bars.size) }
    var hasScrolledToEnd by remember(chartData.granularity) { mutableStateOf(false) }

    val visibleCount = when (chartData.granularity) {
        ChartGranularity.DAILY_7 -> 7
        ChartGranularity.WEEKLY -> 6
        ChartGranularity.MONTHLY -> 6
        ChartGranularity.YEARLY -> 4
    }

    // Live viewport tracking: calculates metrics for bars actually in view
    val visibleBars by remember(chartData.bars, chartData.granularity) {
        derivedStateOf {
            val layout = lazyListState.layoutInfo
            val visibleInfo = layout.visibleItemsInfo
            if (visibleInfo.isEmpty()) {
                chartData.bars.takeLast(visibleCount)
            } else {
                val vStart = layout.viewportStartOffset
                val vEnd = layout.viewportEndOffset
                val inView = visibleInfo.filter { item ->
                    val center = item.offset + item.size / 2
                    center in vStart..vEnd
                }.mapNotNull { chartData.bars.getOrNull(it.index) }

                if (inView.isNotEmpty()) inView else chartData.bars.takeLast(visibleCount)
            }
        }
    }

    val visibleTotal = visibleBars.sumOf { it.amount }
    val visibleAvg = if (visibleBars.isNotEmpty()) visibleTotal / visibleBars.size else 0.0

    val animatedTotal by animateFloatAsState(
        targetValue = visibleTotal.toFloat(),
        animationSpec = tween(durationMillis = 180),
        label = "chart_visible_total"
    )
    val animatedAvg by animateFloatAsState(
        targetValue = visibleAvg.toFloat(),
        animationSpec = tween(durationMillis = 180),
        label = "chart_visible_avg"
    )
    val animatedAvgFraction by animateFloatAsState(
        targetValue = if (chartData.maxAmount > 0 && visibleAvg > 0) {
            val ratio = (visibleAvg / chartData.maxAmount).coerceIn(0.0, 1.0)
            Math.pow(ratio, 0.65).toFloat().coerceIn(0.12f, 1f)
        } else 0.12f,
        animationSpec = tween(durationMillis = 200),
        label = "chart_avg_y_fraction"
    )

    val isLatestWindow = visibleBars.lastOrNull()?.id == chartData.bars.lastOrNull()?.id
    val windowDateLabel = remember(visibleBars, chartData.granularity, isLatestWindow) {
        if (isLatestWindow) {
            when (chartData.granularity) {
                ChartGranularity.DAILY_7 -> "Last 7 Days"
                ChartGranularity.WEEKLY -> "Last 6 Weeks"
                ChartGranularity.MONTHLY -> "Last 6 Months"
                ChartGranularity.YEARLY -> "Recent Years"
            }
        } else {
            val first = visibleBars.firstOrNull()
            val last = visibleBars.lastOrNull()
            if (first != null && last != null) {
                when (chartData.granularity) {
                    ChartGranularity.DAILY_7 -> {
                        if (first.timestamp > 0 && last.timestamp > 0) {
                            val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                            "${fmt.format(Date(first.timestamp))} – ${fmt.format(Date(last.timestamp))}"
                        } else {
                            "${first.secondaryLabel} – ${last.secondaryLabel}"
                        }
                    }
                    ChartGranularity.WEEKLY -> {
                        if (first.timestamp > 0 && last.timestamp > 0) {
                            val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                            "${fmt.format(Date(first.timestamp))} – ${fmt.format(Date(last.timestamp))}"
                        } else {
                            "${first.primaryLabel} – ${last.primaryLabel}"
                        }
                    }
                    ChartGranularity.MONTHLY -> {
                        if (first.timestamp > 0 && last.timestamp > 0) {
                            val fmtMonthYear = SimpleDateFormat("MMM ''yy", Locale.getDefault())
                            "${fmtMonthYear.format(Date(first.timestamp))} – ${fmtMonthYear.format(Date(last.timestamp))}"
                        } else {
                            "${first.primaryLabel} – ${last.primaryLabel}"
                        }
                    }
                    ChartGranularity.YEARLY -> {
                        "${first.primaryLabel} – ${last.primaryLabel}"
                    }
                }
            } else ""
        }
    }

    val activeBar = chartData.bars.find { it.id == selectedBarId }
        ?: visibleBars.find { it.isCurrent }
        ?: visibleBars.lastOrNull()
        ?: chartData.bars.lastOrNull()

    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, outlineColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title and Granularity Toggle Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Title and Window Date Range Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Spending Trend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (windowDateLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.3f)),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = windowDateLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Row 2: Metrics Summary (Total Amount on Left, Avg Badge on Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Total: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = onSurfaceVariant
                        )
                        Text(
                            text = "${FormatUtils.formatAmount(animatedTotal.toDouble())} ETB",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Dynamic Average Badge Chip (Guaranteed single line)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = primaryColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = "Avg: ${FormatUtils.formatCompactAmount(animatedAvg.toDouble())} ETB${chartData.granularity.avgUnit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                        )
                    }
                }

                // Granularity Pill Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceVariant.copy(alpha = 0.45f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ChartGranularity.entries.forEach { granularity ->
                        val isSelected = granularity == chartData.granularity
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) primaryColor else Color.Transparent,
                            animationSpec = tween(180),
                            label = "chart_tab_bg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "chart_tab_text"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(bgColor)
                                .clickable {
                                    selectedBarId = null
                                    onGranularitySelected(granularity)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = granularity.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Interactive Selected Bar Inspection Capsule
            activeBar?.let { bar ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.4f)),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (bar.isCurrent) primaryColor else onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bar.fullTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${FormatUtils.formatAmount(bar.amount)} ETB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bar.amount > 0) primaryColor else onSurfaceVariant
                            )
                            if (chartData.totalAmount > 0 && bar.amount > 0) {
                                val pct = (bar.amount / chartData.totalAmount) * 100
                                val pctText = if (pct < 1.0 && pct > 0.0) {
                                    String.format(Locale.getDefault(), "%.1f%%", pct)
                                } else {
                                    "${pct.toInt()}%"
                                }
                                Text(
                                    text = " ($pctText)",
                                    fontSize = 10.sp,
                                    color = onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Native Smooth-Scrollable Bar Chart with LazyRow
            val bars = chartData.bars
            val maxAmount = chartData.maxAmount

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val availableWidth = maxWidth
                val columnWidth = (availableWidth / visibleCount.toFloat()).coerceAtLeast(46.dp)
                val barWidth = when (chartData.granularity) {
                    ChartGranularity.YEARLY -> 28.dp
                    ChartGranularity.DAILY_7 -> 20.dp
                    else -> 22.dp
                }.coerceAtMost(columnWidth * 0.62f)

                // Position viewport at the most recent period on initial load or tab switch
                LaunchedEffect(chartData.granularity) {
                    hasScrolledToEnd = false
                }

                LaunchedEffect(chartData.granularity, bars.size) {
                    if (bars.isNotEmpty() && !hasScrolledToEnd) {
                        lazyListState.scrollToItem(bars.size - 1)
                        hasScrolledToEnd = true
                        previousBarCount = bars.size
                    } else if (hasScrolledToEnd && bars.size > previousBarCount) {
                        // Prepend index compensation: keeps user viewport perfectly locked on scroll
                        val addedItems = bars.size - previousBarCount
                        val currentFirstIndex = lazyListState.firstVisibleItemIndex
                        val currentOffset = lazyListState.firstVisibleItemScrollOffset
                        lazyListState.scrollToItem(currentFirstIndex + addedItems, currentOffset)
                        previousBarCount = bars.size
                    }
                }

                // Detect when user actively scrolls near the left edge to fetch earlier chunk on demand
                LaunchedEffect(lazyListState) {
                    snapshotFlow {
                        Triple(lazyListState.firstVisibleItemIndex, lazyListState.isScrollInProgress, bars.size)
                    }.collect { (firstIndex, isScrolling, count) ->
                        if (isScrolling && firstIndex < 3 && count > 0) {
                            onLoadMoreHistory()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .drawBehind {
                            // Draw Average Benchmark Dashed Line across the chart background
                            if (animatedAvg > 0 && maxAmount > 0) {
                                val topPadding = 24.dp.toPx()
                                val bottomLabelsPadding = 48.dp.toPx()
                                val availableBarHeight = size.height - topPadding - bottomLabelsPadding
                                val barBaseY = size.height - bottomLabelsPadding
                                val avgY = barBaseY - (availableBarHeight * animatedAvgFraction).coerceAtLeast(6.dp.toPx())

                                drawLine(
                                    color = primaryColor.copy(alpha = 0.35f),
                                    start = Offset(4.dp.toPx(), avgY),
                                    end = Offset(size.width - 32.dp.toPx(), avgY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                )

                                val avgTagLayout = textMeasurer.measure(
                                    text = "Avg",
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor.copy(alpha = 0.75f)
                                    )
                                )
                                drawText(
                                    textLayoutResult = avgTagLayout,
                                    topLeft = Offset(
                                        size.width - avgTagLayout.size.width - 2.dp.toPx(),
                                        avgY - avgTagLayout.size.height / 2f
                                    )
                                )
                            }
                        }
                ) {
                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        items(
                            items = bars,
                            key = { it.id }
                        ) { bar ->
                            val isSelected = bar.id == activeBar?.id
                            Column(
                                modifier = Modifier
                                    .width(columnWidth)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedBarId = bar.id
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 1. Top Amount Label (Centered)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val compactAmount = FormatUtils.formatCompactAmount(bar.amount)
                                    Text(
                                        text = compactAmount,
                                        fontSize = if (isSelected) 10.sp else 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) primaryColor else if (bar.amount > 0) onSurfaceVariant else onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                }

                                // 2. Bar Graphic Area (Centered with soft contrast curve)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val rawRatio = if (maxAmount > 0) (bar.amount / maxAmount).coerceIn(0.0, 1.0) else 0.0
                                    val heightFraction = if (bar.amount > 0) {
                                        Math.pow(rawRatio, 0.65).toFloat().coerceIn(0.12f, 1f)
                                    } else {
                                        0.06f // subtle base pill
                                    }

                                    val barBrush = when {
                                        isSelected -> Brush.verticalGradient(
                                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.75f))
                                        )
                                        bar.isCurrent -> Brush.verticalGradient(
                                            colors = listOf(primaryColor.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.6f))
                                        )
                                        bar.amount > 0 -> SolidColor(onSurfaceVariant.copy(alpha = 0.5f))
                                        else -> SolidColor(surfaceVariant.copy(alpha = 0.45f))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight(fraction = heightFraction)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                            .background(barBrush)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        primaryColor.copy(alpha = 0.85f),
                                                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                                    )
                                                } else Modifier
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 3. Bottom Primary Label (Centered)
                                Text(
                                    text = bar.primaryLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected || bar.isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected || bar.isCurrent) primaryColor else onSurfaceVariant
                                )

                                // 4. Bottom Secondary Label (Centered)
                                if (bar.secondaryLabel.isNotEmpty()) {
                                    Text(
                                        text = bar.secondaryLabel,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected || bar.isCurrent) primaryColor.copy(alpha = 0.85f) else onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(11.dp))
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Backward-compatible overload for DailySpendingBar list
@Composable
fun WeeklySpendingBarChart(
    bars: List<DailySpendingBar>,
    modifier: Modifier = Modifier
) {
    val chartBars = remember(bars) {
        bars.mapIndexed { idx, b ->
            SpendingChartBar(
                id = "bar_$idx",
                primaryLabel = b.dayLabel,
                secondaryLabel = b.dateNumber,
                fullTitle = "${b.dayLabel}, ${b.dateNumber}",
                amount = b.amount,
                isCurrent = b.isToday
            )
        }
    }
    val chartData = remember(chartBars) {
        val total = chartBars.sumOf { it.amount }
        val avg = if (chartBars.isNotEmpty()) total / chartBars.size else 0.0
        val max = chartBars.maxOfOrNull { it.amount } ?: 1.0
        SpendingChartData(
            granularity = ChartGranularity.DAILY_7,
            bars = chartBars,
            totalAmount = total,
            averageAmount = avg,
            maxAmount = if (max <= 0.0) 1.0 else max,
            pages = 1
        )
    }
    WeeklySpendingBarChart(
        chartData = chartData,
        onGranularitySelected = {},
        onLoadMoreHistory = {},
        modifier = modifier
    )
}
