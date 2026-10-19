package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.IncomeCategory
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
    onUpdateCash: ((id: Long, amount: Double, type: TransactionType, category: String, description: String, timestamp: Long) -> Unit)? = null,
    onDelete: ((id: Long) -> Unit)? = null,
    expenseCategories: List<String> = ExpenseCategory.allExpenseCategories,
    incomeCategories: List<String> = IncomeCategory.allCategories,
    onCreateCategory: ((name: String, type: TransactionType, iconKey: String, colorHex: Long) -> Unit)? = null
) {
    val isCash = remember(transaction) {
        transaction.bankName.equals("Cash", ignoreCase = true) ||
                transaction.senderAddress.equals("Manual Entry", ignoreCase = true) ||
                transaction.referenceId?.startsWith("CASH_") == true
    }

    if (isCash) {
        // Full Cash Editing & Deletion Bottom Sheet
        CashTransactionEditSheet(
            transaction = transaction,
            onDismiss = onDismiss,
            onUpdateCash = onUpdateCash,
            onDelete = onDelete,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            onCreateCategory = onCreateCategory
        )
    } else {
        // Original Bank SMS Bottom Sheet (Restored exactly as before)
        BankSmsEditSheet(
            transaction = transaction,
            onDismiss = onDismiss,
            onSave = onSave,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            onCreateCategory = onCreateCategory
        )
    }
}

