package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ExportFileFormat
import com.example.domain.OperationsCalculator
import com.example.domain.ReportExporter
import com.example.model.ReportSectionOption
import com.example.ui.theme.ForestEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDatePickerModal(
    initialIsoDate: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirmDate: (String) -> Unit
) {
    val initialMillis = OperationsCalculator.isoDateToUtcMillis(initialIsoDate)
        ?: OperationsCalculator.isoDateToUtcMillis(OperationsCalculator.todayIsoDate())
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onConfirmDate(OperationsCalculator.utcMillisToIsoDate(millis))
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("date_picker_confirm_button")
            ) {
                Text("Select Date")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("date_picker_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 12.dp)
                )
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
fun DownloadedReportBanner(
    fileName: String,
    mimeType: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSendMessenger: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("downloaded_report_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val savedTitle = when {
                    mimeType == ExportFileFormat.CSV.mimeType || fileName.endsWith(".csv", ignoreCase = true) ->
                        "CSV Saved in Downloads"
                    mimeType == ExportFileFormat.XLS.mimeType || fileName.endsWith(".xls", ignoreCase = true) ->
                        "Excel (XLS) Saved in Downloads"
                    else -> "PDF Saved in Downloads"
                }
                Text(
                    text = savedTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    onClick = onOpen,
                    modifier = Modifier.testTag("downloaded_report_open_button")
                ) {
                    Text("Open")
                }
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("downloaded_report_share_button")
                ) {
                    Text("Share")
                }
                if (onSendMessenger != null) {
                    TextButton(
                        onClick = onSendMessenger,
                        modifier = Modifier.testTag("downloaded_report_messenger_button")
                    ) {
                        Text("Messenger")
                    }
                }
            }
        }
    }
}

private const val MAX_BINDER_TEXT_BYTES = 80_000

fun copyReportTextToClipboard(context: Context, label: String, text: String): Boolean {
    val utf8Bytes = text.toByteArray(Charsets.UTF_8)
    if (utf8Bytes.size > MAX_BINDER_TEXT_BYTES) {
        return false
    }
    return try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        true
    } catch (_: RuntimeException) {
        false
    }
}

fun sharePlainTextReport(
    context: Context,
    subject: String,
    content: String,
    fallbackFileName: String? = null
) {
    val utf8Bytes = content.toByteArray(Charsets.UTF_8)
    if (utf8Bytes.size > MAX_BINDER_TEXT_BYTES) {
        val safeName = (fallbackFileName ?: "${subject.replace(Regex("[^A-Za-z0-9._-]"), "_")}.txt")
        val uri = ReportExporter.createShareableReportUri(context, safeName, utf8Bytes)
        shareExportedFileUri(context, uri, safeName, "text/plain")
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    val chooser = Intent.createChooser(intent, "Share $subject").apply {
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {
        // Ignore when no share target or window token unavailable
    }
}

fun openExportedFileUri(context: Context, uri: Uri, mimeType: String, onToast: (String) -> Unit) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onToast("No app found to open this file; tap Share to export.")
    } catch (_: Exception) {
        onToast("Unable to open this file; tap Share to export.")
    }
}

fun shareExportedFileUri(context: Context, uri: Uri, fileName: String, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        clipData = ClipData.newUri(context.contentResolver, fileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Share $fileName").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {
        // Ignore when share sheet cannot be launched
    }
}

fun Set<ReportSectionOption>.resolveIncludedSections(): Set<ReportSectionOption> =
    if (isEmpty()) ReportSectionOption.entries.toSet() else this

fun Set<ReportSectionOption>.describeSelection(): String {
    if (isEmpty() || size == ReportSectionOption.entries.size) return "All Sections"
    if (size == 1) return "${first().label} only"
    return ReportSectionOption.entries.filter { it in this }.joinToString(", ") { it.label }
}

