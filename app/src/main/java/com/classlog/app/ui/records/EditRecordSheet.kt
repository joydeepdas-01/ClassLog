package com.classlog.app.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classlog.app.data.datastore.AppSettings
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.domain.model.Issue
import com.classlog.app.domain.time.TimeFormat
import com.classlog.app.domain.validation.SessionValidator
import com.classlog.app.ui.components.TimeSelectionDialog
import com.classlog.app.ui.components.WheelTimePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditRecordSheet(
    sheetState: SheetState,
    session: WorkSessionEntity,
    categories: List<CategoryEntity>,
    batches: List<BatchEntity>,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (
        session: WorkSessionEntity,
        newStart: Int,
        newEnd: Int,
        newCategory: CategoryEntity,
        newBatch: BatchEntity?,
        newNote: String
    ) -> Unit
) {
    val initialCategory = categories.find { it.id == session.categoryId }
        ?: CategoryEntity(id = session.categoryId, name = session.categoryNameSnapshot, isTeaching = session.isTeachingSnapshot)

    val initialBatch = session.batchId?.let { bId ->
        batches.find { it.id == bId } ?: session.batchNameSnapshot?.let { BatchEntity(id = bId, name = it) }
    }

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedBatch by remember { mutableStateOf(initialBatch) }
    var startMinutes by remember { mutableIntStateOf(session.startMinutes) }
    var endMinutes by remember { mutableIntStateOf(session.endMinutes) }
    var note by remember { mutableStateOf(session.note) }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val isTeaching = selectedCategory.isTeaching
    val durationMinutes = endMinutes - startMinutes
    val use24h = settings.use24HourFormat

    val validationIssues = SessionValidator.validate(
        start = startMinutes,
        end = endMinutes,
        isTeaching = isTeaching,
        batchId = selectedBatch?.id
    )
    val isValid = validationIssues.isEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val dateStr = LocalDate.ofEpochDay(session.dateEpochDay)
                    .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Work Type Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "WORK TYPE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory.id == category.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = category
                                if (!category.isTeaching) selectedBatch = null
                            },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Batch Section (Always visible, with required/optional indicator)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BATCH",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTeaching) "(required for teaching)" else "(optional)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTeaching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    batches.forEach { batch ->
                        val isSelected = selectedBatch?.id == batch.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBatch = batch },
                            label = { Text(batch.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }

            // Time Section: Start and End Time with Native Scroll-Wheel Picker
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIME",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (durationMinutes > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Duration: ${TimeFormat.duration(durationMinutes)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Dual Cards for Start Time and End Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // START TIME CARD
                    Surface(
                        onClick = { showFromPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "START TIME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { showFromPicker = true },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Start Time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TimeFormat.clock(startMinutes, use24h),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // END TIME CARD
                    Surface(
                        onClick = { showToPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "END TIME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(
                                    onClick = { showToPicker = true },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit End Time", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TimeFormat.clock(endMinutes, use24h),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Note Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Validation Warning
            if (!isValid) {
                val errorMsg = when (validationIssues.first()) {
                    Issue.END_NOT_AFTER_START -> "End time must be after start time."
                    Issue.END_AFTER_MIDNIGHT -> "Session cannot exceed 24:00 (midnight)."
                    Issue.BATCH_REQUIRED -> "Please select a student batch for teaching."
                }
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSave(session, startMinutes, endMinutes, selectedCategory, selectedBatch, note)
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Update")
                }
            }
        }
    }

    if (showFromPicker) {
        WheelTimePickerDialog(
            title = "Start Time",
            initialMinutes = startMinutes,
            use24h = use24h,
            onDismiss = { showFromPicker = false },
            onConfirm = { mins ->
                startMinutes = mins
                if (endMinutes <= mins) endMinutes = (mins + 60).coerceAtMost(1440)
                showFromPicker = false
            }
        )
    }

    if (showToPicker) {
        WheelTimePickerDialog(
            title = "End Time",
            initialMinutes = endMinutes,
            use24h = use24h,
            onDismiss = { showToPicker = false },
            onConfirm = { mins ->
                endMinutes = mins
                if (startMinutes >= mins) startMinutes = (mins - 60).coerceAtLeast(0)
                showToPicker = false
            }
        )
    }
}
