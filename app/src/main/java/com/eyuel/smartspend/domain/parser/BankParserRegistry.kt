package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ParsedTransaction

class BankParserRegistry(
    private val parsers: List<BankParser> = listOf(
        CbeParser(),
        TelebirrParser(),
        NibParser(),
        AbyssiniaParser(),
        GenericBankParser()
    )
) {

    fun parse(sender: String, body: String, timestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val trimmedBody = body.trim()
        val trimmedSender = sender.trim()

        if (trimmedBody.isEmpty()) return null

        for (parser in parsers) {
            if (parser.canParse(trimmedSender, trimmedBody)) {
                val result = parser.parse(trimmedSender, trimmedBody, timestamp)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    companion object {
        val defaultInstance: BankParserRegistry by lazy { BankParserRegistry() }
    }
}
