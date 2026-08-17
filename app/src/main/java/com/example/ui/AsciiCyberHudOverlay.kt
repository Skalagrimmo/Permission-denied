package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.model.HudNotification
import com.example.model.NotificationLevel
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Minimalist, high-contrast animated Cyberpunk HUD.
 * Replaces heavy multi-line text logs with sleek animated stylish icons,
 * circular & pill micro-meters, and interactive cyberdeck elements to maximize
 * screen real estate for immersive 3D world exploration.
 */
@Composable
fun AsciiCyberHudOverlay(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    val isCriticalHp = engine.playerHealth in 1..25
    val infiniteTransition = rememberInfiniteTransition(label = "hud_glow")

    // Pulsing alpha for critical states
    val alertPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_pulse"
    )

    // Slow rotation for animated radar/cyberdeck icons
    val hackSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hack_spin"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. SLEEK TOP METRIC STRIP (Edge-Hugging, Compact, Animated)
        SleekAnimatedMetricsBar(
            engine = engine,
            isCriticalHp = isCriticalHp,
            alertPulseAlpha = alertPulseAlpha,
            hackSpinAngle = hackSpinAngle
        )

        // 2. DISCREET ANIMATED FLOATING TOAST (Single-line, minimal footprint, auto-fading)
        AnimatedNotificationPill(
            notification = engine.hudNotifications.firstOrNull(),
            onDismiss = { notif -> engine.hudNotifications.remove(notif) }
        )
    }
}

@Composable
fun SleekAnimatedMetricsBar(
    engine: GameEngine,
    isCriticalHp: Boolean,
    alertPulseAlpha: Float,
    hackSpinAngle: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xEE04080D),
                        Color(0xCC060C14)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isCriticalHp) {
                        listOf(Color(0xFFFF1744).copy(alpha = alertPulseAlpha), Color(0xFFFF5252))
                    } else {
                        listOf(Color(0xFF00F0FF), Color(0xFF00FF66).copy(alpha = 0.6f))
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Animated Vital Badges (HP, ARMOR, ENERGY)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health Pill Gauge
            AnimatedVitalPill(
                icon = Icons.Default.Favorite,
                value = engine.playerHealth,
                maxValue = 100,
                activeColor = if (isCriticalHp) Color(0xFFFF1744) else Color(0xFFFF3366),
                isPulsing = isCriticalHp,
                pulseAlpha = alertPulseAlpha,
                tag = "HP",
                testTag = "hud_hp_pill"
            )

            // Armor/Shield Pill Gauge
            AnimatedVitalPill(
                icon = Icons.Default.Shield,
                value = engine.playerArmor,
                maxValue = 100,
                activeColor = Color(0xFF00E5FF),
                isPulsing = false,
                pulseAlpha = 1f,
                tag = "SHD",
                testTag = "hud_armor_pill"
            )

            // Cyber Energy Pill Gauge
            AnimatedVitalPill(
                icon = Icons.Default.Bolt,
                value = engine.playerEnergy.roundToInt(),
                maxValue = 100,
                activeColor = Color(0xFF00FF66),
                isPulsing = engine.playerEnergy < 20f,
                pulseAlpha = alertPulseAlpha,
                tag = "NRG",
                testTag = "hud_nrg_pill"
            )
        }

        // Right: Interactive Quick-Hack Cyberdeck Button & Currency
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick-Hack Interactive Cyberdeck Button
            InteractiveQuickHackButton(
                engine = engine,
                hackSpinAngle = hackSpinAngle,
                alertPulseAlpha = alertPulseAlpha
            )

            // Credits Micro-Badge
            Box(
                modifier = Modifier
                    .background(Color(0x66FFB800), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFFFB800), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "¥${engine.playerCredits}",
                    color = Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Animated micro-vital pill with an icon and smooth filled progress indicator.
 */
@Composable
fun AnimatedVitalPill(
    icon: ImageVector,
    value: Int,
    maxValue: Int,
    activeColor: Color,
    isPulsing: Boolean,
    pulseAlpha: Float,
    tag: String,
    testTag: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (maxValue > 0) (value.toFloat() / maxValue).coerceIn(0f, 1f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "vital_progress"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isPulsing) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "vital_scale"
    )

    Row(
        modifier = Modifier
            .background(Color(0xDD0B121A), RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isPulsing) activeColor.copy(alpha = pulseAlpha) else Color(0x33FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 5.dp, vertical = 3.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tag,
            tint = if (isPulsing) activeColor.copy(alpha = pulseAlpha) else activeColor,
            modifier = Modifier
                .size(13.dp)
                .scale(if (isPulsing) iconScale else 1.0f)
        )

        Spacer(modifier = Modifier.width(3.dp))

        // Sleek Linear Mini-Bar
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                activeColor.copy(alpha = 0.8f),
                                activeColor
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "$value",
            color = if (isPulsing) activeColor else Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

/**
 * Animated Interactive Quick-Hack Trigger Button.
 * Displays glowing circular HUD ring when ready and countdown progress sweep when recharging.
 */
@Composable
fun InteractiveQuickHackButton(
    engine: GameEngine,
    hackSpinAngle: Float,
    alertPulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val isHackReady = engine.remoteHackCooldown <= 0f
    val cooldownProgress = if (engine.remoteHackMaxCooldown > 0f) {
        1.0f - (engine.remoteHackCooldown / engine.remoteHackMaxCooldown).coerceIn(0f, 1f)
    } else 1.0f

    val buttonScale by animateFloatAsState(
        targetValue = if (isHackReady) 1.0f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "hack_scale"
    )

    Box(
        modifier = modifier
            .scale(buttonScale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isHackReady) Color(0xDD002233) else Color(0xCC0B121A)
            )
            .border(
                width = 1.2.dp,
                color = if (isHackReady) Color(0xFF00F0FF) else Color(0xFF37474F),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                enabled = isHackReady,
                onClick = { engine.performRemoteQuickHack() }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("ascii_quick_hack_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Animated Rotating Terminal / ICE Breaker Icon
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isHackReady) {
                    CircularProgressIndicator(
                        progress = { cooldownProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF00F0FF),
                        trackColor = Color(0x3300F0FF),
                        strokeWidth = 1.5.dp,
                        strokeCap = StrokeCap.Round
                    )
                }

                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Quick Hack ICE",
                    tint = if (isHackReady) Color(0xFF00F0FF) else Color.Gray,
                    modifier = Modifier
                        .size(11.dp)
                        .rotate(if (isHackReady) 0f else hackSpinAngle)
                )
            }

            Text(
                text = if (isHackReady) "HACK" else "${String.format(java.util.Locale.US, "%.1f", engine.remoteHackCooldown)}s",
                color = if (isHackReady) Color(0xFF00FF66) else Color(0xFF90A4AE),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp
            )
        }
    }
}

