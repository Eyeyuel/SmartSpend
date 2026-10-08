package com.eyuel.smartspend.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.eyuel.smartspend.domain.model.ExpenseCategory

data class CategoryStyle(
    val icon: ImageVector,
    val color: Color,
    val emoji: String
)

data class BankVisual(
    val icon: ImageVector,
    val accentColor: Color,
    val shortName: String
)

object CategoryVisuals {
    fun getStyle(category: String): CategoryStyle {
        return when (category) {
            ExpenseCategory.FOOD_DINING -> CategoryStyle(
                icon = Icons.Default.Restaurant,
                color = Color(0xFFFB923C), // Orange 400
                emoji = "🍔"
            )
            ExpenseCategory.GROCERIES -> CategoryStyle(
                icon = Icons.Default.LocalGroceryStore,
                color = Color(0xFF4ADE80), // Green 400
                emoji = "🛒"
            )
            ExpenseCategory.TRANSPORTATION -> CategoryStyle(
                icon = Icons.Default.DirectionsCar,
                color = Color(0xFF38BDF8), // Sky 400
                emoji = "🚕"
            )
            ExpenseCategory.BILLS_UTILITIES -> CategoryStyle(
                icon = Icons.Default.Bolt,
                color = Color(0xFFFACC15), // Yellow 400
                emoji = "⚡"
            )
            ExpenseCategory.SHOPPING -> CategoryStyle(
                icon = Icons.Default.ShoppingBag,
                color = Color(0xFFC084FC), // Purple 400
                emoji = "🛍️"
            )
            ExpenseCategory.SALARY_INCOME -> CategoryStyle(
                icon = Icons.Default.Payments,
                color = Color(0xFF34D399), // Emerald 400
                emoji = "💰"
            )
            ExpenseCategory.TRANSFER -> CategoryStyle(
                icon = Icons.Default.SwapHoriz,
                color = Color(0xFF818CF8), // Indigo 400
                emoji = "🔄"
            )
            ExpenseCategory.INTERNAL_TRANSFER -> CategoryStyle(
                icon = Icons.Default.SyncAlt,
                color = Color(0xFF38BDF8), // Sky 400
                emoji = "🔁"
            )
            ExpenseCategory.HEALTHCARE -> CategoryStyle(
                icon = Icons.Default.LocalHospital,
                color = Color(0xFFF43F5E), // Rose 500
                emoji = "💊"
            )
            ExpenseCategory.ENTERTAINMENT -> CategoryStyle(
                icon = Icons.Default.Theaters,
                color = Color(0xFFF472B6), // Pink 400
                emoji = "🎬"
            )
            ExpenseCategory.UNCATEGORIZED -> CategoryStyle(
                icon = Icons.Default.HelpOutline,
                color = Color(0xFF94A3B8), // Slate 400
                emoji = "🏷️"
            )
            else -> CategoryStyle(
                icon = Icons.Default.Category,
                color = Color(0xFF94A3B8),
                emoji = "📦"
            )
        }
    }

    fun getBankVisual(bankName: String): BankVisual {
        return when {
            bankName.contains("CBE", ignoreCase = true) || bankName.contains("Commercial Bank", ignoreCase = true) -> BankVisual(
                icon = Icons.Default.AccountBalance,
                accentColor = Color(0xFF0EA5E9), // Ocean Blue
                shortName = "CBE"
            )
            bankName.contains("Telebirr", ignoreCase = true) -> BankVisual(
                icon = Icons.Default.PhoneAndroid,
                accentColor = Color(0xFFA855F7), // Royal Purple
                shortName = "Telebirr"
            )
            bankName.contains("Dashen", ignoreCase = true) -> BankVisual(
                icon = Icons.Default.AccountBalance,
                accentColor = Color(0xFFF59E0B), // Amber Gold
                shortName = "Dashen"
            )
            bankName.contains("Awash", ignoreCase = true) -> BankVisual(
                icon = Icons.Default.AccountBalance,
                accentColor = Color(0xFF3B82F6), // Blue
                shortName = "Awash"
            )
            bankName.contains("Abyssinia", ignoreCase = true) -> BankVisual(
                icon = Icons.Default.AccountBalance,
                accentColor = Color(0xFF1D4ED8), // Deep Blue
                shortName = "Abyssinia"
            )
            else -> BankVisual(
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = Color(0xFF64748B),
                shortName = "Bank"
            )
        }
    }
}
