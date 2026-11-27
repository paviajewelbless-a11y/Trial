package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExportFileFormat
import com.example.domain.ReportExporter
import com.example.domain.SnapshotBitmapRenderer
import com.example.model.DailyPlantLog
import com.example.model.PlantReportData
import com.example.ui.theme.MessengerBlue
import com.example.ui.theme.OnStatusHealthyContainer
import com.example.ui.theme.StatusHealthyContainer

private val MESSENGER_PACKAGES = listOf(
    "com.facebook.orca",
    "com.facebook.mlite"
)

enum class MessengerSendMode(val label: String) {
    CHAT_SUMMARY("Chat"),
    SNAPSHOT("Snapshot"),
    FILE_ATTACHMENT("File")
}

fun detectInstalledMessengerPackage(context: Context): String? {
    val pm = context.packageManager
    for (pkg in MESSENGER_PACKAGES) {
        val installed = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        if (installed) return pkg
    }
    return null
}

private const val MAX_MESSENGER_TEXT_BYTES = 60_000

fun sendTextToMessenger(
    context: Context,
    text: String,
    subject: String = "10K Agri Feeds Bulacan Plant Report",
    onToast: (String) -> Unit
): Boolean {
    val utf8Bytes = text.toByteArray(Charsets.UTF_8)
    if (utf8Bytes.size > MAX_MESSENGER_TEXT_BYTES) {
        return try {
            val safeName = "Bulacan_Plant_Report.txt"
            val fileUri = ReportExporter.createShareableReportUri(context, safeName, utf8Bytes)
            sendFileToMessenger(
                context = context,
                fileUri = fileUri,
                fileName = safeName,
                mimeType = "text/plain",
                onToast = onToast
            )
        } catch (e: Exception) {
            onToast("Report too large for inline message; export failed: ${e.localizedMessage}")
            false
        }
    }

    val messengerPkg = detectInstalledMessengerPackage(context)
    val baseIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    if (messengerPkg != null) {
        val directIntent = Intent(baseIntent).apply {
            setPackage(messengerPkg)
        }
        try {
            context.startActivity(directIntent)
            onToast("Opening Messenger to send report...")
            return true
        } catch (_: ActivityNotFoundException) {
            // Fall through to system share sheet below
        } catch (_: SecurityException) {
            // Fall through to system share sheet below
        } catch (_: RuntimeException) {
            // Fall through to system share sheet below
        }
    }

    val copied = copyReportTextToClipboard(context, subject, text)
    val chooser = Intent.createChooser(baseIntent, "Send Report to Messenger").apply {
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return try {
        context.startActivity(chooser)
        onToast(
            if (copied) "Messenger app not found — report copied & Share Sheet opened."
            else "Messenger app not found — Share Sheet opened."
        )
        true
    } catch (_: Exception) {
        onToast(
            if (copied) "Messenger is not installed. Report summary copied to clipboard."
            else "Unable to open Messenger or share sheet."
        )
        false
    }
}

fun sendFileToMessenger(
    context: Context,
    fileUri: Uri,
    fileName: String,
    mimeType: String,
    captionText: String = "",
    onToast: (String) -> Unit
): Boolean {
    val messengerPkg = detectInstalledMessengerPackage(context)
    val isFileProviderUri = fileUri.authority == "${context.packageName}.fileprovider"
    // Facebook Messenger's ACTION_SEND handler prioritizes EXTRA_TEXT over EXTRA_STREAM and drops the image/file
    // when EXTRA_TEXT is present. Send only EXTRA_STREAM + ClipData so Messenger attaches the picture/file.
    val baseIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, fileUri)
        clipData = ClipData.newUri(context.contentResolver, fileName, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    if (messengerPkg != null) {
        val directIntent = Intent(baseIntent).apply {
            setPackage(messengerPkg)
        }
        if (isFileProviderUri) {
            try {
                context.grantUriPermission(
                    messengerPkg,
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // ClipData + FLAG_GRANT_READ_URI_PERMISSION handles grant on newer Android versions
            }
        }
        try {
            context.startActivity(directIntent)
            onToast("Opening Messenger with $fileName...")
            return true
        } catch (_: ActivityNotFoundException) {
            // Fall through to chooser
        } catch (_: SecurityException) {
            // Fall through to chooser
        } catch (_: RuntimeException) {
            // Fall through to chooser
        }
    }

    val chooserIntent = Intent.createChooser(baseIntent, "Send $fileName").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return try {
        context.startActivity(chooserIntent)
        onToast("Messenger app not found — choose an app to send $fileName.")
        true
    } catch (_: Exception) {
        onToast("No app available to share $fileName.")
        false
    }
}

@Composable
fun SendToMessengerDialog(
    title: String = stringResource(R.string.messenger_send_title),
    defaultMessageText: String,
    initialFormat: ExportFileFormat = ExportFileFormat.PDF,
    initialMode: MessengerSendMode = MessengerSendMode.CHAT_SUMMARY,
    preAttachedFile: Triple<Uri, String, String>? = null,
    onGenerateShareableFile: ((ExportFileFormat) -> Triple<Uri, String, String>)? = null,
    snapshotPlantOptions: List<String> = emptyList(),
    onRenderSnapshotPreview: ((targetPlantSection: String?, noteText: String) -> Pair<String, Bitmap>)? = null,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit,
    onSent: ((modeLabel: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val messengerPkg = remember(context) { detectInstalledMessengerPackage(context) }
    val isMessengerInstalled = messengerPkg != null

    val canAttachFile = preAttachedFile != null || onGenerateShareableFile != null
    val canSendSnapshot = onRenderSnapshotPreview != null && preAttachedFile == null
    val availableModes = remember(canAttachFile, canSendSnapshot, preAttachedFile) {
        if (preAttachedFile != null) {
            listOf(MessengerSendMode.FILE_ATTACHMENT)
        } else {
            buildList {
                add(MessengerSendMode.CHAT_SUMMARY)
                if (canSendSnapshot) add(MessengerSendMode.SNAPSHOT)
                if (canAttachFile) add(MessengerSendMode.FILE_ATTACHMENT)
            }
        }
    }

    var sendMode by remember(initialMode, preAttachedFile) {
        mutableStateOf(
            if (preAttachedFile != null) {
                MessengerSendMode.FILE_ATTACHMENT
            } else if (initialMode in availableModes) {
                initialMode
            } else {
                MessengerSendMode.CHAT_SUMMARY
            }
        )
    }
    var selectedFormat by remember(initialFormat) { mutableStateOf(initialFormat) }
    var selectedSnapshotPlant by remember(snapshotPlantOptions) { mutableStateOf<String?>(null) }
    var headerNote by remember { mutableStateOf("") }
    var editableMessage by remember(defaultMessageText) { mutableStateOf(defaultMessageText) }

    val composedChatMessage = remember(headerNote, editableMessage) {
        val note = headerNote.trim()
        if (note.isBlank()) editableMessage else "$note\n\n$editableMessage"
    }

    val currentSnapshotRenderer by rememberUpdatedState(onRenderSnapshotPreview)
    val snapshotPreview = remember(sendMode, selectedSnapshotPlant, headerNote, defaultMessageText, canSendSnapshot) {
        val renderer = currentSnapshotRenderer
        if (sendMode == MessengerSendMode.SNAPSHOT && renderer != null) {
            renderer(selectedSnapshotPlant, headerNote)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = MessengerBlue
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (isMessengerInstalled) StatusHealthyContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isMessengerInstalled) OnStatusHealthyContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("messenger_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isMessengerInstalled) Icons.Filled.CheckCircle else Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (isMessengerInstalled) OnStatusHealthyContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isMessengerInstalled) {
                                stringResource(R.string.messenger_ready_badge)
                            } else {
                                stringResource(R.string.messenger_fallback_badge)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isMessengerInstalled) OnStatusHealthyContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (availableModes.size > 1) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        availableModes.forEachIndexed { idx, mode ->
                            val icon = when (mode) {
                                MessengerSendMode.CHAT_SUMMARY -> Icons.Filled.Forum
                                MessengerSendMode.SNAPSHOT -> Icons.Filled.Image
                                MessengerSendMode.FILE_ATTACHMENT -> Icons.Filled.AttachFile
                            }
                            SegmentedButton(
                                selected = sendMode == mode,
                                onClick = { sendMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = availableModes.size
                                ),
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("messenger_mode_${mode.name.lowercase()}")
                            ) {
                                Text(mode.label, maxLines = 1)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = headerNote,
                    onValueChange = { headerNote = it },
                    label = {
                        Text(
                            when (sendMode) {
                                MessengerSendMode.CHAT_SUMMARY -> "Optional Shift / Recipient Note"
                                MessengerSendMode.SNAPSHOT -> "Optional Note on Snapshot"
                                MessengerSendMode.FILE_ATTACHMENT -> "Optional Messenger Caption"
                            }
                        )
                    },
                    placeholder = { Text("e.g., Morning shift / For Bulacan Plant team") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("messenger_note_input")
                )

                when (sendMode) {
                    MessengerSendMode.CHAT_SUMMARY -> {
                        OutlinedTextField(
                            value = editableMessage,
                            onValueChange = { editableMessage = it },
                            label = { Text("Messenger Report Message") },
                            minLines = 7,
                            maxLines = 12,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("messenger_message_preview_input")
                        )
                        Text(
                            text = "Full itemized report (${editableMessage.lines().size} lines) — scroll to review or edit before sending.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MessengerSendMode.SNAPSHOT -> {
                        if (snapshotPlantOptions.size > 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Snapshot Scope",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val allSelected = selectedSnapshotPlant == null
                                    FilterChip(
                                        selected = allSelected,
                                        onClick = { selectedSnapshotPlant = null },
                                        leadingIcon = if (allSelected) {
                                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        label = { Text("All Plants") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("snapshot_plant_chip_all")
                                    )
                                    snapshotPlantOptions.forEach { plant ->
                                        val selected = selectedSnapshotPlant.equals(plant, ignoreCase = true)
                                        FilterChip(
                                            selected = selected,
                                            onClick = { selectedSnapshotPlant = plant },
                                            leadingIcon = if (selected) {
                                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            label = { Text(plant) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("snapshot_plant_chip_${plant.lowercase().replace(" ", "_")}")
                                        )
                                    }
                                }
                            }
                        }

                        if (snapshotPreview != null) {
                            val (snapName, snapBitmap) = snapshotPreview
                            val imageBitmap = remember(snapBitmap) { snapBitmap.asImageBitmap() }
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("messenger_snapshot_preview")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(6.dp)
                                ) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Snapshot preview of $snapName",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Text(
                                text = "High-res snapshot (${snapBitmap.width}×${snapBitmap.height}) — sends as a photo in Messenger.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    MessengerSendMode.FILE_ATTACHMENT -> {
                        if (preAttachedFile != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Ready to Attach",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = preAttachedFile.second,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            ExportFormatSelectorRow(
                                selectedFormat = selectedFormat,
                                onSelectFormat = { selectedFormat = it },
                                tagPrefix = "messenger_attach_format",
                                title = "Attachment Format"
                            )
                            Text(
                                text = "Generates a fresh ${selectedFormat.label} file and attaches it straight into Messenger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sendMode == MessengerSendMode.SNAPSHOT && snapshotPreview != null) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val (snapName, snapBitmap) = snapshotPreview
                                val jpegBytes = SnapshotBitmapRenderer.compressBitmapToJpegBytes(snapBitmap)
                                val saved = ReportExporter.saveSnapshotImageToGallery(
                                    context = context,
                                    fileName = snapName,
                                    jpegBytes = jpegBytes,
                                    width = snapBitmap.width,
                                    height = snapBitmap.height
                                )
                                if (saved.uri != null) {
                                    onShowToast("Snapshot saved to Pictures/FeedMill Ops: $snapName")
                                } else {
                                    val shareUri = ReportExporter.createShareableReportUri(context, snapName, jpegBytes)
                                    shareExportedFileUri(context, shareUri, snapName, "image/jpeg")
                                }
                            } catch (e: Exception) {
                                onShowToast("Failed to save snapshot: ${e.localizedMessage}")
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("messenger_save_snapshot_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Image")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            copyReportTextToClipboard(context, "Bulacan Plant Messenger Report", composedChatMessage)
                            onShowToast(context.getString(R.string.messenger_copied_notice))
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .desktopPointerHand()
                            .testTag("messenger_copy_text_button")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                }

                Button(
                    onClick = {
                        when (sendMode) {
                            MessengerSendMode.CHAT_SUMMARY -> {
                                val ok = sendTextToMessenger(
                                    context = context,
                                    text = composedChatMessage,
                                    onToast = onShowToast
                                )
                                if (ok) {
                                    onSent?.invoke("Chat Summary")
                                    onDismiss()
                                }
                            }

                            MessengerSendMode.SNAPSHOT -> {
                                try {
                                    val (snapName, snapBitmap) = checkNotNull(snapshotPreview)
                                    val jpegBytes = SnapshotBitmapRenderer.compressBitmapToJpegBytes(snapBitmap)
                                    val uri = ReportExporter.createShareableSnapshotImageUri(
                                        context = context,
                                        fileName = snapName,
                                        jpegBytes = jpegBytes,
                                        width = snapBitmap.width,
                                        height = snapBitmap.height
                                    )
                                    val plantTag = selectedSnapshotPlant ?: "All Plants"
                                    val ok = sendFileToMessenger(
                                        context = context,
                                        fileUri = uri,
                                        fileName = snapName,
                                        mimeType = "image/jpeg",
                                        captionText = "",
                                        onToast = onShowToast
                                    )
                                    if (ok) {
                                        onSent?.invoke("Snapshot ($plantTag)")
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    onShowToast("Failed to prepare snapshot: ${e.localizedMessage}")
                                }
                            }

                            MessengerSendMode.FILE_ATTACHMENT -> {
                                try {
                                    val (uri, fileName, mimeType) = preAttachedFile
                                        ?: checkNotNull(onGenerateShareableFile).invoke(selectedFormat)
                                    val caption = headerNote.trim().ifBlank {
                                        "10K Agri Feeds - Bulacan Plant Report ($fileName)"
                                    }
                                    val ok = sendFileToMessenger(
                                        context = context,
                                        fileUri = uri,
                                        fileName = fileName,
                                        mimeType = mimeType,
                                        captionText = caption,
                                        onToast = onShowToast
                                    )
                                    if (ok) {
                                        onSent?.invoke("Attachment ($fileName)")
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    onShowToast("Failed to prepare Messenger file: ${e.localizedMessage}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MessengerBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .desktopPointerHand()
                        .testTag("messenger_confirm_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.messenger_send_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("messenger_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SendPlantReportToMessengerDialog(
    report: PlantReportData,
    initialFormat: ExportFileFormat,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit,
    onSent: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val summaryText = remember(report) {
        ReportExporter.buildMessengerSummaryText(report)
    }
    val plantOptions = remember(report) {
        SnapshotBitmapRenderer.availablePlantSections(report)
    }
    SendToMessengerDialog(
        defaultMessageText = summaryText,
        initialFormat = initialFormat,
        initialMode = MessengerSendMode.CHAT_SUMMARY,
        onGenerateShareableFile = { fmt ->
            val (fileName, bytes) = ReportExporter.generateReportBytes(report, fmt)
            val uri = ReportExporter.createShareableReportUri(context, fileName, bytes)
            Triple(uri, fileName, fmt.mimeType)
        },
        snapshotPlantOptions = plantOptions,
        onRenderSnapshotPreview = { targetPlant, note ->
            val payload = SnapshotBitmapRenderer.buildPlantSnapshotPayload(report, targetPlant)
            val bitmap = SnapshotBitmapRenderer.renderSnapshotBitmap(payload, note)
            payload.fileName to bitmap
        },
        onDismiss = onDismiss,
        onShowToast = onShowToast,
        onSent = onSent
    )
}

@Composable
fun SendDailyLogToMessengerDialog(
    dayLog: DailyPlantLog,
    sectionFilter: String,
    initialFormat: ExportFileFormat,
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit,
    onSent: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val summaryText = remember(dayLog, sectionFilter) {
        ReportExporter.buildDailyLogMessengerText(dayLog, sectionFilter)
    }
    val plantOptions = remember(dayLog, sectionFilter) {
        SnapshotBitmapRenderer.availableDailyLogSections(dayLog, sectionFilter)
    }
    SendToMessengerDialog(
        title = "Send Daily Log to Messenger",
        defaultMessageText = summaryText,
        initialFormat = initialFormat,
        initialMode = MessengerSendMode.CHAT_SUMMARY,
        onGenerateShareableFile = { fmt ->
            val (fileName, bytes) = ReportExporter.generateDailyLogBytes(dayLog, sectionFilter, fmt)
            val uri = ReportExporter.createShareableReportUri(context, fileName, bytes)
            Triple(uri, fileName, fmt.mimeType)
        },
        snapshotPlantOptions = plantOptions,
        onRenderSnapshotPreview = { targetPlant, note ->
            val payload = SnapshotBitmapRenderer.buildDailyLogSnapshotPayload(dayLog, sectionFilter, targetPlant)
            val bitmap = SnapshotBitmapRenderer.renderSnapshotBitmap(payload, note)
            payload.fileName to bitmap
        },
        onDismiss = onDismiss,
        onShowToast = onShowToast,
        onSent = onSent
    )
}