/**
 * Original Bank SMS Edit Sheet (preserves instant tap-to-save and clean layout)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankSmsEditSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (id: Long, description: String, category: String) -> Unit,
    expenseCategories: List<String> = ExpenseCategory.allExpenseCategories,
    incomeCategories: List<String> = IncomeCategory.allCategories,
    onCreateCategory: ((name: String, type: TransactionType, iconKey: String, colorHex: Long) -> Unit)? = null
) {
    var description by remember(transaction) { mutableStateOf(transaction.description) }
    var selectedCategory by remember(transaction) { mutableStateOf(transaction.category) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    val formattedDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    val isIncome = transaction.type == TransactionType.CREDIT
    val isInternalTransfer = transaction.category == ExpenseCategory.INTERNAL_TRANSFER
    val bankVisual = remember(transaction.bankName) {
        CategoryVisuals.getBankVisual(transaction.bankName)
    }

    val orderedCategories = remember(transaction.category, isIncome, expenseCategories, incomeCategories) {
        val baseOrder = if (isIncome) {
            incomeCategories
        } else {
            expenseCategories
        }
        val initialCategory = transaction.category
        if (initialCategory.isNotBlank() && initialCategory != ExpenseCategory.UNCATEGORIZED) {
            listOf(initialCategory) + baseOrder.filter { it != initialCategory }
        } else {
            baseOrder
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                label = { Text("Add Description / Note", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. Taxi to office, lunch, groceries...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    // + New Category Chip
                    Surface(
                        onClick = { showCreateCategoryDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Category",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    orderedCategories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val style = CategoryVisuals.getStyle(cat)

                        Surface(
                            onClick = {
                                selectedCategory = cat
                                onSave(transaction.id, description, cat)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) style.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) style.color else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = cat,
                                    tint = if (isSelected) style.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Original Bank SMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.rawBody,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            initialType = transaction.type,
            existingCategories = orderedCategories,
            onDismiss = { showCreateCategoryDialog = false },
            onCategoryCreated = { name, type, iconKey, colorHex ->
                onCreateCategory?.invoke(name, type, iconKey, colorHex)
                selectedCategory = name
                onSave(transaction.id, description, name)
                showCreateCategoryDialog = false
            }
        )
    }
}

/**
 * Full Cash Entry Edit and Delete Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashTransactionEditSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onUpdateCash: ((id: Long, amount: Double, type: TransactionType, category: String, description: String, timestamp: Long) -> Unit)?,
    onDelete: ((id: Long) -> Unit)?,
    expenseCategories: List<String> = ExpenseCategory.allExpenseCategories,
    incomeCategories: List<String> = IncomeCategory.allCategories,
    onCreateCategory: ((name: String, type: TransactionType, iconKey: String, colorHex: Long) -> Unit)? = null
) {
    var rawAmount by remember(transaction) {
        mutableStateOf(
            if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString()
            else transaction.amount.toString()
        )
    }
    var transactionType by remember(transaction) { mutableStateOf(transaction.type) }
    var selectedCategory by remember(transaction) { mutableStateOf(transaction.category) }
    var description by remember(transaction) { mutableStateOf(transaction.description) }
    var selectedTimestamp by remember(transaction) { mutableLongStateOf(transaction.timestamp) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    val isExpense = transactionType == TransactionType.DEBIT
    val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = parsedAmount > 0.0

    val dateLabel = remember(selectedTimestamp) {
        val nowCal = Calendar.getInstance()
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }

        val isToday = nowCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR)

        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = nowCal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR)

        when {
            isToday -> "Today (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedTimestamp))})"
            isYesterday -> "Yesterday (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedTimestamp))})"
            else -> SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedTimestamp))
        }
    }

    val isIncome = !isExpense

    val orderedCategories = remember(selectedCategory, isIncome, expenseCategories, incomeCategories) {
        val baseOrder = if (isIncome) {
            incomeCategories
        } else {
            expenseCategories
        }
        if (selectedCategory.isNotBlank() && selectedCategory != ExpenseCategory.UNCATEGORIZED) {
            listOf(selectedCategory) + baseOrder.filter { it != selectedCategory }
        } else {
            baseOrder
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Payments Icon, Title, and Close Button
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
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Cash Transaction",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Edit Cash Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Update amount, type, or notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Segmented Control: Expense vs Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expense Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isExpense) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { 
                            if (transactionType != TransactionType.DEBIT) {
                                transactionType = TransactionType.DEBIT
                                if (selectedCategory in IncomeCategory.allCategories) {
                                    selectedCategory = ExpenseCategory.FOOD_DINING
                                }
                            }
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        fontSize = 13.sp,
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = if (isExpense) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Income Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isExpense) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { 
                            if (transactionType != TransactionType.CREDIT) {
                                transactionType = TransactionType.CREDIT
                                if (selectedCategory in ExpenseCategory.allExpenseCategories) {
                                    selectedCategory = IncomeCategory.SALARY
                                }
                            }
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        fontSize = 13.sp,
                        fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isExpense) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount Input Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Amount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ETB",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = rawAmount,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                    rawAmount = input
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "0.00",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quick Increment Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 100, 200, 500).forEach { inc ->
                            Surface(
                                onClick = {
                                    val current = rawAmount.toDoubleOrNull() ?: 0.0
                                    rawAmount = String.format(Locale.US, "%.0f", current + inc)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "+$inc",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Date Selector Card
            Surface(
                onClick = { showDatePickerDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dateLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Change",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2AABEE)
                    )
                }
            }

            // Description / Note Input Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Note", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("e.g. Taxi, lunch, groceries...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (parsedAmount > 0.0) {
                            onUpdateCash?.invoke(
                                transaction.id,
                                parsedAmount,
                                transactionType,
                                selectedCategory,
                                description,
                                selectedTimestamp
                            )
                        }
                    }
                )
            )

            // Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // + New Category Chip
                    Surface(
                        onClick = { showCreateCategoryDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Category",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    orderedCategories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val style = CategoryVisuals.getStyle(cat)

                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) style.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) style.color else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = cat,
                                    tint = if (isSelected) style.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons: Update (Cyan-Blue Primary) & Delete (Red Outline)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Update Button in Cyan-Blue (0xFF2AABEE)
                Button(
                    onClick = {
                        if (parsedAmount > 0.0) {
                            onUpdateCash?.invoke(
                                transaction.id,
                                parsedAmount,
                                transactionType,
                                selectedCategory,
                                description,
                                selectedTimestamp
                            )
                        }
                    },
                    enabled = isSaveEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2AABEE),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Update Cash Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteConfirmationDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Transaction",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog for Cash entries
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444)
                )
            },
            title = {
                Text(
                    text = "Delete Transaction?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this cash entry of ${FormatUtils.formatAmount(transaction.amount)} ETB? This cannot be undone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        onDelete?.invoke(transaction.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmationDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    // DatePickerDialog for Cash entries
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedUtc ->
                            val calLocal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                            val hour = calLocal.get(Calendar.HOUR_OF_DAY)
                            val minute = calLocal.get(Calendar.MINUTE)

                            val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtc
                            }
                            val finalCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, calUtc.get(Calendar.YEAR))
                                set(Calendar.MONTH, calUtc.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, calUtc.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                            }
                            selectedTimestamp = finalCal.timeInMillis
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            initialType = transactionType,
            existingCategories = orderedCategories,
            onDismiss = { showCreateCategoryDialog = false },
            onCategoryCreated = { name, type, iconKey, colorHex ->
                onCreateCategory?.invoke(name, type, iconKey, colorHex)
                selectedCategory = name
                showCreateCategoryDialog = false
            }
        )
    }
}
