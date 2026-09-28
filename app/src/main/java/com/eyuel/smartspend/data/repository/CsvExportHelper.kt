package com.eyuel.smartspend.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.eyuel.smartspend.data.local.TransactionEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExportHelper {

    fun exportAndShareTransactions(context: Context, transactions: List<TransactionEntity>): Boolean {
        if (transactions.isEmpty()) return false

        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "SmartSpend_Transactions_$timestampStr.csv")

            FileWriter(file).use { writer ->
                // Write Header
                writer.append("ID,Date,Bank,Type,Amount,Currency,Category,Description,Reference_ID,Balance_After,Account_Number,Raw_SMS\n")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                for (t in transactions) {
                    val dateFormatted = dateFormat.format(Date(t.timestamp))
                    val escapedDesc = escapeCsv(t.description)
                    val escapedCategory = escapeCsv(t.category)
                    val escapedRef = escapeCsv(t.referenceId ?: "")
                    val escapedAccount = escapeCsv(t.accountNumber ?: "")
                    val escapedRaw = escapeCsv(t.rawBody)

                    writer.append("${t.id},")
                    writer.append("\"$dateFormatted\",")
                    writer.append("\"${t.bankName}\",")
                    writer.append("${t.type.name},")
                    writer.append("${t.amount},")
                    writer.append("${t.currency},")
                    writer.append("\"$escapedCategory\",")
                    writer.append("\"$escapedDesc\",")
                    writer.append("\"$escapedRef\",")
                    writer.append("${t.balanceAfter ?: ""},")
                    writer.append("\"$escapedAccount\",")
                    writer.append("\"$escapedRaw\"\n")
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SmartSpend Transactions Export")
                putExtra(Intent.EXTRA_TEXT, "Exported ${transactions.size} transactions from SmartSpend.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Transactions CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ")
    }
}
