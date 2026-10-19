package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eyuel.smartspend.data.local.UserCategoryEntity
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.IncomeCategory
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.CategoryVisuals
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesBottomSheet(
    categories: List<UserCategoryEntity>,
    onDismiss: () -> Unit,
    onCreateCategoryClick: () -> Unit,
    onDeleteCategory: (name: String, reassignTo: String) -> Unit,
    getTransactionCount: suspend (String) -> Int
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Expenses, 1 = Income
    var categoryToDelete by remember { mutableStateOf<UserCategoryEntity?>(null) }
    var deleteUsageCount by remember { mutableIntStateOf(0) }
    var isLoadingUsage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentType = if (selectedTab == 0) TransactionType.DEBIT else TransactionType.CREDIT
    val filteredCategories = remember(categories, currentType) {
        categories.filter { it.type == currentType }
    }

    val expenseCount = remember(categories) { categories.count { it.type == TransactionType.DEBIT } }
    val incomeCount = remember(categories) { categories.count { it.type == TransactionType.CREDIT } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131B2A), // DarkSurface
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF64748B)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & "+ Add" Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Categories",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Customize and manage transaction tags",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    onClick = onCreateCategoryClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Add New",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // Segmented Tabs: Expenses vs Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B0F17))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Expense Tab
                val isExpenseSelected = selectedTab == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isExpenseSelected) Color(0xFF1E2A3F) else Color.Transparent
                        )
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expenses ($expenseCount)",
                        fontSize = 13.sp,
                        fontWeight = if (isExpenseSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isExpenseSelected) Color.White else Color(0xFF94A3B8)
                    )
                }

                // Income Tab
                val isIncomeSelected = selectedTab == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isIncomeSelected) Color(0xFF1E2A3F) else Color.Transparent
                        )
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income ($incomeCount)",
                        fontSize = 13.sp,
                        fontWeight = if (isIncomeSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isIncomeSelected) Color.White else Color(0xFF94A3B8)
                    )
                }
            }

            // Categories List
            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No categories found",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCategories, key = { it.name }) { category ->
                        val catColor = Color(category.colorHex)
                        val catIcon = CategoryVisuals.getIconByKey(category.iconKey)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0B0F17),
                            border = BorderStroke(1.dp, Color(0xFF1E2A3F))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.2f))
                                            .border(BorderStroke(1.dp, catColor.copy(alpha = 0.5f)), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = category.name,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = category.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (category.isDefault) "Default System Tag" else "Custom Category",
                                            fontSize = 11.sp,
                                            color = if (category.isDefault) Color(0xFF64748B) else Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                if (category.isDefault) {
                                    // Default lock pill
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E2A3F).copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Default",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Default",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    // Delete action button
                                    IconButton(
                                        onClick = {
                                            categoryToDelete = category
                                            isLoadingUsage = true
                                            coroutineScope.launch {
                                                deleteUsageCount = getTransactionCount(category.name)
                                                isLoadingUsage = false
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete ${category.name}",
                                            tint = Color(0xFFF43F5E),
                                            modifier = Modifier.size(20.dp)
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

    // Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        Dialog(onDismissRequest = { categoryToDelete = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFF131B2A),
                border = BorderStroke(1.dp, Color(0xFF1E2A3F))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Delete Category?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val reassignName = if (cat.type == TransactionType.DEBIT) ExpenseCategory.OTHER else IncomeCategory.OTHER_INCOME
                    val usageText = if (isLoadingUsage) {
                        "Checking transaction usage..."
                    } else if (deleteUsageCount > 0) {
                        "This category is currently used in $deleteUsageCount transaction(s). If deleted, those transactions will be safely reassigned to '$reassignName'."
                    } else {
                        "Are you sure you want to delete '${cat.name}'? This action cannot be undone."
                    }

                    Text(
                        text = usageText,
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { categoryToDelete = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E2A3F)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onDeleteCategory(cat.name, reassignName)
                                categoryToDelete = null
                            },
                            enabled = !isLoadingUsage,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF43F5E),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
