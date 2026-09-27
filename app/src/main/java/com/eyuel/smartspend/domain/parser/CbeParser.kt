package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType

class CbeParser : BankParser {

    override val bankName: String = "CBE"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("cbe") ||
                s.contains("commercial bank") ||
                (b.contains("cbe") && (b.contains("debited") || b.contains("credited") || b.contains("transferred") || b.contains("deposited")))
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        // 1. Determine Transaction Type
        val isDebit = lowerBody.contains("debited") ||
                lowerBody.contains("transferred") ||
                lowerBody.contains("withdrawal") ||
                lowerBody.contains("paid")
        val isCredit = lowerBody.contains("credited") ||
                lowerBody.contains("deposited") ||
                lowerBody.contains("received")

        val type = when {
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> return null
        }

        // 2. Extract Amount
        // Regex matches: "debited with ETB 1,500.00", "credited with ETB 500", "ETB 350.00 debited", etc.
        val amountRegex = Regex(
            """(?:(?:debited\s+with|credited\s+with|transferred|deposited)\s+(?:ETB|Birr)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)|(?:ETB|Birr)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:debited|credited|deposited))""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: match?.groupValues?.getOrNull(2)
            ?: run {
                // Fallback: look for the first ETB amount that isn't the balance
                val fallbackRegex = Regex("""(?:ETB|Birr)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
                fallbackRegex.find(body)?.groupValues?.getOrNull(1)
            } ?: return null

        val amount = ParserUtils.parseAmount(rawAmount) ?: return null

        // 3. Extract Metadata
        val balance = ParserUtils.extractBalance(body)
        val referenceId = ParserUtils.extractReference(body)
        val accountNumber = ParserUtils.extractAccountNumber(body)

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = "CBE",
            senderAddress = sender,
            amount = amount,
            currency = "ETB",
            type = type,
            timestamp = timestamp,
            balanceAfter = balance,
            accountNumber = accountNumber,
            rawBody = body
        )
    }
}
