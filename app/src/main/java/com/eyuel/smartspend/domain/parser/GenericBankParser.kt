package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class GenericBankParser : BankParser {

    override val bankName: String = "Generic Financial"

    override fun canParse(sender: String, body: String): Boolean {
        val lower = (sender + " " + body).lowercase()
        val hasFinancialKeywords = lower.contains("debited") ||
                lower.contains("credited") ||
                lower.contains("transferred") ||
                lower.contains("withdrawal") ||
                lower.contains("withdrawn") ||
                lower.contains("deposited") ||
                lower.contains("payment to") ||
                lower.contains("payment of") ||
                lower.contains("paid") ||
                lower.contains("received")
        val hasCurrency = lower.contains("etb") ||
                lower.contains("birr") ||
                lower.contains("usd") ||
                lower.contains("$")

        return hasFinancialKeywords && hasCurrency
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        // 1. Transaction Type
        val isDebit = lowerBody.contains("debited") ||
                lowerBody.contains("transferred") ||
                lowerBody.contains("withdrawal") ||
                lowerBody.contains("withdrawn") ||
                lowerBody.contains("paid") ||
                lowerBody.contains("payment")
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

        // 2. Amount Extraction
        // Matches: "debited with ETB 500", "debited by ETB 600.00", "paid ETB 250.00", "credited with ETB 3,000.00", "ETB 500 debited"
        val amountRegex = Regex(
            """(?:debited\s*(?:with|by)?|credited\s*(?:with|by)?|transferred|deposited|withdrawn|paid|received|payment\s+of)\s*(?:ETB|Birr|USD|\$)?\s*(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: run {
                val reverseRegex = Regex(
                    """(?:ETB|Birr|USD|\$)\s*(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)\s*(?:debited|credited|deposited|transferred|withdrawn|paid)""",
                    RegexOption.IGNORE_CASE
                )
                reverseRegex.find(body)?.groupValues?.getOrNull(1)
            } ?: run {
                // Fallback: first currency amount
                val generalRegex = Regex("""(?:ETB|Birr|USD|\$)\s*(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                generalRegex.find(body)?.groupValues?.getOrNull(1)
            } ?: return null

        val sanitized = rawAmount.replace("-", "").trim()
        val amount = ParserUtils.parseAmount(sanitized) ?: return null

        // 3. Metadata & Bank Resolution
        val cleanBankName = detectBankName(sender, body)
        val balance = ParserUtils.extractBalance(body)
        val explicitRef = ParserUtils.extractReference(body)
        val referenceId = explicitRef ?: ParserUtils.generateDeterministicReference(cleanBankName, timestamp, amount, body)
        val accountNumber = ParserUtils.extractAccountNumber(body)

        // 4. Counterparty & Categorization
        val (counterparty, suggestedCategory) = extractCounterpartyAndCategory(body, cleanBankName, type)

        // 5. Explicit Date
        val parsedTime = extractExplicitDate(body) ?: timestamp

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = cleanBankName,
            senderAddress = sender,
            amount = amount,
            currency = if (lowerBody.contains("usd") || lowerBody.contains("$")) "USD" else "ETB",
            type = type,
            timestamp = parsedTime,
            balanceAfter = balance,
            accountNumber = accountNumber,
            rawBody = body,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory
        )
    }

    private fun detectBankName(sender: String, body: String): String {
        val lower = (sender + " " + body).lowercase()
        return when {
            lower.contains("dashen") || lower.contains("amole") -> "Dashen Bank"
            lower.contains("awash") -> "Awash Bank"
            lower.contains("hibret") || lower.contains("united bank") -> "Hibret Bank"
            lower.contains("cooperative bank") || lower.contains("coop bank") || lower.contains("coop") -> "Coop Bank"
            lower.contains("amhara") -> "Amhara Bank"
            lower.contains("siinqee") || lower.contains("sinqee") -> "Siinqee Bank"
            lower.contains("zemen") -> "Zemen Bank"
            lower.contains("oromia") -> "Oromia Bank"
            lower.contains("wegagen") -> "Wegagen Bank"
            lower.contains("berhan") -> "Berhan Bank"
            lower.contains("enat") -> "Enat Bank"
            lower.contains("global bank") -> "Global Bank"
            lower.contains("tsehay") -> "Tsehay Bank"
            lower.contains("gadaa") -> "Gadaa Bank"
            sender.isNotBlank() && !sender.all { it.isDigit() } -> sender.trim()
            else -> "Bank Alert"
        }
    }

    private fun extractCounterpartyAndCategory(body: String, bankName: String, type: TransactionType): Pair<String?, String> {
        val lower = body.lowercase()

        // 1. "to <Party>" or "from <Party>" or "by <Party>"
        val partyRegex = Regex("""(?:to|from|by)\s+([A-Za-z0-9\s&'-]+?)(?=\s+(?:on|using|with|via|Ref:|$)|[.,\n]|$)""", RegexOption.IGNORE_CASE)
        val match = partyRegex.find(body)
        if (match != null) {
            val candidate = match.groupValues[1].trim()
            val lowerCand = candidate.lowercase()
            if (candidate.isNotBlank() &&
                !lowerCand.contains("account") &&
                !lowerCand.contains("your account") &&
                !lowerCand.contains(bankName.lowercase())
            ) {
                val cat = when {
                    lowerCand.contains("telebirr") -> ExpenseCategory.INTERNAL_TRANSFER
                    lowerCand.contains("cafe") || lowerCand.contains("restaurant") || lowerCand.contains("burger") ||
                            lowerCand.contains("pizza") || lowerCand.contains("coffee") || lowerCand.contains("kitchen") -> ExpenseCategory.FOOD_DINING
                    lowerCand.contains("mart") || lowerCand.contains("supermarket") || lowerCand.contains("market") -> ExpenseCategory.GROCERIES
                    lowerCand.contains("taxi") || lowerCand.contains("ride") || lowerCand.contains("fuel") -> ExpenseCategory.TRANSPORTATION
                    lowerCand.contains("package") || lowerCand.contains("airtime") || lowerCand.contains("bill") -> ExpenseCategory.BILLS_UTILITIES
                    else -> if (type == TransactionType.CREDIT || lower.contains("transfer")) ExpenseCategory.TRANSFER else ExpenseCategory.OTHER
                }
                return Pair(candidate, cat)
            }
        }

        return Pair(
            "$bankName ${if (type == TransactionType.CREDIT) "Deposit" else "Debit"}",
            if (type == TransactionType.CREDIT) ExpenseCategory.TRANSFER else ExpenseCategory.OTHER
        )
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
