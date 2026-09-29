package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class CbeParser : BankParser {

    override val bankName: String = "CBE"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("cbe") ||
                s.contains("commercial bank") ||
                b.contains("banking with cbe") ||
                b.contains("mbreciept.cbe.com.et") ||
                b.contains("apps.cbe.com.et") ||
                (b.contains("cbe") && (b.contains("debited") || b.contains("credited") || b.contains("transferred") || b.contains("debit transaction") || b.contains("deposited")))
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        // 1. Determine Transaction Type
        val isDebit = lowerBody.contains("debited") ||
                lowerBody.contains("transferred") ||
                lowerBody.contains("debit transaction") ||
                lowerBody.contains("withdrawal") ||
                lowerBody.contains("paid")
        val isCredit = lowerBody.contains("credited") ||
                lowerBody.contains("deposited") ||
                lowerBody.contains("received")

        val type = when {
            isDebit && !isCredit -> TransactionType.DEBIT
            isCredit && !isDebit -> TransactionType.CREDIT
            lowerBody.contains("transferred") -> TransactionType.DEBIT
            lowerBody.contains("received") -> TransactionType.CREDIT
            else -> return null
        }

        // 2. Extract Principal Amount
        // Matches: "debited with ETB 4023", "successfully transferred ETB1150.00", "received ETB 300.00", "credited with ETB 500.00", "debit transaction of ETB 20.0."
        val amountRegex = Regex(
            """(?:debited\s+with|credited\s+with|transferred|received|deposited|debit\s+transaction\s+of)\s*(?:ETB|Birr)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: run {
                val fallbackRegex = Regex("""(?:ETB|Birr)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                fallbackRegex.find(body)?.groupValues?.getOrNull(1)
            } ?: return null

        val amount = ParserUtils.parseAmount(rawAmount) ?: return null

        // 3. Extract Metadata
        val balance = ParserUtils.extractBalance(body)
        val explicitRef = ParserUtils.extractReference(body)
        val referenceId = explicitRef ?: ParserUtils.generateDeterministicReference("CBE", timestamp, amount, body)
        val accountNumber = extractUserAccount(body)

        // 4. Extract Counterparty and Smart Category
        val (counterparty, suggestedCategory) = extractCounterpartyAndCategory(body, type)

        // 5. Parse explicit timestamp if present
        val parsedTime = extractExplicitDate(body) ?: timestamp

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = "CBE",
            senderAddress = sender,
            amount = amount,
            currency = "ETB",
            type = type,
            timestamp = parsedTime,
            balanceAfter = balance,
            accountNumber = accountNumber,
            rawBody = body,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory
        )
    }

    private fun extractCounterpartyAndCategory(body: String, type: TransactionType): Pair<String?, String> {
        val lower = body.lowercase()

        // 0. Internal Wallet Transfers (Telebirr, CBE Birr)
        if (lower.contains("telebirr")) {
            return Pair("Telebirr Transfer", ExpenseCategory.INTERNAL_TRANSFER)
        }
        if (lower.contains("cbe birr")) {
            return Pair("CBE Birr Transfer", ExpenseCategory.INTERNAL_TRANSFER)
        }

        // 1. P2P transfers: "to account 1**9667 (Alemtsehay Birhane Kahsay)" or "from account 1**7426 (Minilik Belachew Balkideru)"
        val partyRegex = Regex("""(?:to|from)\s+account\s+[0-9*xX]+\s*\(([^)]+)\)""", RegexOption.IGNORE_CASE)
        val partyMatch = partyRegex.find(body)
        if (partyMatch != null) {
            val name = partyMatch.groupValues[1].trim()
            return Pair(name, ExpenseCategory.TRANSFER)
        }

        // 2. Merchant / Payment recipient
        val merchantRegex = Regex("""paid\s+(?:ETB|Birr)?\s*[0-9.,]+\s+(?:to\s+|for\s+)([A-Za-z0-9\s&'.,]+?)(?:\s+on|\s*\(|\.|$)""", RegexOption.IGNORE_CASE)
        val merchantMatch = merchantRegex.find(body)
        if (merchantMatch != null) {
            val name = merchantMatch.groupValues[1].trim()
            val mLower = name.lowercase()
            val cat = when {
                mLower.contains("cafe") || mLower.contains("restaurant") || mLower.contains("burger") ||
                        mLower.contains("pizza") || mLower.contains("coffee") || mLower.contains("kitchen") -> ExpenseCategory.FOOD_DINING
                mLower.contains("mart") || mLower.contains("supermarket") || mLower.contains("market") -> ExpenseCategory.GROCERIES
                mLower.contains("taxi") || mLower.contains("ride") || mLower.contains("fuel") -> ExpenseCategory.TRANSPORTATION
                mLower.contains("bill") || mLower.contains("telecom") || mLower.contains("water") || mLower.contains("electric") -> ExpenseCategory.BILLS_UTILITIES
                else -> ExpenseCategory.SHOPPING
            }
            return Pair(name, cat)
        }

        // 3. Branch Deposit / Receipt
        if (lower.contains("branchreceipt")) {
            return Pair("Branch Deposit", ExpenseCategory.TRANSFER)
        }

        // 4. Debit Transaction without person (e.g. ATM or general debit)
        if (lower.contains("debit transaction of")) {
            return Pair("CBE Debit Transaction", ExpenseCategory.OTHER)
        }

        // 5. Account debited alert
        if (lower.contains("has been debited")) {
            return Pair("CBE Account Debit", ExpenseCategory.OTHER)
        }

        // 6. Account credited alert
        if (lower.contains("has been credited")) {
            return Pair("CBE Account Deposit", ExpenseCategory.TRANSFER)
        }

        return Pair("CBE Transaction", if (type == TransactionType.CREDIT) ExpenseCategory.TRANSFER else ExpenseCategory.UNCATEGORIZED)
    }

    private fun extractUserAccount(body: String): String? {
        // Priority 1: "to your account 1**7868"
        val toYourRegex = Regex("""to\s+your\s+account\s+([0-9*xX]{4,})""", RegexOption.IGNORE_CASE)
        toYourRegex.find(body)?.groupValues?.getOrNull(1)?.let { return it }

        // Priority 2: "from account 1**7868 to account"
        val fromRegex = Regex("""from\s+account\s+([0-9*xX]{4,})\s+to\s+account""", RegexOption.IGNORE_CASE)
        fromRegex.find(body)?.groupValues?.getOrNull(1)?.let { return it }

        // Priority 3: "your account 1****7868" or "on your account 1****7868"
        val yourAccRegex = Regex("""(?:on\s+)?your\s+account\s+([0-9*xX]{4,})""", RegexOption.IGNORE_CASE)
        yourAccRegex.find(body)?.groupValues?.getOrNull(1)?.let { return it }

        return ParserUtils.extractAccountNumber(body)
    }

    private fun extractExplicitDate(body: String): Long? {
        val dateRegex = Regex("""on\s+([0-9]{2}/[0-9]{2}/[0-9]{4}|[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}-[A-Za-z]{3}-[0-9]{4})""", RegexOption.IGNORE_CASE)
        val rawDate = dateRegex.find(body)?.groupValues?.getOrNull(1) ?: return null

        return try {
            when {
                rawDate.contains("/") -> SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(rawDate)?.time
                rawDate.contains("-") && rawDate.length == 10 -> SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDate)?.time
                rawDate.contains("-") -> SimpleDateFormat("dd-MMM-yyyy", Locale.US).parse(rawDate)?.time
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
