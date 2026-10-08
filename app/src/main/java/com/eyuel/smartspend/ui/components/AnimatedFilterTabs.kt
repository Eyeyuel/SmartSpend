package com.eyuel.smartspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.ui.TransactionFilter

@Composable
fun AnimatedFilterTabs(
    activeFilter: TransactionFilter,
    unreviewedCount: Int,
    onFilterSelected: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        TransactionFilter.ALL to "All",
        TransactionFilter.UNREVIEWED to "Pending",
        TransactionFilter.EXPENSES to "Expenses",
        TransactionFilter.INCOME to "Income"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = filter == activeFilter
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF2AABEE) else Color.Transparent,
                label = "tab_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFilterSelected(filter) }
                    .padding(vertical = 8.dp), // Thinner tabs
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 11.sp, // Smaller font for elegance
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    if (filter == TransactionFilter.UNREVIEWED && unreviewedCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0xFFF59E0B))
                        )
                    }
                }
            }
        }
    }
}
