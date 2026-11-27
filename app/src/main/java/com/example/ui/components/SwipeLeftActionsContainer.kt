package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GrainAmber
import com.example.ui.theme.SlateTeal
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalTextDark
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeLeftActionsContainer(
    itemKey: Any,
    shape: Shape = RoundedCornerShape(14.dp),
    revealWidth: Dp = 144.dp,
    editLabel: String = "Edit",
    deleteLabel: String = "Delete",
    editTestTag: String? = null,
    deleteTestTag: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    swipeRightEnabled: Boolean = false,
    rightRevealWidth: Dp = 156.dp,
    rightFirstLabel: String = "Schedule",
    rightFirstIcon: ImageVector = Icons.Filled.AddTask,
    rightFirstColor: Color = GrainAmber,
    rightFirstTestTag: String? = null,
    onRightFirst: (() -> Unit)? = null,
    rightSecondLabel: String = "Dispatch",
    rightSecondIcon: ImageVector = Icons.Filled.LocalShipping,
    rightSecondColor: Color = ForestEmerald,
    rightSecondTestTag: String? = null,
    onRightSecond: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealLeftPx = with(density) { revealWidth.toPx() }
    val hasSwipeRight = swipeRightEnabled && (onRightFirst != null || onRightSecond != null)
    val revealRightPx = with(density) { rightRevealWidth.toPx() }
    val offsetX = remember(itemKey) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val isDesktopLayout = LocalIsDesktopLayout.current
    val hoverInteractionSource = remember(itemKey) { MutableInteractionSource() }
    val isHovered by hoverInteractionSource.collectIsHoveredAsState()
    val showDesktopActionBar = isDesktopLayout || isHovered

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(hoverInteractionSource),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(shape)
        ) {
            if (hasSwipeRight && offsetX.value > 1f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(rightRevealWidth)
                        .fillMaxHeight()
                ) {
                    if (onRightFirst != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(rightFirstColor)
                                .desktopPointerHand()
                                .then(if (rightFirstTestTag != null) Modifier.testTag(rightFirstTestTag) else Modifier)
                                .clickable {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                    onRightFirst()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = rightFirstIcon,
                                    contentDescription = rightFirstLabel,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = rightFirstLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (onRightSecond != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(rightSecondColor)
                                .desktopPointerHand()
                                .then(if (rightSecondTestTag != null) Modifier.testTag(rightSecondTestTag) else Modifier)
                                .clickable {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                    onRightSecond()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = rightSecondIcon,
                                    contentDescription = rightSecondLabel,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = rightSecondLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (offsetX.value < -1f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(revealWidth)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(SlateTeal)
                            .desktopPointerHand()
                            .then(if (editTestTag != null) Modifier.testTag(editTestTag) else Modifier)
                            .clickable {
                                scope.launch {
                                    offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                                onEdit()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = editLabel,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = editLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(StatusCritical)
                            .desktopPointerHand()
                            .then(if (deleteTestTag != null) Modifier.testTag(deleteTestTag) else Modifier)
                            .clickable {
                                scope.launch {
                                    offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = deleteLabel,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = deleteLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(itemKey, revealLeftPx, hasSwipeRight, revealRightPx) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val maxOffset = if (hasSwipeRight) revealRightPx else 0f
                                val target = (offsetX.value + dragAmount).coerceIn(-revealLeftPx, maxOffset)
                                scope.launch { offsetX.snapTo(target) }
                            },
                            onDragEnd = {
                                val targetValue = when {
                                    offsetX.value < -revealLeftPx * 0.35f -> -revealLeftPx
                                    hasSwipeRight && offsetX.value > revealRightPx * 0.35f -> revealRightPx
                                    else -> 0f
                                }
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        )
                    }
            ) {
                content()
            }
        }

        AnimatedVisibility(visible = showDesktopActionBar) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasSwipeRight) {
                        if (onRightFirst != null) {
                            OutlinedButton(
                                onClick = onRightFirst,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .desktopPointerHand()
                                    .then(if (rightFirstTestTag != null) Modifier.testTag("${rightFirstTestTag}_desktop") else Modifier)
                            ) {
                                Icon(rightFirstIcon, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(rightFirstLabel, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (onRightSecond != null) {
                            OutlinedButton(
                                onClick = onRightSecond,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .desktopPointerHand()
                                    .then(if (rightSecondTestTag != null) Modifier.testTag("${rightSecondTestTag}_desktop") else Modifier)
                            ) {
                                Icon(rightSecondIcon, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(rightSecondLabel, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .desktopPointerHand()
                            .then(if (editTestTag != null) Modifier.testTag("${editTestTag}_desktop") else Modifier)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(editLabel, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalTextDark),
                        border = BorderStroke(1.dp, StatusCriticalTextDark.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .desktopPointerHand()
                            .then(if (deleteTestTag != null) Modifier.testTag("${deleteTestTag}_desktop") else Modifier)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(deleteLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
