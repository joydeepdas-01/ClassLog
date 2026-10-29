package com.classlog.app.ui.setup.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.domain.time.TimeFormat

@Composable
fun AddEditTimeSlotDialog(
    initialTimeSlot: TimeSlotEntity? = null,
    use24h: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (label: String, startMinutes: Int, endMinutes: Int) -> Unit
) {
    var startH by remember { mutableIntStateOf(initialTimeSlot?.let { it.startMinutes / 60 } ?: 10) }
    var startM by remember { mutableIntStateOf(initialTimeSlot?.let { it.startMinutes % 60 } ?: 0) }
    var endH by remember { mutableIntStateOf(initialTimeSlot?.let { it.endMinutes / 60 } ?: 11) }
    var endM by remember { mutableIntStateOf(initialTimeSlot?.let { it.endMinutes % 60 } ?: 0) }

    val startMinutes = (startH * 60) + startM
    val endMinutes = (endH * 60) + endM
    val durationMinutes = endMinutes - startMinutes

    val defaultLabel = "${TimeFormat.clock(startMinutes, use24h)} - ${TimeFormat.clock(endMinutes, use24h)}"
    var customLabel by remember { mutableStateOf(initialTimeSlot?.label ?: "") }
    var isManualLabel by remember { mutableStateOf(initialTimeSlot != null) }

    val activeLabel = if (isManualLabel && customLabel.isNotBlank()) customLabel else defaultLabel
    val isValid = endMinutes > startMinutes && endMinutes <= 1440 && startMinutes >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTimeSlot == null) "Add Time Slot" else "Edit Time Slot") },
        text = {
            Column {
                Text(
                    text = "Time Range",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimePickerField(
                        label = "From",
                        hour = startH,
                        minute = startM,
                        use24h = use24h,
                        onTimeChange = { h, m ->
                            startH = h
                            startM = m
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    TimePickerField(
                        label = "To",
                        hour = endH,
                        minute = endM,
                        use24h = use24h,
                        onTimeChange = { h, m ->
                            endH = h
                            endM = m
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = if (isValid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isValid) "Duration: ${TimeFormat.duration(durationMinutes)}" else "End time must be after start time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isValid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = if (isManualLabel) customLabel else defaultLabel,
                    onValueChange = {
                        isManualLabel = true
                        customLabel = it
                    },
                    label = { Text("Display Label (optional override)") },
                    singleLine = true,
                    supportingText = { Text("Default: $defaultLabel") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isValid,
                onClick = {
                    onConfirm(activeLabel.trim(), startMinutes, endMinutes)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimePickerField(
    label: String,
    hour: Int,
    minute: Int,
    use24h: Boolean,
    onTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var hText by remember(hour) { mutableStateOf(hour.toString().padStart(2, '0')) }
    var mText by remember(minute) { mutableStateOf(minute.toString().padStart(2, '0')) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hText,
                onValueChange = { input ->
                    if (input.length <= 2 && input.all { it.isDigit() }) {
                        hText = input
                        val parsed = input.toIntOrNull()
                        if (parsed != null && parsed in 0..23) {
                            onTimeChange(parsed, minute)
                        }
                    }
                },
                placeholder = { Text("HH") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
            OutlinedTextField(
                value = mText,
                onValueChange = { input ->
                    if (input.length <= 2 && input.all { it.isDigit() }) {
                        mText = input
                        val parsed = input.toIntOrNull()
                        if (parsed != null && parsed in 0..59) {
                            onTimeChange(hour, parsed)
                        }
                    }
                },
                placeholder = { Text("MM") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        val totalMinutes = (hour * 60) + minute
        Text(
            text = TimeFormat.clock(totalMinutes, use24h),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
