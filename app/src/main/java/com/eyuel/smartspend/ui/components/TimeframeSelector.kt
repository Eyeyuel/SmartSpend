package com.eyuel.smartspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.domain.model.TimeframePeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimeframeSelector(
    activeTimeframe: TimeframePeriod,
    customDateRange: Pair<Long, Long>? = null,
    onTimeframeSelected: (TimeframePeriod) -> Unit,
    onOpenCustomDatePicker: () -> Unit = {},
    onClearCustomDateRange: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    val presets = listOf(
        TimeframePeriod.TODAY,
        TimeframePeriod.THIS_WEEK,
        TimeframePeriod.THIS_MONTH,
        TimeframePeriod.ALL_TIME
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, outlineColor),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Preset Buttons: Today, This Week, This Month, All Time
                presets.forEach { period ->
                    val isSelected = period == activeTimeframe

                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) surfaceVariant else Color.Transparent,
                        label = "timeframe_bg"
                    )
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) onSurface else onSurfaceVariant,
                        label = "timeframe_text"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(animatedBgColor)
                            .clickable { onTimeframeSelected(period) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedTextColor
                        )
                    }
                }

                // 2. Calendar Action Pill (Custom Range)
                val isCustomSelected = activeTimeframe == TimeframePeriod.CUSTOM
                val animatedCalBg by animateColorAsState(
                    targetValue = if (isCustomSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
                    label = "cal_pill_bg"
                )
                val animatedCalTint by animateColorAsState(
                    targetValue = if (isCustomSelected) primaryColor else onSurfaceVariant,
                    label = "cal_pill_tint"
                )

                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(animatedCalBg)
                        .clickable { onOpenCustomDatePicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Custom Date Range",
                        tint = animatedCalTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Active Custom Range Banner Capsule
        AnimatedVisibility(visible = activeTimeframe == TimeframePeriod.CUSTOM && customDateRange != null) {
            customDateRange?.let { range ->
                val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                val startStr = fmt.format(Date(range.first))
                val endStr = fmt.format(Date(range.second))
                val daysCount = maxOf(1, ((range.second - range.first + 1000L) / (24 * 60 * 60 * 1000L)).toInt())

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryColor.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenCustomDatePicker() }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$startStr – $endStr ($daysCount days)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onClearCustomDateRange() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Clear ✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
