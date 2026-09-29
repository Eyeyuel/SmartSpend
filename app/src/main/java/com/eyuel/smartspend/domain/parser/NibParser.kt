package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class NibParser : BankParser {

    override val bankName: String = "Nib International Bank"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("nib") ||
                s == "9698" ||
                b.contains("banking with nib") ||
                b.contains("nibbanksc.com") ||
                b.contains("nib international bank") ||
                (b.contains("nib") && (b.contains("debited") || b.contains("credited") || b.contains("transferred")))
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val lowerBody = body.lowercase()

        // 1. Transaction Type
        val isDebit = lowerBody.contains("debited") || lowerBody.contains("transferred") || lowerBody.contains("withdrawn")
        val isCredit = lowerBody.contains("credited") || lowerBody.contains("deposited") || lowerBody.contains("received")

        val type = when {
            isDebit && !isCredit -> TransactionType.DEBIT
            isCredit && !isDebit -> TransactionType.CREDIT
            else -> return null
        }

        // 2. Extract Amount (Handles optional minus sign, e.g., "ETB -1,001.04" or "ETB 25,000.00")
        val amountRegex = Regex(
            """(?:debited\s+with|credited\s+with|transferred|deposited)\s+(?:ETB|Birr)?\s*(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        )
        val match = amountRegex.find(body)
        val rawAmount = match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: run {
                val fallbackRegex = Regex("""(?:ETB|Birr)\s*(-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                fallbackRegex.find(body)?.groupValues?.getOrNull(1)
            } ?: return null

        val sanitizedAmount = rawAmount.replace("-", "").trim()
        val amount = ParserUtils.parseAmount(sanitizedAmount) ?: return null

        // 3. Extract Metadata
        val balance = ParserUtils.extractBalance(body)
        val explicitRef = ParserUtils.extractReference(body)
        val referenceId = explicitRef ?: ParserUtils.generateDeterministicReference("NIB", timestamp, amount, body)
        val accountNumber = extractAccountNumber(body)

        // 4. Extract Counterparty and Smart Category
        val (counterparty, suggestedCategory) = extractCounterpartyAndCategory(body, type)

        // 5. Parse Explicit Date if present (e.g., "On 29 SEP 2026")
        val parsedTime = extractExplicitDate(body) ?: timestamp

        return ParsedTransaction(
            referenceId = referenceId,
            bankName = "Nib International Bank",
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

        // 1. "to telebirr account"
        if (lower.contains("telebirr")) {
            return Pair("Telebirr Transfer", ExpenseCategory.INTERNAL_TRANSFER)
        }

        // 2. "from <Person> Ref:" or "to <Person> Ref:"
        val partyRegex = Regex("""(?:to|from)\s+([A-Za-z0-9\s.,]+?)(?=\s+(?:with|Ref:|\.))""", RegexOption.IGNORE_CASE)
        val partyMatch = partyRegex.find(body)
        if (partyMatch != null) {
            val name = partyMatch.groupValues[1].trim()
            if (name.isNotBlank() && !name.equals("your account", ignoreCase = true)) {
                return Pair(name, ExpenseCategory.TRANSFER)
            }
        }

        return Pair(
            if (type == TransactionType.CREDIT) "Nib Deposit" else "Nib Debit",
            if (type == TransactionType.CREDIT) ExpenseCategory.TRANSFER else ExpenseCategory.OTHER
        )
    }

    private fun extractAccountNumber(body: String): String? {
        val accRegex = Regex("""your\s+Account\s+([0-9*xX]{4,})""", RegexOption.IGNORE_CASE)
        return accRegex.find(body)?.groupValues?.getOrNull(1) ?: ParserUtils.extractAccountNumber(body)
    }

    private fun extractExplicitDate(body: String): Long? {
        val dateRegex = Regex("""On\s+([0-9]{1,2}\s+[A-Za-z]{3}\s+[0-9]{4})""", RegexOption.IGNORE_CASE)
        val rawDate = dateRegex.find(body)?.groupValues?.getOrNull(1) ?: return null

        return try {
            SimpleDateFormat("dd MMM yyyy", Locale.US).parse(rawDate)?.time
        } catch (_: Exception) {
            null
        }
    }
}
