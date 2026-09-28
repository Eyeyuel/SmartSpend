package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.ui.FormatUtils
import com.eyuel.smartspend.ui.model.SpendingInsights
import com.eyuel.smartspend.ui.theme.ExpenseRed

@Composable
fun SpendingInsightsCard(
    insights: SpendingInsights,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Financial Intelligence",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightRow(
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFF97316),
                    title = "Daily Burn Rate",
                    value = "${FormatUtils.formatAmount(insights.dailyAverage)} ETB / day"
                )

                if (insights.largestExpense != null) {
                    InsightRow(
                        icon = Icons.Default.TrendingUp,
                        iconColor = ExpenseRed,
                        title = "Peak Outflow",
                        value = "-${FormatUtils.formatAmount(insights.largestExpense.amount)} ETB (${insights.largestExpense.bankName})"
                    )
                }

                if (insights.topCategory != null) {
                    InsightRow(
                        icon = Icons.Default.Category,
                        iconColor = Color(0xFF6366F1),
                        title = "Top Category",
                        value = insights.topCategory
                    )
                }

                if (insights.topBankSource != null) {
                    InsightRow(
                        icon = Icons.Default.AccountBalance,
                        iconColor = Color(0xFF10B981),
                        title = "Primary Institution",
                        value = insights.topBankSource
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
