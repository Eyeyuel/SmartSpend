package com.eyuel.smartspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun TimeframeSelector(
    activeTimeframe: TimeframePeriod,
    onTimeframeSelected: (TimeframePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF131B2A),
        border = BorderStroke(1.dp, Color(0xFF1E2A3F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeframePeriod.entries.forEach { period ->
                val isSelected = period == activeTimeframe

                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF25344D) else Color.Transparent,
                    label = "timeframe_bg"
                )
                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF94A3B8),
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
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = animatedTextColor
                    )
                }
            }
        }
    }
}
