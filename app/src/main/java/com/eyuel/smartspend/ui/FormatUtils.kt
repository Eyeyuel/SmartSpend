package com.eyuel.smartspend.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object FormatUtils {

    private val decimalFormat = ThreadLocal.withInitial {
        val symbols = DecimalFormatSymbols(Locale.US)
        DecimalFormat("#,##0.00", symbols)
    }

    private val shortDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    }

    private val fullDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault())
    }

    private val dateCache = ConcurrentHashMap<Long, String>()

    fun formatShortDate(timestamp: Long): String {
        return dateCache.computeIfAbsent(timestamp) {
            shortDateFormat.get()?.format(Date(it)) ?: ""
        }
    }

    fun formatFullDate(timestamp: Long): String {
        return fullDateFormat.get()?.format(Date(timestamp)) ?: ""
    }

    fun formatAmount(amount: Double): String {
        return decimalFormat.get()?.format(amount) ?: String.format(Locale.US, "%.2f", amount)
    }
}
