@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.classlog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.classlog.app.domain.time.TimeFormat
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Pure helper for converting between canonical minutes since midnight (0..1439)
 * and 12-hour (Hour 1..12, Minute 0..59, AM/PM) or 24-hour representations.
 */
object WheelTimeConverter {
    fun to12Hour(minutes: Int): Triple<Int, Int, Boolean> {
        val safe = minutes.coerceIn(0, 1439)
        val h24 = safe / 60
        val minute = safe % 60
        val isPm = h24 >= 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return Triple(h12, minute, isPm)
    }

    fun toMinutes(hour12: Int, minute: Int, isPm: Boolean): Int {
        val h12Clamped = hour12.coerceIn(1, 12)
        val mClamped = minute.coerceIn(0, 59)
        val h24 = when {
            !isPm && h12Clamped == 12 -> 0
            !isPm -> h12Clamped
            isPm && h12Clamped == 12 -> 12
            else -> h12Clamped + 12
        }
        return (h24 * 60 + mClamped).coerceIn(0, 1439)
    }

    fun toMinutes24(hour24: Int, minute: Int): Int {
        return (hour24.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)).coerceIn(0, 1439)
    }
}

/**
 * Infinite vertical scroll-wheel column supporting seamless circular scrolling in both directions.
 *
 * Implemented using a virtualized repeated sequence centered around midpoint.
 * Automatically snaps each item to the vertical center, fires tactile haptics on item pass,
 * and seamlessly recenters without jumping when approaching bounds.
 */
