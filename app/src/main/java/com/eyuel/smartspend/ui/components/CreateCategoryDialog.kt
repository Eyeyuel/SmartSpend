package com.eyuel.smartspend.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.CategoryVisuals

@Composable
fun CreateCategoryDialog(
    initialType: TransactionType = TransactionType.DEBIT,
    existingCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onCategoryCreated: (name: String, type: TransactionType, iconKey: String, colorHex: Long) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedColorHex by remember { mutableLongStateOf(CategoryVisuals.selectableColors[0].hex) }
    var selectedIconKey by remember {
        mutableStateOf(
            if (initialType == TransactionType.CREDIT) "Payments" else "Restaurant"
        )
    }

    val focusRequester = remember { FocusRequester() }
    val trimmedName = categoryName.trim()
    val isDuplicate = remember(trimmedName, existingCategories) {
        existingCategories.any { it.equals(trimmedName, ignoreCase = true) }
    }
    val isValid = trimmedName.isNotBlank() && !isDuplicate

    val currentColor = Color(selectedColorHex)
    val currentIcon = CategoryVisuals.getIconByKey(selectedIconKey)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF131B2A), // DarkSurface
            border = BorderStroke(1.dp, Color(0xFF1E2A3F)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Category",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Live Preview Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B0F17))
                        .border(BorderStroke(1.dp, Color(0xFF1E2A3F)), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(currentColor.copy(alpha = 0.2f))
                                .border(BorderStroke(1.5.dp, currentColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentIcon,
                                contentDescription = null,
                                tint = currentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (categoryName.isBlank()) "Category Name" else categoryName.trim(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (categoryName.isBlank()) Color(0xFF64748B) else Color.White
                            )
                            Text(
                                text = if (selectedType == TransactionType.DEBIT) "Expense Category" else "Income Category",
                                fontSize = 12.sp,
                                color = if (selectedType == TransactionType.DEBIT) Color(0xFFF43F5E) else Color(0xFF10B981),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Type Toggle (Expense vs Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B0F17))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Expense Option
                    val isExpense = selectedType == TransactionType.DEBIT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isExpense) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color.Transparent
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isExpense) Color(0xFFF43F5E) else Color.Transparent
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedType = TransactionType.DEBIT
                                if (selectedIconKey == "Payments") selectedIconKey = "Restaurant"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontSize = 13.sp,
                            fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                            color = if (isExpense) Color(0xFFF43F5E) else Color(0xFF94A3B8)
                        )
                    }

                    // Income Option
                    val isIncome = selectedType == TransactionType.CREDIT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isIncome) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isIncome) Color(0xFF10B981) else Color.Transparent
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedType = TransactionType.CREDIT
                                if (selectedIconKey == "Restaurant") selectedIconKey = "Payments"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            fontSize = 13.sp,
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIncome) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { if (it.length <= 25) categoryName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("Category Name", color = if (isDuplicate) Color(0xFFF43F5E) else Color(0xFF94A3B8)) },
                    placeholder = { Text("e.g. Gym, Pet Care, Freelance", color = Color(0xFF64748B), fontSize = 14.sp) },
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text("A category with this name already exists", color = Color(0xFFF43F5E), fontSize = 11.sp) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0B0F17),
                        unfocusedContainerColor = Color(0xFF0B0F17),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF1E2A3F),
                        errorBorderColor = Color(0xFFF43F5E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) {
                                onCategoryCreated(
                                    categoryName.trim(),
                                    selectedType,
                                    selectedIconKey,
                                    selectedColorHex
                                )
                            }
                        }
                    )
                )

                // Color Swatches Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Accent Color",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryVisuals.selectableColors.forEach { colorItem ->
                            val isSelected = selectedColorHex == colorItem.hex
                            val itemColor = Color(colorItem.hex)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(itemColor)
                                    .clickable { selectedColorHex = colorItem.hex }
                                    .border(
                                        BorderStroke(
                                            if (isSelected) 2.5.dp else 0.dp,
                                            if (isSelected) Color.White else Color.Transparent
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = colorItem.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Icon Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose Icon",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryVisuals.selectableIcons.forEach { iconItem ->
                            val isSelected = selectedIconKey == iconItem.key
                            val animatedBg by animateColorAsState(
                                if (isSelected) currentColor.copy(alpha = 0.2f) else Color(0xFF0B0F17),
                                label = "iconBg"
                            )
                            val animatedBorder by animateColorAsState(
                                if (isSelected) currentColor else Color(0xFF1E2A3F),
                                label = "iconBorder"
                            )

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(animatedBg)
                                    .border(BorderStroke(1.5.dp, animatedBorder), RoundedCornerShape(12.dp))
                                    .clickable { selectedIconKey = iconItem.key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconItem.icon,
                                    contentDescription = iconItem.label,
                                    tint = if (isSelected) currentColor else Color(0xFF94A3B8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Actions: Cancel & Create
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2A3F)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF94A3B8)
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            if (isValid) {
                                onCategoryCreated(
                                    categoryName.trim(),
                                    selectedType,
                                    selectedIconKey,
                                    selectedColorHex
                                )
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF0B0F17),
                            disabledContainerColor = Color(0xFF1E2A3F),
                            disabledContentColor = Color(0xFF64748B)
                        )
                    ) {
                        Text("Create Category", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