@Composable
fun ReportSectionSelectorRow(
    selectedSections: Set<ReportSectionOption>,
    onUpdateSections: (Set<ReportSectionOption>) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    val isAll = selectedSections.isEmpty() || selectedSections.size == ReportSectionOption.entries.size
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Include in Report: ${selectedSections.describeSelection()}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isAll,
                onClick = { onUpdateSections(emptySet()) },
                leadingIcon = if (isAll) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                label = { Text("All") },
                modifier = Modifier.testTag("${tagPrefix}_all")
            )
            ReportSectionOption.entries.forEach { option ->
                val checked = !isAll && option in selectedSections
                FilterChip(
                    selected = checked,
                    onClick = {
                        val next = when {
                            isAll -> setOf(option)
                            option in selectedSections -> selectedSections - option
                            else -> selectedSections + option
                        }
                        onUpdateSections(
                            if (next.size == ReportSectionOption.entries.size) emptySet() else next
                        )
                    },
                    leadingIcon = if (checked) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text(option.label) },
                    modifier = Modifier.testTag("${tagPrefix}_${option.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun ExportFormatSelectorRow(
    selectedFormat: ExportFileFormat,
    onSelectFormat: (ExportFileFormat) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
    title: String = "Export Format"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$title: ${selectedFormat.label} (.${selectedFormat.extension})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val formats = ExportFileFormat.entries
            formats.forEachIndexed { index, fmt ->
                val icon = when (fmt) {
                    ExportFileFormat.CSV -> Icons.Filled.TableChart
                    ExportFileFormat.PDF -> Icons.Filled.PictureAsPdf
                    ExportFileFormat.XLS -> Icons.Filled.GridOn
                }
                SegmentedButton(
                    selected = selectedFormat == fmt,
                    onClick = { onSelectFormat(fmt) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = formats.size),
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Select ${fmt.label} format",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("${tagPrefix}_${fmt.extension}")
                ) {
                    Text(fmt.label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun DownloadAlsoPdfXlsDialog(
    title: String,
    initialFormat: ExportFileFormat,
    onDismiss: () -> Unit,
    onConfirmFormats: (List<ExportFileFormat>) -> Unit
) {
    var selectedFormats by remember(initialFormat) { mutableStateOf(setOf(initialFormat)) }
    val orderedSelection = ExportFileFormat.entries.filter { it in selectedFormats }

    val promptCopy = when (initialFormat) {
        ExportFileFormat.CSV -> "You selected CSV. Would you also like to download this file as PDF or Excel (XLS)?"
        ExportFileFormat.PDF -> "You selected PDF. Would you also like to include Excel (XLS) or CSV?"
        ExportFileFormat.XLS -> "You selected Excel (XLS). Would you also like to include PDF or CSV?"
    }

    val confirmLabel = when (orderedSelection.size) {
        0 -> "Select a format"
        1 -> "Download ${orderedSelection.first().label}"
        2 -> "Download ${orderedSelection[0].label} & ${orderedSelection[1].label}"
        else -> "Download All (3)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = promptCopy,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExportFileFormat.entries.forEach { fmt ->
                    val checked = fmt in selectedFormats
                    val (fmtTitle, fmtDesc, fmtIcon) = when (fmt) {
                        ExportFileFormat.CSV -> Triple(
                            "CSV (.csv)",
                            "Raw comma-separated data table",
                            Icons.Filled.TableChart
                        )
                        ExportFileFormat.PDF -> Triple(
                            "PDF (.pdf)",
                            "Printable formatted document",
                            Icons.Filled.PictureAsPdf
                        )
                        ExportFileFormat.XLS -> Triple(
                            "Excel (.xls)",
                            "Formatted multi-sheet workbook",
                            Icons.Filled.GridOn
                        )
                    }

                    val toggle: () -> Unit = {
                        selectedFormats = if (checked) selectedFormats - fmt else selectedFormats + fmt
                    }

                    Surface(
                        color = if (checked) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = toggle)
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("download_format_option_${fmt.extension}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { toggle() },
                                modifier = Modifier.testTag("download_format_check_${fmt.extension}")
                            )
                            Icon(
                                imageVector = fmtIcon,
                                contentDescription = null,
                                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fmtTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = fmtDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (orderedSelection.isNotEmpty()) {
                        onConfirmFormats(orderedSelection)
                    }
                },
                enabled = orderedSelection.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .desktopPointerHand()
                    .testTag("confirm_download_formats_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_download_formats_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
