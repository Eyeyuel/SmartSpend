package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.ui.FormatUtils
import com.eyuel.smartspend.ui.theme.ExpenseRed
import com.eyuel.smartspend.ui.theme.IncomeGreen

@Composable
fun DateGroupHeader(
    dateLabel: String,
    dailySpent: Double,
    dailyIncome: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCBD5E1)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dailyIncome > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(IncomeGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+${FormatUtils.formatAmount(dailyIncome)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
            }

            if (dailySpent > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ExpenseRed.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "-${FormatUtils.formatAmount(dailySpent)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}
