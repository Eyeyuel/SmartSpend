package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.ui.TransactionFilter
import com.eyuel.smartspend.ui.theme.PendingOrange

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
            isSelected = activeFilter == TransactionFilter.ALL,
            onClick = { onFilterSelected(TransactionFilter.ALL) }
        )

        FilterTabChip(
            title = if (unreviewedCount > 0) "Needs Note ($unreviewedCount)" else "Needs Note",
            isSelected = activeFilter == TransactionFilter.UNREVIEWED,
            highlightColor = if (unreviewedCount > 0) PendingOrange else null,
            onClick = { onFilterSelected(TransactionFilter.UNREVIEWED) }
        )

        FilterTabChip(
            title = "Expenses",
            isSelected = activeFilter == TransactionFilter.EXPENSES,
            onClick = { onFilterSelected(TransactionFilter.EXPENSES) }
        )

        FilterTabChip(
            title = "Income",
            isSelected = activeFilter == TransactionFilter.INCOME,
            onClick = { onFilterSelected(TransactionFilter.INCOME) }
        )
    }
}

@Composable
private fun FilterTabChip(
    title: String,
    isSelected: Boolean,
    highlightColor: Color? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF1E293B),
            labelColor = highlightColor ?: Color(0xFF94A3B8),
            selectedContainerColor = highlightColor?.copy(alpha = 0.25f) ?: MaterialTheme.colorScheme.primary,
            selectedLabelColor = highlightColor ?: Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) (highlightColor ?: MaterialTheme.colorScheme.primary) else Color(0xFF334155)
        )
    )
}
