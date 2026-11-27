package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ForestEmerald

val LocalIsDesktopLayout = compositionLocalOf { false }
val LocalWindowWidthDp = compositionLocalOf { 400 }

fun desktopGridColumns(windowWidthDp: Int, maxCols: Int = 2): Int = when {
    windowWidthDp >= 1320 && maxCols >= 3 -> 3
    windowWidthDp >= 860 -> 2
    else -> 1
}

fun Modifier.desktopPointerHand(): Modifier = pointerHoverIcon(PointerIcon.Hand)

fun <T> LazyListScope.desktopGridItems(
    itemsList: List<T>,
    columns: Int,
    keyPrefix: String = "row",
    itemKey: (T) -> Any,
    horizontalSpacing: Dp = 12.dp,
    itemContent: @Composable (T) -> Unit
) {
    if (columns <= 1) {
        items(itemsList, key = { "${keyPrefix}_${itemKey(it)}" }) { item ->
            itemContent(item)
        }
    } else {
        val rows = itemsList.chunked(columns)
        items(rows, key = { row -> "${keyPrefix}_${row.joinToString("|") { itemKey(it).toString() }}" }) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        itemContent(item)
                    }
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DesktopShortcutsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Keyboard,
                contentDescription = "PC Keyboard Shortcuts",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "PC / Desktop Shortcuts & Controls",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Computer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Desktop mode adapts automatically on wide windows (Chromebooks, ChromeOS Flex, Windows Android Desktop, tablets & external monitors) with multi-column cards and 1-click mouse action bars.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ShortcutRow("Alt + 1 .. 0", "Switch between Summary, Daily Log, Monitor, Orders, Schedule, Dispatched, Recovery, Reports, AI Ops & History")
                HorizontalDivider()
                ShortcutRow("Ctrl + Z", "Undo last plant action")
                ShortcutRow("Ctrl + Y / Ctrl + Shift + Z", "Redo last plant action")
                ShortcutRow("Enter (AI Ops Chat)", "Send prompt (Shift + Enter for new line)")
                ShortcutRow("F1 or Ctrl + /", "Open this Keyboard Shortcuts guide")
                HorizontalDivider()
                Text(
                    text = "Mouse Tip: Orders, Schedule & Dispatch cards show direct 1-click Schedule, Dispatch, Edit, and Delete buttons in Desktop mode or on mouse hover—no swipe gesture needed.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .desktopPointerHand()
                    .testTag("close_desktop_shortcuts_button")
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun ShortcutRow(keys: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = keys,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