/**
 * Minimalist, animated floating status chip.
 * Automatically slides in, shows a high-contrast stylish badge with icon,
 * and disappears quickly to keep screen exploration 100% unobstructed.
 */
@Composable
fun AnimatedNotificationPill(
    notification: HudNotification?,
    onDismiss: (HudNotification) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(
            targetOffsetY = { -it / 2 }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        if (notification != null) {
            val (icon, badgeColor) = when (notification.level) {
                NotificationLevel.INFO -> Pair(Icons.Default.Info, Color(0xFF00F0FF))
                NotificationLevel.SUCCESS -> Pair(Icons.Default.CheckCircle, Color(0xFF00FF66))
                NotificationLevel.WARNING -> Pair(Icons.Default.Warning, Color(0xFFFFB800))
                NotificationLevel.CRITICAL -> Pair(Icons.Default.Dangerous, Color(0xFFFF1744))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF0050A10))
                        .border(1.dp, badgeColor, RoundedCornerShape(16.dp))
                        .clickable { onDismiss(notification) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("ascii_notification_item"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = notification.tag,
                        tint = badgeColor,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "[${notification.tag}]",
                        color = badgeColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = notification.message,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Utility to convert numeric percentages into ASCII text block bars.
 */
fun renderAsciiBar(value: Int, max: Int, segments: Int = 10): String {
    val clamped = value.coerceIn(0, max)
    val ratio = if (max > 0) clamped.toFloat() / max else 0f
    val filled = (ratio * segments).roundToInt().coerceIn(0, segments)
    val empty = max(0, segments - filled)

    val fillChars = "█".repeat(filled)
    val emptyChars = "░".repeat(empty)
    return "[$fillChars$emptyChars]"
}
