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
    fun testCbeTransferOutWithFees() {
        val sms = """
            Dear  Eyuel Teklu Berhe You have successfully transferred ETB1150.00 from account 1**7868 to account 1**9667 (Alemtsehay Birhane Kahsay). Service charge of ETB 1.00 and VAT(15%) of ETB0.15 and Disaster Recovery(5%) of 0.05 with total of ETB1151.20 .Your current balance is ETB24,188.28. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxHb3KSsLvR50iT9X  for feedback: https://forms.gle/kGNGQpG3mQCCk3iD6
        """.trimIndent()
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals("CBE", result?.bankName)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(1150.0, result?.amount ?: 0.0, 0.001)
        assertEquals(24188.28, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("v2-hfHCxHb3KSsLvR50iT9X", result?.referenceId)
        assertEquals("Alemtsehay Birhane Kahsay", result?.counterparty)
        assertEquals("1**7868", result?.accountNumber)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.TRANSFER, result?.suggestedCategory)
    }

    @Test
    fun testCbeTransferReceived() {
        val sms = """
            Dear Eyuel Teklu Berhe You have received ETB 300.00 from account 1**7426 (Minilik Belachew Balkideru) to your account 1**7868. Your current balance is ETB25,490.09. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxHaJLoC0SNH0cnM9  for feedback: https://forms.gle/kGNGQpG3mQCCk3iD6
        """.trimIndent()
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(300.0, result?.amount ?: 0.0, 0.001)
        assertEquals(25490.09, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("v2-hfHCxHaJLoC0SNH0cnM9", result?.referenceId)
        assertEquals("Minilik Belachew Balkideru", result?.counterparty)
        assertEquals("1**7868", result?.accountNumber)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.TRANSFER, result?.suggestedCategory)
    }

    @Test
    fun testCbeBranchDepositReceipt() {
        val sms = """
            Dear Mr Eyuel your Account 1****7868 has been credited with ETB 500.00. Your Current Balance is ETB 26341.29. Thank you for Banking with CBE! for Reciept https://apps.cbe.com.et:100/BranchReceipt/FT2626535R4H&11207868
        """.trimIndent()
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(500.0, result?.amount ?: 0.0, 0.001)
        assertEquals(26341.29, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("FT2626535R4H", result?.referenceId)
        assertEquals("1****7868", result?.accountNumber)
    }

    @Test
    fun testCbeDebitTransactionDotFormat() {
        val sms = """
            Dear Eyuel Teklu Berhe A debit transaction of ETB 20.0. has occurred on your account 1****7868. Service charge of ETB 0.00 and VAT(15%) of 0.0 and Disaster Recovery(5%) of 0.00 with total of ETB20.00 .Your current balance is ETB36,050.34. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxGW4hEH9Xr1KbbG1  for feedback: https://forms.gle/kGNGQpG3mQCCk3iD6
        """.trimIndent()
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(20.0, result?.amount ?: 0.0, 0.001)
        assertEquals(36050.34, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("v2-hfHCxGW4hEH9Xr1KbbG1", result?.referenceId)
        assertEquals("1****7868", result?.accountNumber)
    }

    @Test
    fun testCbeDirectDebitAlert() {
        val sms = """
            Dear Mr Eyuel your Account 1****7868 has been debited with ETB 4023 including Service charge ETB0.00ETB0.00 and VAT(15%) . Your Current Balance is ETB 20165.28. Thank you for Banking with CBE!. For feedback https://shorturl.at/auUX0
        """.trimIndent()
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(4023.0, result?.amount ?: 0.0, 0.001)
        assertEquals(20165.28, result?.balanceAfter ?: 0.0, 0.001)
        assertNotNull(result?.referenceId) // Deterministic signature
        assertEquals("1****7868", result?.accountNumber)
    }

    @Test
    fun testCbeHighValueTransferReceived() {
        val sms = """
            Dear Eyuel Teklu Berhe You have received ETB 12,500.00 from account 1**4135 (Saron Seife Yirgu) to your account 1**7868. Your current balance is ETB55,566.00. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxG6SmYfM6Fmn2cqp  for feedback: https://forms.gle/kGNGQpG3mQCCk3iD6
        """.trimIndent()
        val result = registry.parse("CBE", sms)

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(12500.0, result?.amount ?: 0.0, 0.001)
        assertEquals(55566.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("v2-hfHCxG6SmYfM6Fmn2cqp", result?.referenceId)
        assertEquals("Saron Seife Yirgu", result?.counterparty)
        assertEquals("1**7868", result?.accountNumber)
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
    fun testNibDebitToTelebirr() {
        val sms = """
            Dear Customer your Account ****7659 has been Debited with ETB -1,001.04 On 29 SEP 2026 to telebirr account number  with service charge ETB0.87, disaster commission(5%) ETB0.04 and VAT(15%) ETB0.13 on service charge Ref: FT26272JNYFB. Your Current Balance is ETB 23,047.92. For further Info call 9698. Join our social medias using the following link https://www.nibbanksc.com/SocialMedia/. Thank you for Banking with NIB!
        """.trimIndent()
        val result = registry.parse("NIB", sms)

        assertNotNull(result)
        assertEquals("Nib International Bank", result?.bankName)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(1001.04, result?.amount ?: 0.0, 0.001)
        assertEquals(23047.92, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("FT26272JNYFB", result?.referenceId)
        assertEquals("Telebirr Transfer", result?.counterparty)
        assertEquals("****7659", result?.accountNumber)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.TRANSFER, result?.suggestedCategory)
    }

    @Test
    fun testNibCreditFromPerson() {
        val sms = """
            Dear Customer your Account ****7659 has been Credited with ETB 25,000.00     On 03 SEP 2026  from SARON SEIFE YIRGU Ref: FT2624630K1Z. Your Current Balance is ETB 25,050.00. For further Info call 9698. Join our social medias using the following link https://www.nibbanksc.com/SocialMedia/. Thank you for Banking with NIB!
        """.trimIndent()
        val result = registry.parse("9698", sms)

        assertNotNull(result)
        assertEquals("Nib International Bank", result?.bankName)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(25000.0, result?.amount ?: 0.0, 0.001)
        assertEquals(25050.0, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("FT2624630K1Z", result?.referenceId)
        assertEquals("SARON SEIFE YIRGU", result?.counterparty)
        assertEquals("****7659", result?.accountNumber)
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
    fun testAbyssiniaCreditWithSlip() {
        val sms = """
            Dear Eyuel, your account 1*75 was credited with ETB 1,565.00 by Yididya Rezene Abrha. Available Balance: ETB 2,034.01.
            Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT26264JFPVM92688
            Feedback: https://cs.bankofabyssinia.com/cs/?trx=CFT26264JFPVM
            Link your Fayda: https://cs.bankofabyssinia.com/fayda_connect 
            For help, call 8397 (24/7 Toll-Free). Bank of Abyssinia.
        """.trimIndent()
        val result = registry.parse("BOA", sms)

        assertNotNull(result)
        assertEquals("Bank of Abyssinia", result?.bankName)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(1565.0, result?.amount ?: 0.0, 0.001)
        assertEquals(2034.01, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("FT26264JFPVM92688", result?.referenceId)
        assertEquals("Yididya Rezene Abrha", result?.counterparty)
        assertEquals("1*75", result?.accountNumber)
        assertEquals(com.eyuel.smartspend.domain.model.ExpenseCategory.TRANSFER, result?.suggestedCategory)
    }

    @Test
    fun testAbyssiniaDebitWithSlip() {
        val sms = """
            Dear Eyuel, your account 1*75 was debited with ETB 1,005.41. Available Balance: ETB 231.28.
            Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT262020CXSJ08675
            Feedback: https://cs.bankofabyssinia.com/cs/?trx=DFT262020CXSJ
            Link your Fayda: https://cs.bankofabyssinia.com/fayda_connect 
            For help, call 8397 (24/7 Toll-Free). Bank of Abyssinia.
        """.trimIndent()
        val result = registry.parse("8397", sms)

        assertNotNull(result)
        assertEquals("Bank of Abyssinia", result?.bankName)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(1005.41, result?.amount ?: 0.0, 0.001)
        assertEquals(231.28, result?.balanceAfter ?: 0.0, 0.001)
        assertEquals("FT262020CXSJ08675", result?.referenceId)
        assertEquals("1*75", result?.accountNumber)
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
