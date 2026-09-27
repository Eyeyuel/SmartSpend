package com.eyuel.smartspend.domain.parser

import com.eyuel.smartspend.domain.model.ParsedTransaction

interface BankParser {
    val bankName: String

    /**
     * Determines whether this parser can handle the given SMS based on sender address and content.
     */
    fun canParse(sender: String, body: String): Boolean

    /**
     * Parses the SMS message into a structured ParsedTransaction. Returns null if parsing fails.
     */
    fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction?
}
