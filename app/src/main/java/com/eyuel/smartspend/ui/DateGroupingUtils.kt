package com.eyuel.smartspend.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DateGroupingUtils {

    private val labelCache = ConcurrentHashMap<Long, String>()
    private val dayFormat = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())

    fun getDayStartMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getDateLabel(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val dayStart = getDayStartMillis(timestamp)
        val todayStart = getDayStartMillis(nowMillis)
        val oneDayMillis = 24 * 60 * 60 * 1000L

        return when (dayStart) {
            todayStart -> "Today"
            todayStart - oneDayMillis -> "Yesterday"
            else -> {
                labelCache.computeIfAbsent(dayStart) {
                    val calCurrent = Calendar.getInstance().apply { timeInMillis = nowMillis }
                    val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }

                    if (calCurrent.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR)) {
                        dayFormat.format(Date(timestamp))
                    } else {
                        yearFormat.format(Date(timestamp))
                    }
                }
            }
        }
    }
}
