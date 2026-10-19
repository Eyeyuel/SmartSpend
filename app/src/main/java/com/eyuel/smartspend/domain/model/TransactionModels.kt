package com.eyuel.smartspend.domain.model

enum class TransactionType {
    DEBIT,  // Expense / Outgoing money
    CREDIT  // Income / Incoming money
}

enum class BankSource(val displayName: String, val defaultSenderKeywords: List<String>) {
    CBE("Commercial Bank of Ethiopia", listOf("CBE", "127", "Commercial Bank")),
    TELEBIRR("Telebirr", listOf("telebirr", "127", "Ethio Telecom")),
    ABYSSINIA("Bank of Abyssinia", listOf("Abyssinia", "BOA")),
    AWASH("Awash Bank", listOf("Awash", "AWASHBANK")),
    DASHEN("Dashen Bank", listOf("Dashen", "DASHENBANK")),
    OTHER("Other Financial", emptyList());

    companion object {
        fun fromSender(sender: String): BankSource {
            val lower = sender.lowercase()
            return entries.firstOrNull { source ->
                source.defaultSenderKeywords.any { kw -> lower.contains(kw.lowercase()) }
            } ?: OTHER
        }
    }
}

object ExpenseCategory {
    const val UNCATEGORIZED = "Uncategorized"
    const val FOOD_DINING = "Food & Dining"
    const val GROCERIES = "Groceries"
    const val TRANSPORTATION = "Transportation"
    const val BILLS_UTILITIES = "Bills & Utilities"
    const val SHOPPING = "Shopping"
    const val SALARY_INCOME = "Salary & Income"
    const val TRANSFER = "Transfer"
    const val INTERNAL_TRANSFER = "Internal Transfer"
    const val HEALTHCARE = "Healthcare"
    const val ENTERTAINMENT = "Entertainment"
    const val OTHER = "Other"

    val allExpenseCategories = listOf(
        TRANSPORTATION,
        FOOD_DINING,
        GROCERIES,
        BILLS_UTILITIES,
        SHOPPING,
        TRANSFER,
        INTERNAL_TRANSFER,
        HEALTHCARE,
        ENTERTAINMENT,
        OTHER,
        UNCATEGORIZED
    )

    val allCategories = listOf(
        UNCATEGORIZED,
        FOOD_DINING,
        GROCERIES,
        TRANSPORTATION,
        BILLS_UTILITIES,
        SHOPPING,
        SALARY_INCOME,
        TRANSFER,
        INTERNAL_TRANSFER,
        HEALTHCARE,
        ENTERTAINMENT,
        OTHER
    )
}

object IncomeCategory {
    const val SALARY = "Salary & Wages"
    const val BUSINESS = "Business & Sales"
    const val FREELANCE = "Freelance & Consulting"
    const val TRANSFER = "Transfer / Received"
    const val GIFT = "Gift & Allowance"
    const val INVESTMENT = "Investment & Interest"
    const val RENTAL = "Rental Income"
    const val REFUND = "Refund / Cash Back"
    const val INTERNAL_TRANSFER = "Internal Transfer"
    const val OTHER_INCOME = "Other Income"

    val allCategories = listOf(
        SALARY,
        BUSINESS,
        FREELANCE,
        TRANSFER,
        GIFT,
        INVESTMENT,
        RENTAL,
        REFUND,
        INTERNAL_TRANSFER,
        OTHER_INCOME
    )
}

data class ParsedTransaction(
    val referenceId: String?,
    val bankName: String,
    val senderAddress: String,
    val amount: Double,
    val currency: String = "ETB",
    val type: TransactionType,
    val timestamp: Long,
    val balanceAfter: Double? = null,
    val accountNumber: String? = null,
    val rawBody: String,
    val counterparty: String? = null,
    val suggestedCategory: String? = null
)
