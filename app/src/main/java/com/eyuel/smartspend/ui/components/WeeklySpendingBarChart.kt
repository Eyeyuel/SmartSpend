package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.ui.FormatUtils
import com.eyuel.smartspend.ui.model.DailySpendingBar

@Composable
fun WeeklySpendingBarChart(
    bars: List<DailySpendingBar>,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    val maxAmount = remember(bars) {
        val max = bars.maxOfOrNull { it.amount } ?: 1.0
        if (max <= 0.0) 1.0 else max
    }

    val total7DaySpent = remember(bars) {
        bars.sumOf { it.amount }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-Day Spending Trend",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Total: ${FormatUtils.formatAmount(total7DaySpent)} ETB",
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Last 7 Days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            val primaryColor = MaterialTheme.colorScheme.primary
            val barNormalColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            val barEmptyColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barCount = bars.size
                    val barWidth = 24.dp.toPx()
                    val spacing = (canvasWidth - (barWidth * barCount)) / (barCount + 1)
                    val minBarHeight = 6.dp.toPx()

                    bars.forEachIndexed { index, bar ->
                        val x = spacing + (index * (barWidth + spacing))
                        val heightFraction = (bar.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                        val calculatedHeight = (canvasHeight * heightFraction).coerceAtLeast(minBarHeight)
                        val y = canvasHeight - calculatedHeight

                        val barColor = when {
                            bar.isToday -> primaryColor
                            bar.amount > 0 -> barNormalColor
                            else -> barEmptyColor
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, calculatedHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day Labels Row below the chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                bars.forEach { bar ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = bar.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (bar.isToday) MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bar.dateNumber,
                            fontSize = 10.sp,
                            color = if (bar.isToday) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
