package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.theme.ExpenseRed
import com.eyuel.smartspend.ui.theme.IncomeGreen
import java.util.*

private val CategoryColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6), // Purple
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Orange
    Color(0xFF64748B)  // Slate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsBottomSheet(
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit
) {
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    }

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
    }

    val categoryBreakdown = remember(transactions) {
        val expenses = transactions.filter { it.type == TransactionType.DEBIT }
        val grouped = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        grouped
    }

    val bankBreakdown = remember(transactions) {
        transactions.groupBy { it.bankName }
            .mapValues { entry -> entry.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF64748B)) }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Spending Analytics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Category & Banking Breakdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // CSV Export Button
                    FilledTonalButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Quick Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Outflow", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "-${String.format(Locale.US, "%,.2f", totalExpense)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color(0xFF334155))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Inflow", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = "+${String.format(Locale.US, "%,.2f", totalIncome)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }
                }
            }

            // Category Breakdown Section
            item {
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (categoryBreakdown.isEmpty()) {
                item {
                    Text(
                        text = "No expenses recorded yet to show category distribution.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                items(categoryBreakdown) { (category, amount) ->
                    val percentage = if (totalExpense > 0) (amount / totalExpense) else 0.0
                    val colorIndex = Math.abs(category.hashCode()) % CategoryColors.size
                    val color = CategoryColors[colorIndex]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "${String.format(Locale.US, "%,.2f", amount)} ETB (${String.format(Locale.US, "%.1f", percentage * 100)}%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Linear Progress Bar
                            LinearProgressIndicator(
                                progress = { percentage.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = color,
                                trackColor = Color(0xFF334155),
                            )
                        }
                    }
                }
            }

            // Bank Distribution Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Activity by Institution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bankBreakdown.forEach { (bank, count) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF334155),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bank,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
