package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType

class GenericBankParser : BankParser {

    override val bankName: String = "Generic Financial"

    override fun canParse(sender: String, body: String): Boolean {
        val lower = body.lowercase()
        val hasFinancialKeywords = lower.contains("debited") ||
                lower.contains("credited") ||
                lower.contains("transferred") ||
                lower.contains("withdrawal") ||
                lower.contains("withdrawn") ||
                lower.contains("deposited") ||
                lower.contains("payment to") ||
                lower.contains("payment of")
        val hasCurrency = lower.contains("etb") ||
                lower.contains("birr") ||
                lower.contains("usd") ||
                lower.contains("$")

        return hasFinancialKeywords && hasCurrency
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lower = body.lowercase()

        val isDebit = lower.contains("debited") ||
                lower.contains("transferred") ||
                lower.contains("withdrawal") ||
                lower.contains("withdrawn") ||
                lower.contains("paid") ||
                lower.contains("payment")
        val isCredit = lower.contains("credited") ||
                lower.contains("deposited") ||
                lower.contains("received")

        val type = when {
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> return null
        }

        // Amount regex
        val amountRegex = Regex(
            """(?:ETB|Birr|USD|\$)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:debited|credited|deposited|transferred|withdrawn|paid)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1) ?: run {
            // Alternative: keyword followed by amount
            val altRegex = Regex(
                """(?:debited|credited|deposited|transferred|withdrawn|paid|amount(?:\s+of)?)\s*(?:with|by|to|for|is)?\s*(?:ETB|Birr|USD|\$)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""",
                RegexOption.IGNORE_CASE
            )
            altRegex.find(body)?.groupValues?.getOrNull(1)
        } ?: run {
            // General fallback: first currency amount
            val generalRegex = Regex("""(?:ETB|Birr|USD|\$)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
            generalRegex.find(body)?.groupValues?.getOrNull(1)
        } ?: return null

        val amount = ParserUtils.parseAmount(rawAmount) ?: return null

        val balance = ParserUtils.extractBalance(body)
        val referenceId = ParserUtils.extractReference(body)
        val accountNumber = ParserUtils.extractAccountNumber(body)

        // Try to derive a friendly bank name from sender or body
        val cleanBankName = when {
            sender.isNotBlank() && !sender.all { it.isDigit() } -> sender.trim()
            lower.contains("awash") -> "Awash Bank"
            lower.contains("dashen") -> "Dashen Bank"
            lower.contains("abyssinia") -> "Bank of Abyssinia"
            lower.contains("hibret") -> "Hibret Bank"
            lower.contains("cooperative bank") || lower.contains("coop") -> "Coop Bank"
            lower.contains("amhara") -> "Amhara Bank"
            lower.contains("siinqee") -> "Siinqee Bank"
            lower.contains("zemen") -> "Zemen Bank"
            lower.contains("oromia") -> "Oromia Bank"
            lower.contains("wegagen") -> "Wegagen Bank"
            lower.contains("berhan") -> "Berhan Bank"
            else -> "Bank Alert"
        }

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = cleanBankName,
            senderAddress = sender,
            amount = amount,
            currency = if (lower.contains("usd") || lower.contains("$")) "USD" else "ETB",
            type = type,
            timestamp = timestamp,
            balanceAfter = balance,
            accountNumber = accountNumber,
            rawBody = body
        )
    }
}
