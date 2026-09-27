package com.eyuel.smartspend

import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.domain.parser.BankParserRegistry
import org.junit.Assert.*
import org.junit.Test

class BankParserTest {

    private val registry = BankParserRegistry.defaultInstance

    @Test
    fun testCbeDebitParsing() {
        val sms = "Dear Customer, your account 1000****4912 has been debited with ETB 1,500.00 on 24/09/2026. Your current balance is ETB 12,450.00. Ref: FT26268QXXXX. Thank you for banking with CBE."
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals("CBE", result?.bankName)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(1500.0, result?.amount ?: 0.0, 0.001)
        assertEquals(12450.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("FT26268QXXXX", result?.referenceId)
    }

    @Test
    fun testCbeCreditParsing() {
        val sms = "Dear Customer, your account 1000****4912 has been credited with ETB 5,000.00 on 24/09/2026 by Eyuel. Your current balance is ETB 17,450.00. Ref: FT2409110022."
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals("CBE", result?.bankName)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(5000.0, result?.amount ?: 0.0, 0.001)
        assertEquals(17450.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("FT2409110022", result?.referenceId)
    }

    @Test
    fun testTelebirrMerchantPayment() {
        val sms = "You have paid ETB 450.00 to Kaldi's Coffee (Merchant ID: 84920) on 24/09/2026 09:15:30. Transaction ID: 1048291055. Remaining balance is ETB 1,000.00."
        val result = registry.parse("telebirr", sms)

        assertNotNull(result)
        assertEquals("Telebirr", result?.bankName)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(450.0, result?.amount ?: 0.0, 0.001)
        assertEquals(1000.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("1048291055", result?.referenceId)
    }

    @Test
    fun testTelebirrTransferReceived() {
        val sms = "You have received ETB 1,500.00 from 0911****12 (Abebe) on 24/09/2026 11:05:00. Transaction ID: 1048291099. Your current balance is ETB 2,500.00."
        val result = registry.parse("127", sms)

        assertNotNull(result)
        assertEquals("Telebirr", result?.bankName)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(1500.0, result?.amount ?: 0.0, 0.001)
        assertEquals(2500.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("1048291099", result?.referenceId)
    }

    @Test
    fun testAbyssiniaDebit() {
        val sms = "Dear Customer, your account 10****491 has been debited with ETB 800.00 on 24-Sep-2026. Available balance is ETB 4,100.00. Ref: BOA24098492"
        val result = registry.parse("Abyssinia", sms)

        assertNotNull(result)
        assertEquals("Bank of Abyssinia", result?.bankName)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(800.0, result?.amount ?: 0.0, 0.001)
        assertEquals(4100.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("BOA24098492", result?.referenceId)
    }

    @Test
    fun testAwashGenericParsing() {
        val sms = "Dear Customer, your account 013****123 is debited by ETB 600.00 on 24/09/2026. Bal: ETB 8,200.00. Ref: AW2910482. Awash Bank."
        val result = registry.parse("AwashBank", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(600.0, result?.amount ?: 0.0, 0.001)
        assertEquals(8200.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("AW2910482", result?.referenceId)
    }

    @Test
    fun testNonFinancialSmsIgnored() {
        val sms = "Hello, your verification code is 849281. Do not share this with anyone."
        val result = registry.parse("Google", sms)

        assertNull(result)
    }
}
