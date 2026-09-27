package com.eyuel.smartspend.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.eyuel.smartspend.SmartSpendApp
import com.eyuel.smartspend.domain.parser.BankParserRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group messages by originating address in case of multi-part messages
        val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: "" }

        for ((sender, parts) in messagesBySender) {
            val fullBody = parts.joinToString(separator = "") { it.displayMessageBody ?: "" }
            val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

            val parsed = BankParserRegistry.defaultInstance.parse(sender, fullBody, timestamp)
            if (parsed != null) {
                Log.d("SmartSpend", "Intercepted bank SMS from $sender: ${parsed.type} ${parsed.amount} ${parsed.currency}")

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = SmartSpendApp.instance.repository
                        val saved = repository.saveParsedTransaction(parsed)
                        if (saved) {
                            Log.d("SmartSpend", "Successfully recorded new transaction: ${parsed.bankName} - ${parsed.amount}")
                        }
                    } catch (e: Exception) {
                        Log.e("SmartSpend", "Failed to save transaction: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
