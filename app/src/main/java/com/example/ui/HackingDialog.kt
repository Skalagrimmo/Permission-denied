package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.HackNodeType
import com.example.model.HackingSession

@Composable
fun HackingDialog(
    session: HackingSession,
    onNodeClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Terminal Breach // ${session.sectorName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "ICE Level ${session.iceLevel} • Zero-Day Packet Injection",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant)
                            .testTag("close_hack_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Trace Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SECURITY TRACE",
                            color = if (session.traceProgress > 70f) Color(0xFFB3261E) else colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "${session.traceProgress.toInt()}%",
                            color = if (session.traceProgress > 70f) Color(0xFFB3261E) else colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (session.traceProgress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = if (session.traceProgress > 70f) Color(0xFFB3261E) else colorScheme.primary,
                        trackColor = colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Network Graph Canvas with dynamic constraints
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    val availableWidth = maxWidth
                    val availableHeight = maxHeight

                    // Draw Connection Lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        session.nodes.forEach { node ->
                            val startOffset = Offset(node.x * size.width, node.y * size.height)
                            node.connectedNodeIds.forEach { targetId ->
                                val target = session.nodes.find { it.id == targetId }
                                if (target != null && target.id > node.id) {
                                    val endOffset = Offset(target.x * size.width, target.y * size.height)
                                    val isLineActive = node.isCaptured && target.isCaptured
                                    drawLine(
                                        color = if (isLineActive) Color(0xFF48A252) else Color(0xFFCAC4D0),
                                        start = startOffset,
                                        end = endOffset,
                                        strokeWidth = if (isLineActive) 4f else 2f
                                    )
                                }
                            }
                        }
                    }

                    // Render Nodes
                    session.nodes.forEach { node ->
                        val nodeColor = when {
                            node.isCaptured -> Color(0xFF48A252)
                            node.type == HackNodeType.CORE_MAINFRAME -> Color(0xFF6750A4)
                            node.type == HackNodeType.ICE_FIREWALL -> Color(0xFFB3261E)
                            else -> Color(0xFF625B71)
                        }

                        val nodeOffsetX = (node.x * (availableWidth.value - 50)).dp
                        val nodeOffsetY = (node.y * (availableHeight.value - 40)).dp

                        Box(
                            modifier = Modifier
                                .offset(x = nodeOffsetX, y = nodeOffsetY)
                                .size(48.dp, 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorScheme.surface)
                                .border(1.2.dp, nodeColor, RoundedCornerShape(10.dp))
                                .clickable { onNodeClick(node.id) }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = node.type.label,
                                    color = nodeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 7.sp
                                )
                                if (!node.isCaptured && node.currentHp > 0) {
                                    Text(
                                        text = "${node.currentHp}HP",
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 7.sp
                                    )
                                } else if (node.isCaptured) {
                                    Text(
                                        text = "OWNED",
                                        color = Color(0xFF48A252),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 7.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom instructions
                Text(
                    text = "Tap adjacent nodes to inject exploit packets before trace completes. Breach ROOT CORE to override sector.",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp
                )
            }
        }
    }
}
