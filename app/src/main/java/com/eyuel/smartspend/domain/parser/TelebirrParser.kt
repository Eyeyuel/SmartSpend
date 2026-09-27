package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType

class TelebirrParser : BankParser {

    override val bankName: String = "Telebirr"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("telebirr") ||
                s == "127" ||
                b.contains("telebirr") ||
                b.contains("transaction id:") && (b.contains("you have transferred") || b.contains("you have received") || b.contains("you have paid"))
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        // 1. Transaction Type
        val isDebit = lowerBody.contains("transferred") ||
                lowerBody.contains("paid") ||
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
            """(?:transferred|paid|received|deposited|bought|withdrawn)\s+(?:ETB|Birr)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""",
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

        // Try to extract counterparty phone or merchant name: e.g. "to 0912****56" or "from 0911****12"
        val counterpartyRegex = Regex("""(?:to|from)\s+([0-9*+]{9,15}(?:\s*\([^)]+\))?|[A-Za-z0-9'\s]+(?=\s+on|\s+\(Merchant))""", RegexOption.IGNORE_CASE)
        val counterparty = counterpartyRegex.find(body)?.groupValues?.getOrNull(1)?.trim()

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = "Telebirr",
            senderAddress = sender,
            amount = amount,
            currency = "ETB",
            type = type,
            timestamp = timestamp,
            balanceAfter = balance,
            accountNumber = counterparty,
            rawBody = body
        )
    }
}
