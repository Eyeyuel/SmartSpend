package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class TelebirrParser : BankParser {

    override val bankName: String = "Telebirr"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("telebirr") ||
                s == "127" ||
                b.contains("telebirr") ||
                b.contains("transaction number is") ||
                b.contains("transaction id:") && (b.contains("you have transferred") || b.contains("you have received") || b.contains("you have paid"))
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        // 1. Transaction Type
        val isDebit = lowerBody.contains("transferred") ||
                lowerBody.contains("paid") ||
                lowerBody.contains("recharged") ||
                lowerBody.contains("bought") ||
                lowerBody.contains("withdrawn")
        val isCredit = lowerBody.contains("received") ||
                lowerBody.contains("deposited") ||
                lowerBody.contains("cash-in")

        val type = when {
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> return null
        }

        // 2. Extract Amount
        val amountRegex = Regex(
            """(?:transferred|paid|received|deposited|bought|withdrawn|recharged)\s+(?:ETB|Birr)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1) ?: run {
            val fallbackRegex = Regex("""(?:ETB|Birr)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
            fallbackRegex.find(body)?.groupValues?.getOrNull(1)
        } ?: return null

        val amount = ParserUtils.parseAmount(rawAmount) ?: return null

        // 3. Extract Metadata
        val balance = ParserUtils.extractBalance(body)
        val referenceId = ParserUtils.extractReference(body)

        // 4. Extract Counterparty and Smart Category
        val (counterparty, suggestedCategory) = extractCounterpartyAndCategory(body)

        // 5. Parse explicit timestamp if present
        val parsedTime = extractExplicitDate(body) ?: timestamp

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = "Telebirr",
            senderAddress = sender,
            amount = amount,
            currency = "ETB",
            type = type,
            timestamp = parsedTime,
            balanceAfter = balance,
            accountNumber = extractAccount(body),
            rawBody = body,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory
        )
    }

    private fun extractCounterpartyAndCategory(body: String): Pair<String?, String> {
        // 1. Package purchase
        val pkgRegex = Regex("""for\s+package\s+(.*?)\s+(?:purchase\s+made|purchase|on\s+[0-9])""", RegexOption.IGNORE_CASE)
        val pkgMatch = pkgRegex.find(body)
        if (pkgMatch != null) {
            return Pair(pkgMatch.groupValues[1].trim(), ExpenseCategory.BILLS_UTILITIES)
        }

        // 2. Airtime recharge
        val airRegex = Regex("""recharged\s+ETB\s+[0-9.,]+\s+airtime\s+for\s+([0-9]+)""", RegexOption.IGNORE_CASE)
        val airMatch = airRegex.find(body)
        if (airMatch != null) {
            return Pair("Airtime (${airMatch.groupValues[1]})", ExpenseCategory.BILLS_UTILITIES)
        }

        // 3. Pay bill
        val billRegex = Regex("""to\s+pay\s+bill\s+for\s+([A-Za-z0-9\-_]+)""", RegexOption.IGNORE_CASE)
        val billMatch = billRegex.find(body)
        if (billMatch != null) {
            return Pair("Bill #${billMatch.groupValues[1]}", ExpenseCategory.BILLS_UTILITIES)
        }

        // 4. Merchant / Service fee
        val mchRegex = Regex("""(?:from\s+[0-9]{4,}\s*-\s*|for\s+Service\s+Fee\s+from\s+)([A-Za-z0-9\s&'.,]+?)(?:\s+on\s+[0-9])""", RegexOption.IGNORE_CASE)
        val mchMatch = mchRegex.find(body)
        if (mchMatch != null) {
            val name = mchMatch.groupValues[1].trim()
            val lower = name.lowercase()
            val cat = when {
                lower.contains("cafe") || lower.contains("restaurant") || lower.contains("burger") ||
                        lower.contains("pizza") || lower.contains("coffee") || lower.contains("kitchen") ||
                        lower.contains("bakery") || lower.contains("bar") -> ExpenseCategory.FOOD_DINING
                lower.contains("mart") || lower.contains("supermarket") || lower.contains("market") ||
                        lower.contains("grocery") -> ExpenseCategory.GROCERIES
                lower.contains("taxi") || lower.contains("ride") || lower.contains("transport") ||
                        lower.contains("fuel") || lower.contains("oil") || lower.contains("total") -> ExpenseCategory.TRANSPORTATION
                lower.contains("pharmacy") || lower.contains("hospital") || lower.contains("clinic") ||
                        lower.contains("med") -> ExpenseCategory.HEALTHCARE
                else -> ExpenseCategory.SHOPPING
            }
            return Pair(name, cat)
        }

        // 5. Transfer to person or bank
        val toPersonRegex = Regex("""transferred\s+ETB\s+[0-9.,]+\s+to\s+([A-Za-z0-9\s'.,]+?)(?:\s*\([0-9*+]+\)|\s+on\s+[0-9])""", RegexOption.IGNORE_CASE)
        val toMatch = toPersonRegex.find(body)
        if (toMatch != null) {
            val name = toMatch.groupValues[1].trim()
            val isBank = name.contains("Bank", ignoreCase = true)
            return Pair(name, if (isBank) ExpenseCategory.INTERNAL_TRANSFER else ExpenseCategory.TRANSFER)
        }

        // 6. Received from person or bank
        val fromRegex = Regex("""from\s+([A-Za-z0-9\s'.,]+?)(?:\s*\([0-9*+]+\)|\s+to\s+your|\s+on\s+[0-9])""", RegexOption.IGNORE_CASE)
        val fromMatch = fromRegex.find(body)
        if (fromMatch != null) {
            val name = fromMatch.groupValues[1].trim()
            val isBank = name.contains("Bank", ignoreCase = true)
            return Pair(name, if (isBank) ExpenseCategory.INTERNAL_TRANSFER else ExpenseCategory.TRANSFER)
        }

        return Pair(null, ExpenseCategory.UNCATEGORIZED)
    }

    private fun extractAccount(body: String): String? {
        val accRegex = Regex("""telebirr\s+Account\s+([0-9]{9,15})""", RegexOption.IGNORE_CASE)
        return accRegex.find(body)?.groupValues?.getOrNull(1)
    }

    private fun extractExplicitDate(body: String): Long? {
        val dateRegex = Regex("""on\s+([0-9]{4}-[0-9]{2}-[0-9]{2}\s+[0-9]{2}:[0-9]{2}:[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4}\s+[0-9]{2}:[0-9]{2}:[0-9]{2})""")
        val rawDate = dateRegex.find(body)?.groupValues?.getOrNull(1) ?: return null

        return try {
            if (rawDate.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(rawDate)?.time
            } else {
                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).parse(rawDate)?.time
            }
        } catch (_: Exception) {
            null
        }
    }
}
