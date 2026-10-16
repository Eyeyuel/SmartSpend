package com.eyuel.smartspend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerBottomSheet(
    initialStartDateMillis: Long?,
    initialEndDateMillis: Long?,
    onApplyRange: (startMillis: Long, endMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialStartUtc = initialStartDateMillis?.let { localMillisToUtcMidnight(it) }
    val initialEndUtc = initialEndDateMillis?.let { localMillisToUtcMidnight(it) }

    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartUtc,
        initialSelectedEndDateMillis = initialEndUtc,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Cannot select dates in the future (compare against today's UTC midnight)
                val todayUtcMidnight = localMillisToUtcMidnight(System.currentTimeMillis())
                return utcTimeMillis <= todayUtcMidnight
            }
        }
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    val isApplyEnabled = datePickerState.selectedStartDateMillis != null

    val applyAction: () -> Unit = {
        val startUtc = datePickerState.selectedStartDateMillis
        val endUtc = datePickerState.selectedEndDateMillis ?: startUtc
        if (startUtc != null && endUtc != null) {
            val localStart = utcMillisToLocalDayStart(startUtc)
            val localEnd = utcMillisToLocalDayEnd(endUtc)
            onApplyRange(localStart, localEnd)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = outlineColor)
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header Bar: Cancel / Title / Apply Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = onSurfaceVariant
                    )
                }

                Text(
                    text = "Select Date Range",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                TextButton(
                    onClick = applyAction,
                    enabled = isApplyEnabled
                ) {
                    Text(
                        text = "Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isApplyEnabled) primaryColor else onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            HorizontalDivider(
                color = outlineColor.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Preset Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val todayLocal = System.currentTimeMillis()

                QuickPresetChip(label = "Last 7 Days") {
                    val startUtc = localMillisToUtcMidnight(Calendar.getInstance().apply {
                        timeInMillis = todayLocal
                        add(Calendar.DAY_OF_YEAR, -6)
                    }.timeInMillis)
                    val endUtc = localMillisToUtcMidnight(todayLocal)
                    datePickerState.setSelection(startUtc, endUtc)
                }

                QuickPresetChip(label = "Last 14 Days") {
                    val startUtc = localMillisToUtcMidnight(Calendar.getInstance().apply {
                        timeInMillis = todayLocal
                        add(Calendar.DAY_OF_YEAR, -13)
                    }.timeInMillis)
                    val endUtc = localMillisToUtcMidnight(todayLocal)
                    datePickerState.setSelection(startUtc, endUtc)
                }

                QuickPresetChip(label = "Last 30 Days") {
                    val startUtc = localMillisToUtcMidnight(Calendar.getInstance().apply {
                        timeInMillis = todayLocal
                        add(Calendar.DAY_OF_YEAR, -29)
                    }.timeInMillis)
                    val endUtc = localMillisToUtcMidnight(todayLocal)
                    datePickerState.setSelection(startUtc, endUtc)
                }

                QuickPresetChip(label = "This Month") {
                    val startUtc = localMillisToUtcMidnight(Calendar.getInstance().apply {
                        timeInMillis = todayLocal
                        set(Calendar.DAY_OF_MONTH, 1)
                    }.timeInMillis)
                    val endUtc = localMillisToUtcMidnight(todayLocal)
                    datePickerState.setSelection(startUtc, endUtc)
                }

                QuickPresetChip(label = "Previous Month") {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = todayLocal
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, -1)
                    }
                    val startUtc = localMillisToUtcMidnight(cal.timeInMillis)
                    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, lastDay)
                    val endUtc = localMillisToUtcMidnight(cal.timeInMillis)
                    datePickerState.setSelection(startUtc, endUtc)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar Picker Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                DateRangePicker(
                    state = datePickerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = surfaceColor,
                        titleContentColor = onSurface,
                        headlineContentColor = primaryColor,
                        weekdayContentColor = onSurfaceVariant,
                        subheadContentColor = onSurface,
                        yearContentColor = onSurface,
                        currentYearContentColor = primaryColor,
                        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedYearContainerColor = primaryColor,
                        dayContentColor = onSurface,
                        disabledDayContentColor = onSurfaceVariant.copy(alpha = 0.35f),
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedDayContainerColor = primaryColor,
                        todayContentColor = primaryColor,
                        todayDateBorderColor = primaryColor,
                        dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        dayInSelectionRangeContainerColor = primaryColor.copy(alpha = 0.22f)
                    )
                )
            }

            // Bottom Confirm Button with Dynamic Selection Label
            val selectedStart = datePickerState.selectedStartDateMillis
            val selectedEnd = datePickerState.selectedEndDateMillis ?: selectedStart
            val rangeLabel = if (selectedStart != null) {
                val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                val s = fmt.format(Date(utcMillisToLocalDayStart(selectedStart)))
                val e = fmt.format(Date(utcMillisToLocalDayEnd(selectedEnd!!)))
                if (selectedStart == selectedEnd) s else "$s – $e"
            } else {
                "Select start and end dates"
            }

            Button(
                onClick = applyAction,
                enabled = isApplyEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = primaryColor.copy(alpha = 0.15f),
                    disabledContentColor = onSurfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = if (isApplyEnabled) "Apply: $rangeLabel" else "Select Date Range",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * Converts a timestamp in the device's local timezone to UTC midnight milliseconds,
 * which is what Material 3 DateRangePickerState requires for correct date representations.
 */
private fun localMillisToUtcMidnight(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
    }
    return utcCal.timeInMillis
}

/**
 * Converts UTC midnight millis returned by Material 3 DateRangePickerState to the start of the
 * same calendar day (00:00:00.000) in the user's local timezone.
 */
private fun utcMillisToLocalDayStart(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

/**
 * Converts UTC midnight millis returned by Material 3 DateRangePickerState to the end of the
 * same calendar day (23:59:59.999) in the user's local timezone.
 */
private fun utcMillisToLocalDayEnd(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return localCal.timeInMillis
}
