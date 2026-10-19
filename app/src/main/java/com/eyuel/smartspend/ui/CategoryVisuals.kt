package com.eyuel.smartspend.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.IncomeCategory

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

data class AvailableIcon(
    val key: String,
    val icon: ImageVector,
    val label: String
)

data class AvailableColor(
    val hex: Long,
    val label: String
)

object CategoryVisuals {

    val selectableColors = listOf(
        AvailableColor(0xFF38BDF8, "Sky Blue"),
        AvailableColor(0xFF34D399, "Emerald"),
        AvailableColor(0xFFFB923C, "Orange"),
        AvailableColor(0xFFFACC15, "Amber"),
        AvailableColor(0xFFC084FC, "Purple"),
        AvailableColor(0xFFF472B6, "Pink"),
        AvailableColor(0xFFF43F5E, "Rose"),
        AvailableColor(0xFF2DD4BF, "Teal"),
        AvailableColor(0xFF818CF8, "Indigo"),
        AvailableColor(0xFF60A5FA, "Blue"),
        AvailableColor(0xFFA78BFA, "Violet"),
        AvailableColor(0xFF94A3B8, "Slate")
    )

    val selectableIcons = listOf(
        AvailableIcon("Restaurant", Icons.Default.Restaurant, "Food"),
        AvailableIcon("LocalCafe", Icons.Default.LocalCafe, "Coffee"),
        AvailableIcon("LocalGroceryStore", Icons.Default.LocalGroceryStore, "Groceries"),
        AvailableIcon("ShoppingBag", Icons.Default.ShoppingBag, "Shopping"),
        AvailableIcon("DirectionsCar", Icons.Default.DirectionsCar, "Car"),
        AvailableIcon("LocalGasStation", Icons.Default.LocalGasStation, "Fuel"),
        AvailableIcon("Bolt", Icons.Default.Bolt, "Utilities"),
        AvailableIcon("Home", Icons.Default.Home, "Housing"),
        AvailableIcon("FitnessCenter", Icons.Default.FitnessCenter, "Gym"),
        AvailableIcon("SportsEsports", Icons.Default.SportsEsports, "Gaming"),
        AvailableIcon("Theaters", Icons.Default.Theaters, "Movies"),
        AvailableIcon("School", Icons.Default.School, "Education"),
        AvailableIcon("Book", Icons.Default.Book, "Books"),
        AvailableIcon("Pets", Icons.Default.Pets, "Pets"),
        AvailableIcon("Flight", Icons.Default.Flight, "Travel"),
        AvailableIcon("LocalHospital", Icons.Default.LocalHospital, "Health"),
        AvailableIcon("Medication", Icons.Default.Medication, "Pharmacy"),
        AvailableIcon("Spa", Icons.Default.Spa, "Beauty"),
        AvailableIcon("Work", Icons.Default.Work, "Work"),
        AvailableIcon("Payments", Icons.Default.Payments, "Salary"),
        AvailableIcon("Storefront", Icons.Default.Storefront, "Business"),
        AvailableIcon("Savings", Icons.Default.Savings, "Savings"),
        AvailableIcon("TrendingUp", Icons.Default.TrendingUp, "Invest"),
        AvailableIcon("CardGiftcard", Icons.Default.CardGiftcard, "Gift"),
        AvailableIcon("SwapHoriz", Icons.Default.SwapHoriz, "Transfer"),
        AvailableIcon("Category", Icons.Default.Category, "General")
    )

    private val customStyles = java.util.concurrent.ConcurrentHashMap<String, CategoryStyle>()

    fun getIconByKey(key: String): ImageVector {
        return selectableIcons.firstOrNull { it.key.equals(key, ignoreCase = true) }?.icon
            ?: Icons.Default.Category
    }

    fun registerCustomCategory(name: String, iconKey: String, colorHex: Long) {
        customStyles[name.trim().lowercase()] = CategoryStyle(
            icon = getIconByKey(iconKey),
            color = Color(colorHex),
            emoji = "🏷️"
        )
    }

    fun unregisterCustomCategory(name: String) {
        customStyles.remove(name.trim().lowercase())
    }

    fun getStyle(category: String): CategoryStyle {
        val trimmed = category.trim()
        val custom = customStyles[trimmed.lowercase()]
        if (custom != null) {
            return custom
        }

        return when (trimmed) {
            // Expense Categories
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
            ExpenseCategory.SALARY_INCOME, IncomeCategory.SALARY -> CategoryStyle(
                icon = Icons.Default.Payments,
                color = Color(0xFF34D399), // Emerald 400
                emoji = "💰"
            )
            ExpenseCategory.TRANSFER -> CategoryStyle(
                icon = Icons.Default.SwapHoriz,
                color = Color(0xFF818CF8), // Indigo 400
                emoji = "🔄"
            )
            ExpenseCategory.INTERNAL_TRANSFER, IncomeCategory.INTERNAL_TRANSFER -> CategoryStyle(
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

            // Dedicated Income Categories
            IncomeCategory.BUSINESS -> CategoryStyle(
                icon = Icons.Default.Storefront,
                color = Color(0xFF2DD4BF), // Teal 400
                emoji = "🏬"
            )
            IncomeCategory.FREELANCE -> CategoryStyle(
                icon = Icons.Default.Work,
                color = Color(0xFF38BDF8), // Sky 400
                emoji = "💼"
            )
            IncomeCategory.TRANSFER -> CategoryStyle(
                icon = Icons.Default.CallReceived,
                color = Color(0xFF60A5FA), // Blue 400
                emoji = "📥"
            )
            IncomeCategory.GIFT -> CategoryStyle(
                icon = Icons.Default.CardGiftcard,
                color = Color(0xFFF472B6), // Pink 400
                emoji = "🎁"
            )
            IncomeCategory.INVESTMENT -> CategoryStyle(
                icon = Icons.Default.TrendingUp,
                color = Color(0xFFFACC15), // Yellow 400
                emoji = "📈"
            )
            IncomeCategory.RENTAL -> CategoryStyle(
                icon = Icons.Default.Home,
                color = Color(0xFFA78BFA), // Purple 400
                emoji = "🏠"
            )
            IncomeCategory.REFUND -> CategoryStyle(
                icon = Icons.Default.Replay,
                color = Color(0xFF4ADE80), // Green 400
                emoji = "↩️"
            )
            IncomeCategory.OTHER_INCOME -> CategoryStyle(
                icon = Icons.Default.Savings,
                color = Color(0xFF10B981), // Emerald 500
                emoji = "💵"
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
            bankName.contains("Cash", ignoreCase = true) -> BankVisual(
                icon = Icons.Default.Payments,
                accentColor = Color(0xFF10B981), // Emerald 500
                shortName = "Cash"
            )
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