@Composable
fun <T> InfiniteWheelColumn(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    formatItem: (T) -> String = { it.toString() },
    itemHeight: Dp = 46.dp,
    visibleItemsCount: Int = 5,
    modifier: Modifier = Modifier
) {
    val count = items.size
    val virtualCount = 100_000
    val midPoint = virtualCount / 2
    val baseOffset = midPoint - (midPoint % count)
    val initialListIndex = remember(initialIndex, count) {
        baseOffset + (initialIndex.coerceIn(0, count - 1))
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val centerVirtualIndex by remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (itemHeightPx > 0f && offset > itemHeightPx / 2f) {
                firstIndex + 1
            } else {
                firstIndex
            }
        }
    }

    val canonicalIndex by remember {
        derivedStateOf {
            Math.floorMod(centerVirtualIndex, count)
        }
    }

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Notify parent and trigger haptic tick whenever centered item changes
    var lastNotifiedIndex by remember { mutableIntStateOf(initialIndex) }
    LaunchedEffect(canonicalIndex) {
        if (canonicalIndex != lastNotifiedIndex) {
            lastNotifiedIndex = canonicalIndex
            onItemSelected(canonicalIndex)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Seamless silent recentering when scroll finishes near virtual boundaries
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val current = listState.firstVisibleItemIndex
            if (current < 10_000 || current > 90_000) {
                val canonical = Math.floorMod(current, count)
                val recentered = baseOffset + canonical
                listState.scrollToItem(recentered, listState.firstVisibleItemScrollOffset)
            }
        }
    }

    val topBottomPadding = itemHeight * ((visibleItemsCount - 1) / 2)

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = topBottomPadding),
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItemsCount)
    ) {
        items(
            count = virtualCount,
            key = { it }
        ) { virtualIndex ->
            val itemCanonicalIndex = Math.floorMod(virtualIndex, count)
            val item = items[itemCanonicalIndex]
            val distance = abs(virtualIndex - centerVirtualIndex)

            val textAlpha = when (distance) {
                0 -> 1f
                1 -> 0.55f
                2 -> 0.25f
                else -> 0.1f
            }
            val textScale = when (distance) {
                0 -> 1.0f
                1 -> 0.88f
                2 -> 0.76f
                else -> 0.65f
            }
            val fontWeight = when (distance) {
                0 -> FontWeight.Bold
                1 -> FontWeight.Medium
                else -> FontWeight.Normal
            }
            val textColor = if (distance == 0) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (virtualIndex != centerVirtualIndex) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(virtualIndex, 0)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatItem(item),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = (22 * textScale).sp,
                        fontWeight = fontWeight,
                        color = textColor.copy(alpha = textAlpha)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * MIUI Clock inspired Native-Style Scroll-Wheel Time Picker Dialog.
 *
 * Contains three vertically scrollable columns:
 * 1. Hour (01–12)
 * 2. Minute (00–59)
 * 3. Period (AM / PM)
 * (Or 2 columns: Hour 00–23, Minute 00–59 if [use24h] is true).
 *
 * All columns support true infinite circular wheel scrolling in both directions.
 */
@Composable
fun WheelTimePickerDialog(
    title: String = "Select Time",
    initialMinutes: Int,
    use24h: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val itemHeight = 46.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val surfaceColor = MaterialTheme.colorScheme.surface

    if (use24h) {
        // 24-Hour Wheel Mode: Hours 00..23, Minutes 00..59
        val initH24 = (initialMinutes.coerceIn(0, 1439) / 60)
        val initMin = (initialMinutes.coerceIn(0, 1439) % 60)

        val hours24 = remember { (0..23).toList() }
        val minutes = remember { (0..59).toList() }

        var selectedH24 by remember { mutableIntStateOf(initH24) }
        var selectedMin by remember { mutableIntStateOf(initMin) }

        val currentMinutes = WheelTimeConverter.toMinutes24(selectedH24, selectedMin)

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = surfaceColor,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Time Preview Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = TimeFormat.clock(currentMinutes, true),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Column Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "HOUR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = "MINUTE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2-Column Wheel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight * 5)
                            .drawWithContent {
                                drawContent()
                                // Top gradient fade
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0.4f), Color.Transparent),
                                        startY = 0f,
                                        endY = itemHeightPx * 1.5f
                                    )
                                )
                                // Bottom gradient fade
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, surfaceColor.copy(alpha = 0.4f), surfaceColor),
                                        startY = size.height - itemHeightPx * 1.5f,
                                        endY = size.height
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Center Selection Highlight Band
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfiniteWheelColumn(
                                items = hours24,
                                initialIndex = initH24,
                                onItemSelected = { idx -> selectedH24 = hours24[idx] },
                                formatItem = { it.toString().padStart(2, '0') },
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(itemHeight * 2.5f)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            InfiniteWheelColumn(
                                items = minutes,
                                initialIndex = initMin,
                                onItemSelected = { idx -> selectedMin = minutes[idx] },
                                formatItem = { it.toString().padStart(2, '0') },
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm(currentMinutes)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    } else {
        // 12-Hour MIUI Clock Wheel Mode: Hours 01..12, Minutes 00..59, AM/PM
        val (initH12, initMin, initIsPm) = WheelTimeConverter.to12Hour(initialMinutes)

        val hours12 = remember { (1..12).toList() }
        val minutes = remember { (0..59).toList() }
        val periods = remember { listOf("AM", "PM") }

        var selectedH12 by remember { mutableIntStateOf(initH12) }
        var selectedMin by remember { mutableIntStateOf(initMin) }
        var selectedIsPm by remember { mutableStateOf(initIsPm) }

        val currentMinutes = WheelTimeConverter.toMinutes(selectedH12, selectedMin, selectedIsPm)

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = surfaceColor,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Time Preview Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = TimeFormat.clock(currentMinutes, false),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Column Labels (Hour / Minute / Period)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "HOUR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = "MINUTE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = "PERIOD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3-Column Wheel Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight * 5)
                            .drawWithContent {
                                drawContent()
                                // Top gradient fade
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0.4f), Color.Transparent),
                                        startY = 0f,
                                        endY = itemHeightPx * 1.5f
                                    )
                                )
                                // Bottom gradient fade
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, surfaceColor.copy(alpha = 0.4f), surfaceColor),
                                        startY = size.height - itemHeightPx * 1.5f,
                                        endY = size.height
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Center Selection Highlight Band
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Hour Column (01..12)
                            InfiniteWheelColumn(
                                items = hours12,
                                initialIndex = initH12 - 1,
                                onItemSelected = { idx -> selectedH12 = hours12[idx] },
                                formatItem = { it.toString().padStart(2, '0') },
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f)
                            )

                            // Subtle vertical separator
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(itemHeight * 2.5f)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // 2. Minute Column (00..59)
                            InfiniteWheelColumn(
                                items = minutes,
                                initialIndex = initMin,
                                onItemSelected = { idx -> selectedMin = minutes[idx] },
                                formatItem = { it.toString().padStart(2, '0') },
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f)
                            )

                            // Subtle vertical separator
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(itemHeight * 2.5f)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // 3. Period Column (AM / PM)
                            InfiniteWheelColumn(
                                items = periods,
                                initialIndex = if (initIsPm) 1 else 0,
                                onItemSelected = { idx -> selectedIsPm = (idx == 1) },
                                formatItem = { it },
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm(currentMinutes)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}
