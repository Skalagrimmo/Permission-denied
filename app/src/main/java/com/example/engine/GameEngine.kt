package com.example.engine

import com.example.audio.GameAudioEngine
import com.example.model.*
import java.util.Random
import kotlin.math.*

class GameEngine(
    val audioElement: GameAudioEngine = GameAudioEngine(),
    val haptics: GameHapticsEngine? = null
) {
    var world: GeneratedWorld = WorldGenerator.generateDistrict(DistrictId.DISTRICT_01)
        private set

    // Player State & Movement Controller
    val movementController: PlayerMovementController = PlayerMovementController()
    var playerX = 2.5f
    var playerY = 0.5f
    var playerZ = 2.5f
    var playerYaw = 0f   // In degrees
    var playerPitch = 0f // In degrees

    var playerHealth = 100
    var playerArmor = 50
    var playerEnergy = 100f
    var playerCredits = 250

    var isCrouching = false
    var isSprinting = false
    var isCloaked = false
    var isThermalVision = false
    var isOverclocked = false
    var overclockTimer = 0f
    var dashCooldownTimer = 0f
    var remoteHackCooldown = 0f
    val remoteHackMaxCooldown = 5.0f

    // Live High-Contrast ASCII HUD Notifications Queue
    val hudNotifications = mutableListOf<HudNotification>()

    val weapons = mutableListOf<Weapon>(
        Weapon.createDefaultPistol(),
        Weapon.createStunner(),
        Weapon.createSmg(),
        Weapon.createEmpGrenade()
    )
    var currentWeaponIndex = 0
    val currentWeapon: Weapon
        get() = weapons[currentWeaponIndex]

    val inventory = mutableListOf<InventoryItem>(
        InventoryItem("stim_1", ItemType.HEALTH_STIM, "Medi-Stim Pack", "Restores 40 Health", 2),
        InventoryItem("armor_1", ItemType.ARMOR_PLATE, "Ceramic Armor Plate", "Restores 50 Armor", 1),
        InventoryItem("bat_1", ItemType.ENERGY_BATTERY, "Bio-Battery", "Restores 60 Energy", 2),
        InventoryItem("emp_1", ItemType.EMP_CELL, "EMP Cell", "Ammo for EMP Disruptor", 2)
    )

    val activeAugmentations = mutableSetOf<AugmentationType>(
        AugmentationType.ACTIVE_CAMO,
        AugmentationType.CYBER_OPTICS
    )

    // Game Session Stats
    var alarmsTriggered = 0
    var enemiesKilled = 0
    var enemiesStunned = 0
    var terminalsHacked = 0
    var evidenceCollected = 0
    var contractsCompleted = 0
    var ghostIndexAcquired = false
    var isMissionComplete = false
    var isGameOver = false
    var missionReport: MissionReport? = null

    var elapsedSeconds = 0L
    private var secondAccumulator = 0f

    // Visual FX timers
    var muzzleFlashAlpha = 0f
    var damageFlashAlpha = 0f
    var footstepTimer = 0f

    // Threat haptics timers
    private var lastThreatPulseTimer = 0f
    private var lastSuspiciousPulseTimer = 0f
    private var laserTripwireTimer = 0f

    // Active Hacking Session (if any)
    var activeHacking: HackingSession? = null

    // Interaction prompt near player
    var nearbyPrompt: String? = null
    var nearbyInteractableAction: (() -> Unit)? = null

    fun loadDistrict(district: DistrictId, seed: Long = 1337L) {
        world = WorldGenerator.generateDistrict(district, seed)
        playerX = world.spawnX
        playerZ = world.spawnZ
        playerYaw = 0f
        playerPitch = 0f
        playerHealth = 100
        playerArmor = 50
        playerEnergy = 100f
        ghostIndexAcquired = false
        isMissionComplete = false
        isGameOver = false
        missionReport = null
        alarmsTriggered = 0
        enemiesKilled = 0
        enemiesStunned = 0
        terminalsHacked = 0
        evidenceCollected = 0
        contractsCompleted = 0
        elapsedSeconds = 0L
        remoteHackCooldown = 0f
        hudNotifications.clear()
        audioElement.setAlarmLevel(0)
        postNotification("NET_LINK", "NEURAL LINK ESTABLISHED // ${district.title.uppercase()}", NotificationLevel.SUCCESS)
    }

    fun postNotification(
        tag: String,
        message: String,
        level: NotificationLevel = NotificationLevel.INFO,
        durationSec: Float = 3.5f
    ) {
        val totalSec = elapsedSeconds
        val mm = (totalSec / 60).toString().padStart(2, '0')
        val ss = (totalSec % 60).toString().padStart(2, '0')
        val notif = HudNotification(
            tag = tag,
            message = message,
            level = level,
            timestampText = "$mm:$ss",
            remainingLifetimeSec = durationSec,
            initialLifetimeSec = durationSec
        )
        hudNotifications.add(0, notif)
        if (hudNotifications.size > 5) {
            hudNotifications.removeAt(hudNotifications.lastIndex)
        }
    }

    fun performRemoteQuickHack(): Boolean {
        if (remoteHackCooldown > 0f) {
            postNotification("ICE_WARN", "CYBERDECK RECHARGE [${String.format(java.util.Locale.US, "%.1f", remoteHackCooldown)}s]", NotificationLevel.WARNING)
            return false
        }
        if (playerEnergy < 25f) {
            postNotification("PWR_WARN", "INSUFFICIENT ENERGY FOR REMOTE HACK", NotificationLevel.WARNING)
            return false
        }

        // Search for nearest enemy, camera, or turret
        var target: Enemy? = null
        var minDistance = 14.0f
        world.enemies.filter { it.hp > 0 && it.state != EnemyAiState.UNCONSCIOUS && !it.isPoweredOff }.forEach { enemy ->
            val dist = MathUtils.distance2D(playerX, playerZ, enemy.x, enemy.z)
            if (dist < minDistance) {
                minDistance = dist
                target = enemy
            }
        }

        val enemyTarget = target
        if (enemyTarget != null) {
            playerEnergy = max(0f, playerEnergy - 25f)
            remoteHackCooldown = remoteHackMaxCooldown
            audioElement.playHackBeep(true)
            haptics?.onHackSuccess()

            if (enemyTarget.type == EnemyType.SURVEILLANCE_CAMERA || enemyTarget.type == EnemyType.CEILING_TURRET) {
                enemyTarget.isPoweredOff = true
                postNotification("SYS_OVERRIDE", "REMOTE ICE BREACH: ${enemyTarget.type.name} DISABLED", NotificationLevel.SUCCESS)
            } else {
                enemyTarget.state = EnemyAiState.UNCONSCIOUS
                enemyTarget.hp = 0
                enemiesStunned++
                postNotification("NEURAL_EMP", "SYNAPSE OVERLOAD: ${enemyTarget.type.name} NEUTRALIZED", NotificationLevel.SUCCESS)
            }
            return true
        } else {
            postNotification("SCAN_FAIL", "NO REMOTE TARGETS IN WIRELESS RANGE", NotificationLevel.INFO)
            return false
        }
    }

    fun update(moveX: Float, moveZ: Float, deltaYaw: Float, deltaPitch: Float, deltaSec: Float) {
        if (isGameOver || isMissionComplete) return

        val simDelta = if (isOverclocked) deltaSec * 0.5f else deltaSec

        secondAccumulator += deltaSec
        if (secondAccumulator >= 1.0f) {
            elapsedSeconds += 1
            secondAccumulator -= 1.0f
        }

        // Update View Angles (Non-inverted FPS orientation)
        val (newYaw, newPitch) = movementController.updateViewAngles(playerYaw, playerPitch, deltaYaw, deltaPitch)
        playerYaw = newYaw
        playerPitch = newPitch

        // Augmentation Timers & Energy
        if (isCloaked) {
            val cost = AugmentationType.ACTIVE_CAMO.energyCostPerSec * simDelta
            if (playerEnergy >= cost) {
                playerEnergy -= cost
            } else {
                isCloaked = false
            }
        }
        if (isThermalVision) {
            val cost = AugmentationType.CYBER_OPTICS.energyCostPerSec * simDelta
            if (playerEnergy >= cost) {
                playerEnergy -= cost
            } else {
                isThermalVision = false
            }
        }
        if (isOverclocked) {
            overclockTimer -= deltaSec
            if (overclockTimer <= 0f) {
                isOverclocked = false
            }
        }
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= deltaSec
        }
        if (remoteHackCooldown > 0f) {
            remoteHackCooldown = max(0f, remoteHackCooldown - deltaSec)
        }

        // Decay Notifications
        val iter = hudNotifications.iterator()
        while (iter.hasNext()) {
            val notif = iter.next()
            notif.remainingLifetimeSec -= deltaSec
            if (notif.remainingLifetimeSec <= 0f) {
                iter.remove()
            }
        }

        // Passive Energy Recharge
        if (!isCloaked && !isThermalVision && playerEnergy < 100f) {
            playerEnergy = min(100f, playerEnergy + 5.0f * simDelta)
        }

        // Weapon Reload Timers
        weapons.forEach { w ->
            if (w.isReloading && System.currentTimeMillis() - w.reloadStartTime >= w.reloadTimeMs) {
                val needed = w.maxMag - w.ammoInMag
                val toLoad = min(needed, w.reserveAmmo)
                w.ammoInMag += toLoad
                w.reserveAmmo -= toLoad
                w.isReloading = false
            }
        }

        // Decay Visual FX
        if (muzzleFlashAlpha > 0f) muzzleFlashAlpha = max(0f, muzzleFlashAlpha - deltaSec * 6.0f)
        if (damageFlashAlpha > 0f) damageFlashAlpha = max(0f, damageFlashAlpha - deltaSec * 3.0f)

        // 3D Movement with Continuous ASCII Grid Collision & Sliding
        val currentSpeed = when {
            isCrouching -> movementController.crouchSpeed
            isSprinting -> movementController.sprintSpeed
            else -> movementController.walkSpeed
        }

        val (vx, vz) = movementController.computeVelocity(moveX, moveZ, playerYaw, currentSpeed, simDelta)

        // Footsteps
        if (abs(vx) > 0.001f || abs(vz) > 0.001f) {
            footstepTimer += simDelta
            val footstepInterval = if (isSprinting) 0.3f else 0.55f
            if (footstepTimer >= footstepInterval) {
                footstepTimer = 0f
                if (!activeAugmentations.contains(AugmentationType.AUDIO_DAMPENER)) {
                    audioElement.playFootstep()
                    propagateSound(playerX, playerZ, if (isSprinting) 10f else 4f)
                }
            }
        }

        // Resolve 3D motion against ASCII grid with sliding collision resolution
        val (resolvedX, resolvedZ) = movementController.resolveGridMovement(playerX, playerZ, vx, vz, world)
        playerX = resolvedX
        playerZ = resolvedZ

        // Check Laser Tripwires
        val gridX = playerX.toInt()
        val gridZ = playerZ.toInt()
        if (gridX in 0 until world.width && gridZ in 0 until world.height) {
            if (world.grid[gridX][gridZ] == TileType.LASER_TRIPWIRE && laserTripwireTimer <= 0f) {
                laserTripwireTimer = 2.0f
                damagePlayer(15)
                triggerAlarm()
            }
        }
        if (laserTripwireTimer > 0f) laserTripwireTimer -= deltaSec

        // Update Hacking Session if active
        activeHacking?.let { hack ->
            hack.updateTick(deltaSec)
            if (hack.isSuccess) {
                terminalsHacked++
                playerCredits += 150
                audioElement.playHackBeep(true)
                haptics?.onHackSuccess()
                // Disable turrets/cameras in sector or power off
                world.enemies.filter { it.type == EnemyType.CEILING_TURRET || it.type == EnemyType.SURVEILLANCE_CAMERA }
                    .forEach { it.isPoweredOff = true }
                activeHacking = null
            } else if (hack.isFailed) {
                triggerAlarm()
                audioElement.playHackBeep(false)
                haptics?.onHackFailed()
                activeHacking = null
            }
        }

        // Update Enemies AI & Threat Detection Haptics
        updateEnemies(simDelta)

        var maxAlert = 0f
        world.enemies.filter { it.hp > 0 && it.state != EnemyAiState.UNCONSCIOUS && !it.isPoweredOff }.forEach { e ->
            if (e.alertLevel > maxAlert) maxAlert = e.alertLevel
        }
        if (maxAlert >= 65f && lastThreatPulseTimer <= 0f) {
            haptics?.onDetectionWarning()
            lastThreatPulseTimer = 1.2f
        } else if (maxAlert >= 30f && lastSuspiciousPulseTimer <= 0f) {
            haptics?.onSuspiciousPing()
            lastSuspiciousPulseTimer = 2.2f
        }
        if (lastThreatPulseTimer > 0f) lastThreatPulseTimer -= deltaSec
        if (lastSuspiciousPulseTimer > 0f) lastSuspiciousPulseTimer -= deltaSec

        // Check Interactions Near Player
        checkNearbyInteractions()
    }

    fun isSolidAt(x: Float, z: Float): Boolean {
        return movementController.checkCollisionWithWorld(x, z, world)
    }

    fun fireWeapon(): Boolean {
        val weapon = currentWeapon
        val now = System.currentTimeMillis()
        if (now - weapon.lastFiredTime < weapon.fireRateMs) return false
        if (weapon.ammoInMag <= 0) {
            reloadCurrentWeapon()
            return false
        }

        weapon.ammoInMag--
        weapon.lastFiredTime = now
        muzzleFlashAlpha = 1.0f

        when (weapon.type) {
            WeaponType.SILENCED_PISTOL -> {
                audioElement.playShootPistol()
                propagateSound(playerX, playerZ, weapon.type.baseNoiseRadius)
                raycastBullet(weapon.damage, isLethal = true)
            }
            WeaponType.ARC_STUNNER -> {
                audioElement.playStunnerZap()
                propagateSound(playerX, playerZ, weapon.type.baseNoiseRadius)
                raycastBullet(weapon.damage, isLethal = false)
            }
            WeaponType.CYBER_SMG -> {
                audioElement.playShootSmg()
                propagateSound(playerX, playerZ, weapon.type.baseNoiseRadius)
                raycastBullet(weapon.damage, isLethal = true)
            }
            WeaponType.EMP_GRENADE -> {
                audioElement.playEmpBlast()
                triggerEmpBlast(playerX + sin(playerYaw * MathUtils.DEG_TO_RAD) * 3f, playerZ + cos(playerYaw * MathUtils.DEG_TO_RAD) * 3f)
            }
            else -> {}
        }
        haptics?.onWeaponFired(weapon.type)
        return true
    }

    fun reloadCurrentWeapon() {
        val w = currentWeapon
        if (w.isReloading || w.ammoInMag >= w.maxMag || w.reserveAmmo <= 0) return
        w.isReloading = true
        w.reloadStartTime = System.currentTimeMillis()
        audioElement.playUiClick()
    }

    fun performTakedown(): Boolean {
        val stealthTarget = world.enemies.find { e ->
            e.hp > 0 && e.state != EnemyAiState.UNCONSCIOUS &&
                    MathUtils.distance2D(playerX, playerZ, e.x, e.z) < 1.8f &&
                    e.alertLevel < 60f
        } ?: return false

        stealthTarget.state = EnemyAiState.UNCONSCIOUS
        stealthTarget.hp = 0
        enemiesStunned++
        audioElement.playStunnerZap()
        haptics?.onTakedownImpact()
        return true
    }

    fun activateDash(): Boolean {
        if (!activeAugmentations.contains(AugmentationType.DASH_THRUSTERS)) return false
        if (dashCooldownTimer > 0f || playerEnergy < AugmentationType.DASH_THRUSTERS.energyCostInstant) return false

        playerEnergy -= AugmentationType.DASH_THRUSTERS.energyCostInstant
        dashCooldownTimer = 2.5f

        val radYaw = playerYaw * MathUtils.DEG_TO_RAD
        val dashDist = 4.0f
        val targetX = playerX + sin(radYaw) * dashDist
        val targetZ = playerZ + cos(radYaw) * dashDist

        if (!isSolidAt(targetX, targetZ)) {
            playerX = targetX
            playerZ = targetZ
        }
        audioElement.playUiClick()
        haptics?.onDashSurge()
        return true
    }

    fun activateOverclock(): Boolean {
        if (!activeAugmentations.contains(AugmentationType.NEURAL_OVERCLOCK)) return false
        if (isOverclocked || playerEnergy < AugmentationType.NEURAL_OVERCLOCK.energyCostInstant) return false

        playerEnergy -= AugmentationType.NEURAL_OVERCLOCK.energyCostInstant
        isOverclocked = true
        overclockTimer = 4.5f
        audioElement.playUiClick()
        haptics?.onDashSurge()
        return true
    }

    fun toggleCamo(): Boolean {
        if (!activeAugmentations.contains(AugmentationType.ACTIVE_CAMO)) return false
        if (!isCloaked && playerEnergy < 15f) return false
        isCloaked = !isCloaked
        audioElement.playUiClick()
        haptics?.onCloakToggle(isCloaked)
        return true
    }

    fun toggleThermal(): Boolean {
        if (!activeAugmentations.contains(AugmentationType.CYBER_OPTICS)) return false
        if (!isThermalVision && playerEnergy < 10f) return false
        isThermalVision = !isThermalVision
        audioElement.playUiClick()
        haptics?.onCloakToggle(isThermalVision)
        return true
    }

    fun useItem(itemType: ItemType): Boolean {
        val item = inventory.find { it.type == itemType && it.count > 0 } ?: return false
        when (itemType) {
            ItemType.HEALTH_STIM -> {
                if (playerHealth >= 100) return false
                playerHealth = min(100, playerHealth + 40)
            }
            ItemType.ARMOR_PLATE -> {
                if (playerArmor >= 100) return false
                playerArmor = min(100, playerArmor + 50)
            }
            ItemType.ENERGY_BATTERY -> {
                if (playerEnergy >= 100f) return false
                playerEnergy = min(100f, playerEnergy + 60f)
            }
            ItemType.EMP_CELL -> {
                val empWeapon = weapons.find { it.type == WeaponType.EMP_GRENADE }
                empWeapon?.let { it.reserveAmmo += 2 }
            }
            else -> return false
        }
        item.count--
        if (item.count <= 0) inventory.remove(item)
        audioElement.playUiClick()
        return true
    }

    private fun raycastBullet(damage: Int, isLethal: Boolean) {
        val radYaw = playerYaw * MathUtils.DEG_TO_RAD
        val dirX = sin(radYaw)
        val dirZ = cos(radYaw)

        var closestEnemy: Enemy? = null
        var closestDist = 18.0f

        world.enemies.filter { it.hp > 0 && it.state != EnemyAiState.UNCONSCIOUS }.forEach { enemy ->
            val ex = enemy.x - playerX
            val ez = enemy.z - playerZ
            val dot = ex * dirX + ez * dirZ
            if (dot > 0.5f && dot < closestDist) {
                // Cross product check for ray width
                val cross = abs(ex * dirZ - ez * dirX)
                if (cross < 0.8f) {
                    closestDist = dot
                    closestEnemy = enemy
                }
            }
        }

        closestEnemy?.let { enemy ->
            if (enemy.isBoss && enemy.bossShieldActive && isLethal) {
                // Deflected by Paladin Shield unless stunner/EMP
                damageFlashAlpha = 0.3f
                return
            }

            if (!isLethal) {
                enemy.state = EnemyAiState.UNCONSCIOUS
                enemy.hp = 0
                enemiesStunned++
            } else {
                val dmg = if (enemy.armor > 0) {
                    val armorAbsorb = min(enemy.armor, damage / 2)
                    enemy.armor -= armorAbsorb
                    damage - armorAbsorb
                } else damage

                enemy.hp -= dmg
                if (enemy.hp <= 0) {
                    enemy.hp = 0
                    enemy.state = EnemyAiState.DEAD
                    enemiesKilled++
                } else {
                    enemy.state = EnemyAiState.COMBAT
                    enemy.alertLevel = 100f
                }
            }
        }
    }

    private fun triggerEmpBlast(centerX: Float, centerZ: Float) {
        world.enemies.forEach { enemy ->
            val dist = MathUtils.distance2D(centerX, centerZ, enemy.x, enemy.z)
            if (dist < 6.0f) {
                if (enemy.isBoss) {
                    enemy.bossShieldActive = false // Break boss shield!
                    enemy.hp -= 40
                } else if (enemy.type == EnemyType.CEILING_TURRET || enemy.type == EnemyType.SURVEILLANCE_CAMERA || enemy.type == EnemyType.SECURITY_DRONE) {
                    enemy.hp = 0
                    enemy.isPoweredOff = true
                    enemy.state = EnemyAiState.DEAD
                } else {
                    enemy.state = EnemyAiState.UNCONSCIOUS
                    enemy.hp = 0
                    enemiesStunned++
                }
            }
        }
    }

    private fun propagateSound(sourceX: Float, sourceZ: Float, radius: Float) {
        world.enemies.filter { it.hp > 0 && it.state != EnemyAiState.UNCONSCIOUS }.forEach { enemy ->
            val dist = MathUtils.distance2D(sourceX, sourceZ, enemy.x, enemy.z)
            if (dist < radius) {
                enemy.targetX = sourceX
                enemy.targetZ = sourceZ
                if (enemy.state != EnemyAiState.COMBAT) {
                    enemy.state = EnemyAiState.INVESTIGATING
                    enemy.alertLevel = min(100f, enemy.alertLevel + 45f)
                }
            }
        }
    }

    private fun updateEnemies(deltaSec: Float) {
        val rng = Random()
        world.enemies.filter { it.hp > 0 && it.state != EnemyAiState.UNCONSCIOUS && !it.isPoweredOff }.forEach { enemy ->
            val distToPlayer = MathUtils.distance2D(playerX, playerZ, enemy.x, enemy.z)

            // Boss logic
            if (enemy.isBoss) {
                updateBossAi(enemy, distToPlayer, deltaSec)
                return@forEach
            }

            // Stationary devices (Camera / Turret)
            if (enemy.type == EnemyType.SURVEILLANCE_CAMERA || enemy.type == EnemyType.CEILING_TURRET) {
                if (distToPlayer < 10.0f && canSeePlayer(enemy)) {
                    enemy.alertLevel += deltaSec * (if (isCloaked) 8f else 50f)
                    if (enemy.alertLevel >= 100f) {
                        triggerAlarm()
                        if (enemy.type == EnemyType.CEILING_TURRET) {
                            enemy.shootCooldown -= deltaSec
                            if (enemy.shootCooldown <= 0f) {
                                enemy.shootCooldown = 0.8f
                                damagePlayer(15)
                            }
                        }
                    }
                } else {
                    enemy.alertLevel = max(0f, enemy.alertLevel - deltaSec * 15f)
                }
                return@forEach
            }

            // Patrol Guards / Drones
            when (enemy.state) {
                EnemyAiState.PATROL, EnemyAiState.IDLE -> {
                    if (canSeePlayer(enemy)) {
                        enemy.alertLevel += deltaSec * (if (isCloaked) 15f else 65f)
                        if (enemy.alertLevel >= 80f) {
                            enemy.state = EnemyAiState.COMBAT
                            triggerAlarm()
                        }
                    } else {
                        enemy.alertLevel = max(0f, enemy.alertLevel - deltaSec * 10f)
                    }

                    // Move between patrol points
                    if (enemy.patrolPoints.isNotEmpty()) {
                        val pt = enemy.patrolPoints[enemy.currentPatrolIndex]
                        val d = MathUtils.distance2D(enemy.x, enemy.z, pt.first, pt.second)
                        if (d < 0.5f) {
                            enemy.currentPatrolIndex = (enemy.currentPatrolIndex + 1) % enemy.patrolPoints.size
                        } else {
                            val dirX = (pt.first - enemy.x) / d
                            val dirZ = (pt.second - enemy.z) / d
                            enemy.x += dirX * enemy.type.speed * deltaSec
                            enemy.z += dirZ * enemy.type.speed * deltaSec
                            enemy.yaw = MathUtils.RAD_TO_DEG * atan2(dirX, dirZ)
                        }
                    }
                }

                EnemyAiState.INVESTIGATING -> {
                    val d = MathUtils.distance2D(enemy.x, enemy.z, enemy.targetX, enemy.targetZ)
                    if (canSeePlayer(enemy)) {
                        enemy.state = EnemyAiState.COMBAT
                        triggerAlarm()
                    } else if (d > 0.5f) {
                        val dirX = (enemy.targetX - enemy.x) / d
                        val dirZ = (enemy.targetZ - enemy.z) / d
                        enemy.x += dirX * enemy.type.speed * deltaSec
                        enemy.z += dirZ * enemy.type.speed * deltaSec
                    } else {
                        enemy.stateTimer += deltaSec
                        if (enemy.stateTimer > 4.0f) {
                            enemy.stateTimer = 0f
                            enemy.state = EnemyAiState.PATROL
                            enemy.alertLevel = 0f
                        }
                    }
                }

                EnemyAiState.COMBAT -> {
                    if (distToPlayer > 1.5f) {
                        val dirX = (playerX - enemy.x) / distToPlayer
                        val dirZ = (playerZ - enemy.z) / distToPlayer
                        enemy.x += dirX * enemy.type.speed * 1.3f * deltaSec
                        enemy.z += dirZ * enemy.type.speed * 1.3f * deltaSec
                    }

                    enemy.shootCooldown -= deltaSec
                    if (enemy.shootCooldown <= 0f && distToPlayer < 12f) {
                        enemy.shootCooldown = if (enemy.type == EnemyType.HEAVY_ENFORCER) 0.6f else 1.2f
                        damagePlayer(if (enemy.type == EnemyType.HEAVY_ENFORCER) 18 else 10)
                    }
                }
                else -> {}
            }
        }
    }

    private fun updateBossAi(boss: Enemy, distToPlayer: Float, deltaSec: Float) {
        if (boss.hp < 150 && boss.bossPhase == 1) {
            boss.bossPhase = 2 // Phase 2: Cyber Katana Dash
            boss.bossShieldActive = false
        }

        if (distToPlayer > 2.0f) {
            val dirX = (playerX - boss.x) / distToPlayer
            val dirZ = (playerZ - boss.z) / distToPlayer
            val bossSpeed = if (boss.bossPhase == 2) 3.5f else 2.0f
            boss.x += dirX * bossSpeed * deltaSec
            boss.z += dirZ * bossSpeed * deltaSec
        }

        boss.shootCooldown -= deltaSec
        if (boss.shootCooldown <= 0f) {
            boss.shootCooldown = if (boss.bossPhase == 2) 1.0f else 1.6f
            damagePlayer(if (boss.bossPhase == 2) 30 else 20)
        }
    }

    private fun canSeePlayer(enemy: Enemy): Boolean {
        val dist = MathUtils.distance2D(playerX, playerZ, enemy.x, enemy.z)
        val maxSight = if (isCloaked) 2.5f else if (isCrouching) 7.0f else 14.0f
        if (dist > maxSight) return false

        // Angle check
        val dx = playerX - enemy.x
        val dz = playerZ - enemy.z
        val angleToPlayer = MathUtils.RAD_TO_DEG * atan2(dx, dz)
        val angleDiff = abs(MathUtils.angleDifference(enemy.yaw, angleToPlayer))
        return angleDiff < (enemy.type.detectionFov * 0.5f)
    }

    private fun damagePlayer(amount: Int) {
        var dmg = amount
        if (activeAugmentations.contains(AugmentationType.SUBDERMAL_PLATING)) {
            dmg = (dmg * 0.65f).toInt()
        }

        val hasShield = playerArmor > 0
        var absorbedArmor = 0
        if (playerArmor > 0) {
            absorbedArmor = min(playerArmor, dmg)
            playerArmor -= absorbedArmor
            dmg -= absorbedArmor
        }
        playerHealth -= dmg
        damageFlashAlpha = 1.0f

        haptics?.onPlayerDamage(isShieldAbsorb = hasShield && dmg == 0, amount = amount)

        if (playerHealth <= 0) {
            playerHealth = 0
            isGameOver = true
            postNotification("SYS_HALT", "VITAL SIGNS FLATLINED // MISSION FAILED", NotificationLevel.CRITICAL)
        } else if (dmg > 0) {
            if (playerHealth < 25) {
                postNotification("INTEGRITY_CRIT", "CRITICAL DAMAGE: HP AT $playerHealth% - STIM REQ", NotificationLevel.CRITICAL)
            } else {
                postNotification("DAMAGE_TAKEN", "BIO-FEEDBACK: -$dmg HP (INTEGRITY: $playerHealth%)", NotificationLevel.WARNING)
            }
        } else if (hasShield) {
            postNotification("SHIELD_ABSORB", "SHIELD ABSORBED IMPACT (-$absorbedArmor ARM)", NotificationLevel.INFO)
        }
    }

    fun triggerAlarm() {
        alarmsTriggered++
        audioElement.setAlarmLevel(min(3, alarmsTriggered))
        audioElement.playAlarm()
        haptics?.onAlarmTriggered()
        postNotification("SYS_ALERT", "SECURITY ALARM LEVEL $alarmsTriggered TRIGGERED", NotificationLevel.CRITICAL)
        world.enemies.filter { it.hp > 0 && it.state != EnemyAiState.UNCONSCIOUS }.forEach {
            it.alertLevel = 100f
            it.state = EnemyAiState.ALERT
            it.targetX = playerX
            it.targetZ = playerZ
        }
    }

    private fun checkNearbyInteractions() {
        nearbyPrompt = null
        nearbyInteractableAction = null

        val px = playerX
        val pz = playerZ

        // 1. Spire Ghost Index Mainframe
        val distSpire = MathUtils.distance2D(px, pz, world.spireCoreX, world.spireCoreZ)
        if (distSpire < 2.2f && !ghostIndexAcquired) {
            nearbyPrompt = "HACK GHOST INDEX MAINFRAME"
            nearbyInteractableAction = {
                ghostIndexAcquired = true
                audioElement.playHackBeep(true)
                haptics?.onHackSuccess()
                playerCredits += 500
                postNotification("PRIMARY_OBJ", "GHOST INDEX ENCRYPTION CORE DECRYPTED [+500 CR]", NotificationLevel.SUCCESS, 5.0f)
            }
            return
        }

        // 2. Extraction Zone
        val distExtract = MathUtils.distance2D(px, pz, world.extractionX, world.extractionZ)
        if (distExtract < 2.0f && ghostIndexAcquired) {
            nearbyPrompt = "ESCAPE & EXTRACT"
            nearbyInteractableAction = {
                postNotification("EXFIL", "EXTRACTION CONFIRMED // MISSION REPORT GENERATING", NotificationLevel.SUCCESS)
                finishMission(EndingChoice.LEAK)
            }
            return
        }

        // 3. Doors
        val nearDoor = world.doors.find { MathUtils.distance2D(px, pz, it.x.toFloat() + 0.5f, it.z.toFloat() + 0.5f) < 1.8f }
        nearDoor?.let { door ->
            if (door.isOpen) {
                nearbyPrompt = "CLOSE DOOR"
                nearbyInteractableAction = {
                    door.isOpen = false
                    world.grid[door.x][door.z] = TileType.DOOR_CLOSED
                    audioElement.playUiClick()
                    haptics?.onUiClick()
                }
            } else if (!door.isLocked) {
                nearbyPrompt = "OPEN DOOR"
                nearbyInteractableAction = {
                    door.isOpen = true
                    world.grid[door.x][door.z] = TileType.DOOR_OPEN
                    audioElement.playUiClick()
                    haptics?.onUiClick()
                }
            } else {
                val reqKey = door.requiredKeycard
                val hasKey = inventory.any { it.type == reqKey && it.count > 0 }
                if (hasKey) {
                    nearbyPrompt = "UNLOCK WITH KEYCARD"
                    nearbyInteractableAction = {
                        door.isLocked = false
                        door.isOpen = true
                        world.grid[door.x][door.z] = TileType.DOOR_OPEN
                        audioElement.playUiClick()
                        haptics?.onKeycardUnlocked()
                        postNotification("SECURITY", "SECURITY ACCESS GRANTED: ${reqKey?.name ?: "KEYCARD"}", NotificationLevel.SUCCESS)
                    }
                } else {
                    nearbyPrompt = "DOOR LOCKED (${reqKey?.name ?: "KEY REQUIRED"})"
                }
            }
            return
        }

        // 4. Terminals
        val nearTerminal = world.terminals.find { MathUtils.distance2D(px, pz, it.x, it.z) < 1.8f && !it.isHacked }
        nearTerminal?.let { term ->
            nearbyPrompt = "JACK INTO TERMINAL (${term.sectorName})"
            nearbyInteractableAction = {
                activeHacking = HackingSession.createSession(term.id, term.sectorName, term.securityIceLevel)
                term.isHacked = true
                audioElement.playUiClick()
                haptics?.onHackNodeCaptured()
                postNotification("JACK_IN", "CYBERSAPACE PROTOCOL INITIATED // ICE LVL ${term.securityIceLevel}", NotificationLevel.INFO)
            }
            return
        }

        // 5. Loot Chests
        val nearChest = world.lootChests.find { MathUtils.distance2D(px, pz, it.x, it.z) < 1.8f && !it.isOpened }
        nearChest?.let { chest ->
            nearbyPrompt = "LOOT CONTAINER (${chest.itemType.name})"
            nearbyInteractableAction = {
                chest.isOpened = true
                val existing = inventory.find { it.type == chest.itemType }
                if (existing != null) {
                    existing.count += chest.count
                } else {
                    inventory.add(InventoryItem(chest.id.toString(), chest.itemType, chest.itemType.name.replace('_', ' '), "Collected item", chest.count))
                }
                if (chest.itemType == ItemType.EVIDENCE_SLATE) {
                    evidenceCollected++
                    postNotification("DATA_SLATE", "CLASSIFIED EVIDENCE RECOVERED ($evidenceCollected)", NotificationLevel.SUCCESS)
                } else if (chest.itemType == ItemType.FACTION_CONTRACT) {
                    contractsCompleted++
                    postNotification("CONTRACT", "FACTION BOUNTY CONTRACT SECURED ($contractsCompleted)", NotificationLevel.SUCCESS)
                } else {
                    postNotification("INVENTORY", "ACQUIRED: ${chest.itemType.name} x${chest.count}", NotificationLevel.INFO)
                }
                audioElement.playUiClick()
                haptics?.onLootCollected()
            }
            return
        }
    }

    fun finishMission(choice: EndingChoice) {
        isMissionComplete = true
        val stealthBonus = max(0, 3000 - (alarmsTriggered * 800) - (enemiesKilled * 300))
        val stealthRank = when {
            alarmsTriggered == 0 && enemiesKilled == 0 -> "GHOST (S+)"
            alarmsTriggered == 0 -> "SHADOW (A)"
            alarmsTriggered <= 2 -> "SPECTRE (B)"
            else -> "CHAOS (C)"
        }
        val finalScore = 2000 + choice.scoreBonus + stealthBonus + (terminalsHacked * 250) + (evidenceCollected * 400) + (enemiesStunned * 150)

        missionReport = MissionReport(
            districtLevel = world.district.level,
            districtName = world.district.title,
            score = finalScore,
            stealthRating = stealthRank,
            alarmsTriggered = alarmsTriggered,
            enemiesKilled = enemiesKilled,
            enemiesStunned = enemiesStunned,
            terminalsHacked = terminalsHacked,
            evidenceFound = evidenceCollected,
            timeElapsedSeconds = elapsedSeconds,
            endingChoice = choice,
            isVictory = true
        )
    }
}
