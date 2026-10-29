package com.classlog.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.domain.time.TimeFormat
import com.classlog.app.ui.setup.dialog.AddEditTimeSlotDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTimeSlotsScreen(
    timeSlots: List<TimeSlotEntity>,
    use24h: Boolean,
    onBack: () -> Unit,
    onSaveTimeSlot: (label: String, startMinutes: Int, endMinutes: Int) -> Unit,
    onUpdateTimeSlot: (timeSlot: TimeSlotEntity) -> Unit,
    onToggleTimeSlotActive: (timeSlot: TimeSlotEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimeSlotEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Time Slots") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Time Slot")
            }
        }
    ) { padding ->
        if (timeSlots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No time slots configured.\nTap + to add a time slot.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(timeSlots, key = { it.id }) { slot ->
                    TimeSlotItemCard(
                        slot = slot,
                        use24h = use24h,
                        onEdit = { editingSlot = slot },
                        onToggleActive = { onToggleTimeSlotActive(slot) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTimeSlotDialog(
            initialTimeSlot = null,
            use24h = use24h,
            onDismiss = { showAddDialog = false },
            onConfirm = { label, start, end ->
                onSaveTimeSlot(label, start, end)
                showAddDialog = false
            }
        )
    }

    editingSlot?.let { slot ->
        AddEditTimeSlotDialog(
            initialTimeSlot = slot,
            use24h = use24h,
            onDismiss = { editingSlot = null },
            onConfirm = { label, start, end ->
                onUpdateTimeSlot(slot.copy(label = label, startMinutes = start, endMinutes = end))
                editingSlot = null
            }
        )
    }
}

@Composable
private fun TimeSlotItemCard(
    slot: TimeSlotEntity,
    use24h: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    val duration = slot.endMinutes - slot.startMinutes
    val cardAlpha = if (slot.isActive) 1f else 0.65f
    val clockRange = "${TimeFormat.clock(slot.startMinutes, use24h)} - ${TimeFormat.clock(slot.endMinutes, use24h)}"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (slot.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = slot.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = TimeFormat.duration(duration),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (!slot.isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "Hidden",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = clockRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit time slot",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleActive) {
                    Icon(
                        imageVector = if (slot.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (slot.isActive) "Hide time slot" else "Activate time slot",
                        tint = if (slot.isActive) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
