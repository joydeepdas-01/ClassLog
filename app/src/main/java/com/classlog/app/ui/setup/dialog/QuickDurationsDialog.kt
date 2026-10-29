package com.classlog.app.ui.setup.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classlog.app.domain.time.TimeFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickDurationsDialog(
    currentDurations: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val presetDurations = listOf(30, 45, 60, 75, 90, 120, 150, 180, 240)
    var selectedDurations by remember { mutableStateOf(currentDurations.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Duration Chips") },
        text = {
            Column {
                Text(
                    text = "Select durations that will appear as one-tap chips on the Quick Entry screen:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetDurations.forEach { minutes ->
                        val isSelected = selectedDurations.contains(minutes)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDurations = if (isSelected) {
                                    if (selectedDurations.size > 1) selectedDurations - minutes else selectedDurations
                                } else {
                                    selectedDurations + minutes
                                }
                            },
                            label = { Text(TimeFormat.duration(minutes)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedDurations.toList().sorted())
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
