package com.classlog.app.ui.summary

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classlog.app.data.export.CsvExporter
import com.classlog.app.data.export.PdfReportGenerator
import com.classlog.app.util.FileShareUtils
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportSheet(
    uiState: SummaryUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val monthName = uiState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val baseFileName = "ClassLog_${monthName}_${uiState.currentMonth.year}"

    var pendingSaveType by remember { mutableStateOf<String?>(null) } // "pdf" or "csv"

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    if (pendingSaveType == "pdf") {
                        PdfReportGenerator.generateReport(
                            yearMonth = uiState.currentMonth,
                            teacherName = uiState.settings.teacherName,
                            sessions = uiState.sessions,
                            use24Hour = uiState.settings.use24HourFormat,
                            outputStream = os,
                            institutionName = uiState.settings.institutionName
                        )
                    } else if (pendingSaveType == "csv") {
                        CsvExporter.writeToStream(
                            sessions = uiState.sessions,
                            use24Hour = uiState.settings.use24HourFormat,
                            outputStream = os
                        )
                    }
                }
                Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Export Monthly Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$monthName ${uiState.currentMonth.year} • ${uiState.summary.sessionCount} sessions logged",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: PDF Timesheet
            ExportOptionCard(
                title = "PDF Timesheet Report",
                description = "Printable A4 document with header, KPI cards, batch breakdown, daily logs, and signature line.",
                icon = Icons.Default.PictureAsPdf,
                onShare = {
                    FileShareUtils.shareFile(
                        context = context,
                        fileName = "$baseFileName.pdf",
                        mimeType = "application/pdf",
                        chooserTitle = "Share Monthly Timesheet PDF"
                    ) { os ->
                        PdfReportGenerator.generateReport(
                            yearMonth = uiState.currentMonth,
                            teacherName = uiState.settings.teacherName,
                            sessions = uiState.sessions,
                            use24Hour = uiState.settings.use24HourFormat,
                            outputStream = os,
                            institutionName = uiState.settings.institutionName
                        )
                    }
                },
                onSave = {
                    pendingSaveType = "pdf"
                    createDocLauncher.launch("$baseFileName.pdf")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: CSV Spreadsheet
            ExportOptionCard(
                title = "CSV Spreadsheet",
                description = "Standard table format compatible with Microsoft Excel, Google Sheets, and LibreOffice Calc.",
                icon = Icons.Default.TableChart,
                onShare = {
                    FileShareUtils.shareFile(
                        context = context,
                        fileName = "$baseFileName.csv",
                        mimeType = "text/csv",
                        chooserTitle = "Share Session Log CSV"
                    ) { os ->
                        CsvExporter.writeToStream(
                            sessions = uiState.sessions,
                            use24Hour = uiState.settings.use24HourFormat,
                            outputStream = os
                        )
                    }
                },
                onSave = {
                    pendingSaveType = "csv"
                    createDocLauncher.launch("$baseFileName.csv")
                }
            )
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                FilledTonalButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to File")
                }
            }
        }
    }
}
