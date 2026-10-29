package com.classlog.app.ui.components

import androidx.compose.runtime.Composable

/**
 * Time selection dialog that delegates to [WheelTimePickerDialog] for a native MIUI scroll-wheel experience.
 */
@Composable
fun TimeSelectionDialog(
    title: String,
    initialMinutes: Int,
    use24h: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    WheelTimePickerDialog(
        title = title,
        initialMinutes = initialMinutes,
        use24h = use24h,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
