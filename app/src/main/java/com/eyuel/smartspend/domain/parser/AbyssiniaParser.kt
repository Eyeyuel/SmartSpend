package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType

class AbyssiniaParser : BankParser {

    override val bankName: String = "Bank of Abyssinia"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("abyssinia") ||
                s.contains("boa") ||
                b.contains("abyssinia") ||
                b.contains("boa")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        val isDebit = lowerBody.contains("debited") || lowerBody.contains("withdrawn") || lowerBody.contains("transferred")
        val isCredit = lowerBody.contains("credited") || lowerBody.contains("deposited") || lowerBody.contains("received")

        val type = when {
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> return null
        }

        val amountRegex = Regex(
            """(?:debited\s+with|credited\s+with|amount\s+of)\s+(?:ETB|Birr)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1) ?: run {
            val fallback = Regex("""(?:ETB|Birr)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
            fallback.find(body)?.groupValues?.getOrNull(1)
        } ?: return null

        val amount = ParserUtils.parseAmount(rawAmount) ?: return null

        val balance = ParserUtils.extractBalance(body)
        val referenceId = ParserUtils.extractReference(body)
        val accountNumber = ParserUtils.extractAccountNumber(body)

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = "Bank of Abyssinia",
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
