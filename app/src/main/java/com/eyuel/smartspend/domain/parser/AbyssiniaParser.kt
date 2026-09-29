package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType

class AbyssiniaParser : BankParser {

    override val bankName: String = "Bank of Abyssinia"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("abyssinia") ||
                s.contains("boa") ||
                s == "8397" ||
                b.contains("bank of abyssinia") ||
                b.contains("bankofabyssinia.com") ||
                b.contains("abyssinia") ||
                b.contains("boa")
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        val isDebit = lowerBody.contains("debited") || lowerBody.contains("withdrawn") || lowerBody.contains("transferred") || lowerBody.contains("paid")
        val isCredit = lowerBody.contains("credited") || lowerBody.contains("deposited") || lowerBody.contains("received")

        val type = when {
            isDebit && !isCredit -> TransactionType.DEBIT
            isCredit && !isDebit -> TransactionType.CREDIT
            else -> return null
        }

        val amountRegex = Regex(
            """(?:debited\s+with|credited\s+with|amount\s+of)\s+(?:ETB|Birr)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1) ?: run {
            val fallback = Regex("""(?:ETB|Birr)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
            fallback.find(body)?.groupValues?.getOrNull(1)
        } ?: return null

        val amount = ParserUtils.parseAmount(rawAmount) ?: return null

        val balance = ParserUtils.extractBalance(body)
        val explicitRef = ParserUtils.extractReference(body)
        val referenceId = explicitRef ?: ParserUtils.generateDeterministicReference("BOA", timestamp, amount, body)
        val accountNumber = ParserUtils.extractAccountNumber(body)

        val (counterparty, suggestedCategory) = extractCounterpartyAndCategory(body, type)

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
            rawBody = body,
            counterparty = counterparty,
            suggestedCategory = suggestedCategory
        )
    }

    private fun extractCounterpartyAndCategory(body: String, type: TransactionType): Pair<String?, String> {
        // 0. Transfer to/from Telebirr
        if (body.contains("telebirr", ignoreCase = true)) {
            return Pair("Telebirr Transfer", ExpenseCategory.INTERNAL_TRANSFER)
        }

        // 1. "credited with ETB ... by <Person>."
        val byRegex = Regex("""by\s+([A-Za-z0-9\s.,]+?)(?=\.\s+Available|\.$)""", RegexOption.IGNORE_CASE)
        val byMatch = byRegex.find(body)
        if (byMatch != null) {
            val name = byMatch.groupValues[1].trim()
            if (name.isNotBlank()) {
                return Pair(name, ExpenseCategory.TRANSFER)
            }
        }

        // 2. "transferred ... to <Person>"
        val toRegex = Regex("""(?:to|for)\s+([A-Za-z0-9\s.,]+?)(?=\.\s+Available|\s+on|\.$)""", RegexOption.IGNORE_CASE)
        val toMatch = toRegex.find(body)
        if (toMatch != null) {
            val name = toMatch.groupValues[1].trim()
            if (name.isNotBlank() && !name.contains("fayda", ignoreCase = true)) {
                return Pair(name, ExpenseCategory.TRANSFER)
            }
        }

        return Pair(
            if (type == TransactionType.CREDIT) "BOA Deposit" else "BOA Debit",
            if (type == TransactionType.CREDIT) ExpenseCategory.TRANSFER else ExpenseCategory.OTHER
        )
    }
}
