package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.CategoryVisuals
import com.eyuel.smartspend.ui.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionBottomSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (id: Long, description: String, category: String) -> Unit,
    onDelete: ((id: Long) -> Unit)? = null
) {
    var description by remember(transaction) { mutableStateOf(transaction.description) }
    var selectedCategory by remember(transaction) { mutableStateOf(transaction.category) }

    val formattedDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    val isIncome = transaction.type == TransactionType.CREDIT
    val isInternalTransfer = transaction.category == ExpenseCategory.INTERNAL_TRANSFER
    val bankVisual = remember(transaction.bankName) {
        CategoryVisuals.getBankVisual(transaction.bankName)
    }

    // Bring the suggested / selected category to the front (index 0),
    // followed by Transportation, Food & Dining, and other common categories.
    val orderedCategories = remember(transaction.category) {
        val baseOrder = listOf(
            ExpenseCategory.TRANSPORTATION,
            ExpenseCategory.FOOD_DINING,
            ExpenseCategory.GROCERIES,
            ExpenseCategory.BILLS_UTILITIES,
            ExpenseCategory.SHOPPING,
            ExpenseCategory.TRANSFER,
            ExpenseCategory.INTERNAL_TRANSFER,
            ExpenseCategory.SALARY_INCOME,
            ExpenseCategory.HEALTHCARE,
            ExpenseCategory.ENTERTAINMENT,
            ExpenseCategory.OTHER,
            ExpenseCategory.UNCATEGORIZED
        )
        val initialCategory = transaction.category
        if (initialCategory.isNotBlank() && initialCategory != ExpenseCategory.UNCATEGORIZED) {
            listOf(initialCategory) + baseOrder.filter { it != initialCategory }
        } else {
            baseOrder
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Bank, Date, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(bankVisual.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = bankVisual.icon,
                            contentDescription = transaction.bankName,
                            tint = bankVisual.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bankVisual.shortName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = when {
                        isInternalTransfer -> "${FormatUtils.formatAmount(transaction.amount)} ${transaction.currency}"
                        isIncome -> "+${FormatUtils.formatAmount(transaction.amount)} ${transaction.currency}"
                        else -> "-${FormatUtils.formatAmount(transaction.amount)} ${transaction.currency}"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isInternalTransfer -> Color(0xFF38BDF8)
                        isIncome -> Color(0xFF34D399)
                        else -> Color(0xFFFB7185)
                    }
                )
            }

            // Description Input Field with Done action to save
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Add Description / Note", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. Taxi to office, lunch, groceries...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                    focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSave(transaction.id, description, selectedCategory)
                    }
                )
            )

            // Category Selector: Tap to save instantly
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap to save",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    orderedCategories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val style = CategoryVisuals.getStyle(cat)

                        Surface(
                            onClick = {
                                selectedCategory = cat
                                onSave(transaction.id, description, cat)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) style.color.copy(alpha = 0.15f) else androidx.compose.material3.MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) style.color else androidx.compose.material3.MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = cat,
                                    tint = if (isSelected) style.color else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = style.color,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Raw SMS Snippet Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Original Bank SMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.rawBody,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
