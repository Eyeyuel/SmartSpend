package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.ui.TransactionFilter

@Composable
fun FilterBar(
    activeFilter: TransactionFilter,
    unreviewedCount: Int,
    onFilterSelected: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterTabChip(
            title = "All",
            icon = Icons.Default.Layers,
            isSelected = activeFilter == TransactionFilter.ALL,
            accentColor = Color(0xFF38BDF8),
            onClick = { onFilterSelected(TransactionFilter.ALL) }
        )

        FilterTabChip(
            title = if (unreviewedCount > 0) "Needs Note ($unreviewedCount)" else "Needs Note",
            icon = Icons.Default.EditNote,
            isSelected = activeFilter == TransactionFilter.UNREVIEWED,
            accentColor = Color(0xFFF59E0B),
            hasHighlight = unreviewedCount > 0,
            onClick = { onFilterSelected(TransactionFilter.UNREVIEWED) }
        )

        FilterTabChip(
            title = "Expenses",
            icon = Icons.Default.ArrowUpward,
            isSelected = activeFilter == TransactionFilter.EXPENSES,
            accentColor = Color(0xFFFB7185),
            onClick = { onFilterSelected(TransactionFilter.EXPENSES) }
        )

        FilterTabChip(
            title = "Income",
            icon = Icons.Default.ArrowDownward,
            isSelected = activeFilter == TransactionFilter.INCOME,
            accentColor = Color(0xFF34D399),
            onClick = { onFilterSelected(TransactionFilter.INCOME) }
        )
    }
}

@Composable
private fun FilterTabChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    hasHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            accentColor.copy(alpha = 0.2f)
        } else if (hasHighlight) {
            Color(0xFF451A03).copy(alpha = 0.4f)
        } else {
            Color(0xFF131B2A)
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) accentColor else if (hasHighlight) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color(0xFF1E2A3F)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else if (hasHighlight) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else if (hasHighlight) Color(0xFFFBBF24) else Color(0xFF94A3B8)
            )
        }
    }
}
