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
    fun testTelebirrBankCreditTransfer() {
        val sms = """
            Dear Eyuel,
            You have received  ETB 1,000.00 by transaction number DIT38LM4HL on 2026-09-29 08:52:33 from Nib International Bank SC to your telebirr Account 251979409973 - Eyuel Teklu Berhe. Your current balance is ETB 1,050.75.
            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()
        val result = registry.parse("127", sms)

        assertNotNull(result)
        assertEquals("Telebirr", result?.bankName)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(1000.0, result?.amount ?: 0.0, 0.001)
        assertEquals(1050.75, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("DIT38LM4HL", result?.referenceId)
        assertEquals("Nib International Bank SC", result?.counterparty)
        assertEquals("251979409973", result?.accountNumber)
    }

    @Test
    fun testTelebirrP2PReceivedYonatan() {
        val sms = """
            Dear Eyuel 
            You have received ETB 290.00 from Yonatan Adera(2519****0409)  on 25/09/2026 13:28:52. Your transaction number is DIP54O52IR. Your current E-Money Account balance is ETB 3,594.03.
            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()
        val result = registry.parse("telebirr", sms)

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(290.0, result?.amount ?: 0.0, 0.001)
        assertEquals(3594.03, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("DIP54O52IR", result?.referenceId)
        assertEquals("Yonatan Adera", result?.counterparty)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.TRANSFER, result?.suggestedCategory)
    }

    @Test
    fun testTelebirrAirtimeRecharge() {
        val sms = """
            Dear Eyuel 
            You have recharged ETB 20.00 airtime for 943132747 on 06/09/2026 11:50:26. Your transaction number is DI60HN3KCU. Your current  balance is  ETB 941.03. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DI60HN3KCU
            For any support and information related to telebirr service
            Send SMS to 126 or Contact us via
            Telegram: https://t.me/telebirr 
            Facebook: https://facebook.com/telebirr or
            Visit our website :https://www.ethiotelecom.et/telebirr/  
            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()
        val result = registry.parse("127", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(20.0, result?.amount ?: 0.0, 0.001)
        assertEquals(941.03, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("DI60HN3KCU", result?.referenceId)
        assertEquals("Airtime (943132747)", result?.counterparty)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.BILLS_UTILITIES, result?.suggestedCategory)
    }

    @Test
    fun testTelebirrPackagePurchase() {
        val sms = """
            Dear Eyuel
            You have paid ETB 130.00 for package Monthly Voice plus Data Package: 1.2 GB and 168Min purchase made for 979409973 on 04/09/2026 20:12:34. Your transaction number is  DI47G4OFSZ. Your current balance is ETB 1,033.03.To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DI47G4OFSZ
            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()
        val result = registry.parse("telebirr", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(130.0, result?.amount ?: 0.0, 0.001)
        assertEquals(1033.03, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("DI47G4OFSZ", result?.referenceId)
        assertEquals("Monthly Voice plus Data Package: 1.2 GB and 168Min", result?.counterparty)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.BILLS_UTILITIES, result?.suggestedCategory)
    }

    @Test
    fun testTelebirrMerchantCafePayment() {
        val sms = """
            Dear Eyuel
            You have paid ETB 179.99 for Service Fee from 779976 - ELFIGN CAFE AND RESTAURANT PLC on 27/09/2026 16:10:29. Your transaction number is  DIR26YNV8K. Your current balance is ETB 2,840.04. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DIR26YNV8K
            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()
        val result = registry.parse("telebirr", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(179.99, result?.amount ?: 0.0, 0.001)
        assertEquals(2840.04, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("DIR26YNV8K", result?.referenceId)
        assertEquals("ELFIGN CAFE AND RESTAURANT PLC", result?.counterparty)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.FOOD_DINING, result?.suggestedCategory)
    }

    @Test
    fun testTelebirrBillPayment() {
        val sms = """
            Dear Eyuel
            You have paid ETB 2,499.99 to pay bill for 29101662370 on 28/09/2026 20:37:21. Your transaction number is DIS98BAX0H. Your telebirr account balance is  ETB 97.75. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DIS98BAX0H
            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()
        val result = registry.parse("telebirr", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(2499.99, result?.amount ?: 0.0, 0.001)
        assertEquals(97.75, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("DIS98BAX0H", result?.referenceId)
        assertEquals("Bill #29101662370", result?.counterparty)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.BILLS_UTILITIES, result?.suggestedCategory)
    }

    @Test
    fun testTelebirrP2PTransferDebit() {
        val sms = """
            Dear Eyuel 
            You have transferred ETB 20.00 to Yaleyi Kashun (2519****2542) on 29/09/2026 08:49:17. Your transaction number is DIT48LI8E0. The service fee is  ETB 0.87 and  15% VAT on the service fee is ETB 0.13. Your current E-Money Account  balance is ETB 50.75. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DIT48LI8E0.

            Thank you for using telebirr
            Ethio telecom
        """.trimIndent()
        val result = registry.parse("127", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(20.0, result?.amount ?: 0.0, 0.001) // Ensure service fee/VAT is NOT taken as amount
        assertEquals(50.75, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("DIT48LI8E0", result?.referenceId)
        assertEquals("Yaleyi Kashun", result?.counterparty)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.TRANSFER, result?.suggestedCategory)
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
