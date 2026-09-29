package com.eyuel.smartspend.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.eyuel.smartspend.domain.model.ParsedTransaction
import com.eyuel.smartspend.domain.parser.BankParserRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncResult(
    val scannedCount: Int,
    val importedCount: Int
)

class SmsSyncManager(
    private val context: Context,
    private val repository: TransactionRepository
) {

    suspend fun syncInbox(maxDaysBack: Int = 180): SyncResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val sinceTime = startTime - (maxDaysBack.toLong() * 24 * 60 * 60 * 1000)

        // SQL-level filtering: Only query messages from banking senders or containing financial keywords
        // This eliminates 90%+ of personal chats, OTPs, and spam at the SQLite level!
        val selection = "(${Telephony.Sms.DATE} >= ?) AND (" +
                "${Telephony.Sms.ADDRESS} LIKE '%cbe%' OR " +
                "${Telephony.Sms.ADDRESS} LIKE '%telebirr%' OR " +
                "${Telephony.Sms.ADDRESS} = '127' OR " +
                "${Telephony.Sms.ADDRESS} LIKE '%nib%' OR " +
                "${Telephony.Sms.ADDRESS} = '9698' OR " +
                "${Telephony.Sms.ADDRESS} LIKE '%abyssinia%' OR " +
                "${Telephony.Sms.ADDRESS} LIKE '%boa%' OR " +
                "${Telephony.Sms.ADDRESS} = '8397' OR " +
                "${Telephony.Sms.ADDRESS} LIKE '%awash%' OR " +
                "${Telephony.Sms.ADDRESS} LIKE '%dashen%' OR " +
                "${Telephony.Sms.ADDRESS} LIKE '%bank%' OR " +
                "${Telephony.Sms.BODY} LIKE '%debited%' OR " +
                "${Telephony.Sms.BODY} LIKE '%credited%' OR " +
                "${Telephony.Sms.BODY} LIKE '%transferred%' OR " +
                "${Telephony.Sms.BODY} LIKE '%recharged%' OR " +
                "${Telephony.Sms.BODY} LIKE '%package%')"

        val selectionArgs = arrayOf(sinceTime.toString())
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        val cursor: Cursor? = try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        } catch (e: SecurityException) {
            Log.e("SmartSpend", "Permission denied reading SMS inbox: ${e.message}")
            return@withContext SyncResult(0, 0)
        } catch (e: Exception) {
            Log.e("SmartSpend", "Error scanning SMS inbox: ${e.message}", e)
            return@withContext SyncResult(0, 0)
        }

        var scanned = 0
        val parsedList = mutableListOf<ParsedTransaction>()

        cursor?.use { c ->
            val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                scanned++
                val address = if (addressIdx != -1) c.getString(addressIdx) ?: "" else ""
                val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                val parsed = BankParserRegistry.defaultInstance.parse(address, body, date)
                if (parsed != null) {
                    parsedList.add(parsed)
                }
            }
        }

        val healedCount = repository.rescanAndHealTransactions()
        val imported = repository.saveParsedTransactions(parsedList)
        val durationMs = System.currentTimeMillis() - startTime
        Log.d("SmartSpend", "Optimized SMS Sync: Scanned $scanned candidate SMS in ${durationMs}ms, imported $imported new transactions, healed $healedCount existing")

        SyncResult(scannedCount = scanned, importedCount = imported + healedCount)
    }
}
