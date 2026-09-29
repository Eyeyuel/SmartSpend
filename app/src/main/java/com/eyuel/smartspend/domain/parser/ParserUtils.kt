package com.eyuel.smartspend.domain.parser

object ParserUtils {

    fun parseAmount(raw: String): Double? {
        val sanitized = raw.replace(",", "").trim()
        return sanitized.toDoubleOrNull()
    }

    fun extractBalance(text: String): Double? {
        // Matches e.g.:
        // "current balance is ETB 12,450.00"
        // "balance is: 1,500.50"
        // "available balance: ETB 500"
        // "Bal: ETB 10,000.00"
        // "Remaining balance is ETB 250.00"
        val regex = Regex(
            """(?:current\s+balance|available\s+balance|remaining\s+balance|balance|bal)[\s:is]+(?:ETB|Birr|USD)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(text)
        return match?.groupValues?.getOrNull(1)?.let { parseAmount(it) }
    }

    fun extractReference(text: String): String? {
        // Matches e.g.:
        // Telebirr: "Your transaction number is DIS17HYCLP" or "receipt/DIS17HYCLP"
        // CBE: "mbreciept.cbe.com.et/v2-hfHCxHb3KSsLvR50iT9X" or "Ref: FT240912..."
        // BOA / Awash / Dashen: "Txn ID: 1048291048", "Transaction ID: 9482910", "Trans. ID: 128471"
        val regex = Regex(
            """(?:transaction\s+number\s+is\s+|receipt/|mbreciept\.cbe\.com\.et/|Ref(?:\s*no|\.|\s*id)?[\s:]*|Txn\s*ID[\s:]*|Transaction\s*ID[\s:]*|Trans(?:\.|\s*)ID[\s:]*)([A-Za-z0-9\-_]{6,})""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(text)?.groupValues?.getOrNull(1)
    }

    fun generateDeterministicReference(
        sender: String,
        timestamp: Long,
        amount: Double,
        rawBody: String
    ): String {
        val explicit = extractReference(rawBody)
        if (explicit != null) return explicit

        val cleanSender = sender.replace(Regex("[^A-Za-z0-9]"), "").lowercase().ifEmpty { "bank" }
        val bodyHash = (rawBody.trim().hashCode().toLong() and 0xFFFFFFFFL).toString(16)
        return "sig_${cleanSender}_${timestamp}_${amount}_${bodyHash}"
    }

    fun extractAccountNumber(text: String): String? {
        // Matches e.g.:
        // "account 1000****4912"
        // "A/C 10****491"
        // "account number: 1000123456"
        val regex = Regex(
            """(?:account(?:\s*number)?|A/C)[\s:]*([0-9*xX]{4,})""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(text)?.groupValues?.getOrNull(1)
    }
}
