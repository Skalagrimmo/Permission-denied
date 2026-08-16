package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.model.AugmentationType
import com.example.model.ItemType
import com.example.model.WeaponType
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun GameHudOverlay(
    engine: GameEngine,
    sensitivity: Float,
    onPauseClick: () -> Unit,
    onInventoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stick state
    var stickOffset by remember { mutableStateOf(Offset.Zero) }
    val maxStickRadius = 60f

    // Running game loop driver
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                val deltaSec = ((frameTime - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = frameTime

                // Calculate movement from joystick
                val moveX = (stickOffset.x / maxStickRadius).coerceIn(-1f, 1f)
                val moveZ = -(stickOffset.y / maxStickRadius).coerceIn(-1f, 1f)

                engine.update(moveX, moveZ, 0f, 0f, deltaSec)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Right Look Touch Area (Screen Swipe for Camera Aiming)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
                .align(Alignment.CenterEnd)
                .pointerInput(sensitivity) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val yawDelta = dragAmount.x * 0.22f * sensitivity
                        val pitchDelta = -dragAmount.y * 0.22f * sensitivity
                        engine.update(0f, 0f, yawDelta, pitchDelta, 0f)
                    }
                }
        )

        // 2. Top HUD Status Bar
        TopStatusBar(
            engine = engine,
            onPauseClick = onPauseClick,
            onInventoryClick = onInventoryClick,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // 3. Center Interaction Prompt Button
        engine.nearbyPrompt?.let { prompt ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD0A1118)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 60.dp)
                    .border(1.5.dp, Color(0xFF00F0FF), RoundedCornerShape(8.dp))
                    .clickable { engine.nearbyInteractableAction?.invoke() }
                    .testTag("interact_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "Interact",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = prompt,
                        color = Color(0xFF00F0FF),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 4. Left Virtual Movement Stick
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = 24.dp)
                .background(Color(0x3300F0FF), CircleShape)
                .border(1.5.dp, Color(0x6600F0FF), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { stickOffset = Offset.Zero },
                        onDragCancel = { stickOffset = Offset.Zero }
                    ) { change, dragAmount ->
                        change.consume()
                        val newOffset = stickOffset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        stickOffset = if (dist > maxStickRadius) {
                            Offset(newOffset.x / dist * maxStickRadius, newOffset.y / dist * maxStickRadius)
                        } else newOffset
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .offset { IntOffset(stickOffset.x.roundToInt(), stickOffset.y.roundToInt()) }
                    .align(Alignment.Center)
                    .background(Color(0xCC00F0FF), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        // 5. Augmentations & Quick Consumables Bar (Left Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (engine.activeAugmentations.contains(AugmentationType.ACTIVE_CAMO)) {
                HudIconButton(
                    icon = Icons.Default.VisibilityOff,
                    label = if (engine.isCloaked) "CLOAK ON" else "CLOAK",
                    isActive = engine.isCloaked,
                    activeColor = Color(0xFF00FF66),
                    onClick = { engine.toggleCamo() },
                    testTag = "aug_cloak"
                )
            }
            if (engine.activeAugmentations.contains(AugmentationType.CYBER_OPTICS)) {
                HudIconButton(
                    icon = Icons.Default.RemoveRedEye,
                    label = if (engine.isThermalVision) "THERMAL" else "OPTICS",
                    isActive = engine.isThermalVision,
                    activeColor = Color(0xFF00F0FF),
                    onClick = { engine.toggleThermal() },
                    testTag = "aug_thermal"
                )
            }
            if (engine.activeAugmentations.contains(AugmentationType.DASH_THRUSTERS)) {
                HudIconButton(
                    icon = Icons.Default.FastForward,
                    label = "DASH",
                    isActive = engine.dashCooldownTimer > 0f,
                    activeColor = Color(0xFFFFB800),
                    onClick = { engine.activateDash() },
                    testTag = "aug_dash"
                )
            }
            if (engine.activeAugmentations.contains(AugmentationType.NEURAL_OVERCLOCK)) {
                HudIconButton(
                    icon = Icons.Default.Speed,
                    label = if (engine.isOverclocked) "SLOW-MO" else "OVERCLOCK",
                    isActive = engine.isOverclocked,
                    activeColor = Color(0xFFFF0077),
                    onClick = { engine.activateOverclock() },
                    testTag = "aug_overclock"
                )
            }
        }

        // 6. Right Combat & Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Weapon Info & Swap
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xAA080E16), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
                    .clickable {
                        engine.currentWeaponIndex = (engine.currentWeaponIndex + 1) % engine.weapons.size
                        engine.audioElement.playUiClick()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("weapon_swap_button")
            ) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = "Swap Weapon",
                    tint = Color(0xFF00F0FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${engine.currentWeapon.type.displayName}  [${engine.currentWeapon.ammoInMag}/${engine.currentWeapon.reserveAmmo}]",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Quick Consumables Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConsumableButton(
                    label = "STIM",
                    count = engine.inventory.find { it.type == ItemType.HEALTH_STIM }?.count ?: 0,
                    icon = Icons.Default.LocalHospital,
                    color = Color(0xFFFF1744),
                    onClick = { engine.useItem(ItemType.HEALTH_STIM) },
                    testTag = "quick_stim"
                )
                ConsumableButton(
                    label = "ARMOR",
                    count = engine.inventory.find { it.type == ItemType.ARMOR_PLATE }?.count ?: 0,
                    icon = Icons.Default.Shield,
                    color = Color(0xFF00E5FF),
                    onClick = { engine.useItem(ItemType.ARMOR_PLATE) },
                    testTag = "quick_armor"
                )
                ConsumableButton(
                    label = "BATTERY",
                    count = engine.inventory.find { it.type == ItemType.ENERGY_BATTERY }?.count ?: 0,
                    icon = Icons.Default.Bolt,
                    color = Color(0xFFFFB800),
                    onClick = { engine.useItem(ItemType.ENERGY_BATTERY) },
                    testTag = "quick_battery"
                )
            }

            // Action Buttons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crouch / Sneak Toggle
                SmallActionButton(
                    icon = Icons.Default.Accessibility,
                    label = if (engine.isCrouching) "SNEAK" else "STAND",
                    isActive = engine.isCrouching,
                    onClick = { engine.isCrouching = !engine.isCrouching },
                    testTag = "crouch_button"
                )

                // Sprint Toggle
                SmallActionButton(
                    icon = Icons.Default.DirectionsRun,
                    label = if (engine.isSprinting) "SPRINT" else "WALK",
                    isActive = engine.isSprinting,
                    onClick = { engine.isSprinting = !engine.isSprinting },
                    testTag = "sprint_button"
                )

                // Stealth Takedown / Stun
                SmallActionButton(
                    icon = Icons.Default.PanTool,
                    label = "TAKEDOWN",
                    isActive = false,
                    onClick = { engine.performTakedown() },
                    testTag = "takedown_button"
                )

                // Primary FIRE button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFFFF0055), Color(0xFF880022))),
                            CircleShape
                        )
                        .border(2.dp, Color(0xFFFF5252), CircleShape)
                        .clickable { engine.fireWeapon() }
                        .testTag("fire_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Adjust,
                            contentDescription = "Fire",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (engine.currentWeapon.isReloading) "RELOAD" else "FIRE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopStatusBar(
    engine: GameEngine,
    onPauseClick: () -> Unit,
    onInventoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xDD080E14), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // District Title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = engine.world.district.title.uppercase(),
                color = Color(0xFF00F0FF),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            // Stat Bars
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                StatMeter(label = "HP", value = engine.playerHealth, max = 100, color = Color(0xFFFF1744))
                StatMeter(label = "ARMOR", value = engine.playerArmor, max = 100, color = Color(0xFF00E5FF))
                StatMeter(label = "NRG", value = engine.playerEnergy.toInt(), max = 100, color = Color(0xFF00FF66))
            }
        }

        // Alarms Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Alarms",
                tint = if (engine.alarmsTriggered > 0) Color(0xFFFF1744) else Color(0xFF4A6572),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "ALARM: ${engine.alarmsTriggered}",
                color = if (engine.alarmsTriggered > 0) Color(0xFFFF1744) else Color(0xFF78909C),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        // Ghost Index Indicator
        if (engine.ghostIndexAcquired) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF00FF66), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "INDEX SECURED",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Inventory & Pause Buttons
        IconButton(onClick = onInventoryClick, modifier = Modifier.size(32.dp).testTag("inventory_button")) {
            Icon(Icons.Default.Inventory2, contentDescription = "Inventory", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onPauseClick, modifier = Modifier.size(32.dp).testTag("pause_button")) {
            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun StatMeter(label: String, value: Int, max: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label ",
            color = Color.LightGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1E2630))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((value.toFloat() / max).coerceIn(0f, 1f))
                    .background(color)
            )
        }
    }
}

@Composable
fun HudIconButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor.copy(alpha = 0.25f) else Color(0xBB080E16)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(54.dp)
            .border(
                1.dp,
                if (isActive) activeColor else Color(0x44FFFFFF),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (isActive) activeColor else Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ConsumableButton(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xAA080E16)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .size(46.dp)
            .border(1.dp, if (count > 0) color.copy(alpha = 0.6f) else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
            .clickable(enabled = count > 0, onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (count > 0) color else Color.DarkGray,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "x$count",
                color = if (count > 0) Color.White else Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SmallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xDD00F0FF) else Color(0xAA080E16)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (isActive) Color.Black else Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
