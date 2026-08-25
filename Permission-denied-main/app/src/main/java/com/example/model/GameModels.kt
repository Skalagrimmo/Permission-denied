package com.example.model

enum class WeaponType(val displayName: String, val isLethal: Boolean, val baseNoiseRadius: Float) {
    SILENCED_PISTOL("Silenced Whisper", true, 3.5f),
    ARC_STUNNER("Arc Stunner", false, 1.0f),
    CYBER_SMG("Spectre SMG", true, 18.0f),
    EMP_GRENADE("EMP Disruptor", false, 12.0f),
    CYBERDECK("Cyberdeck MK-IV", false, 0.0f),
    LOCKPICK("Nanowire Pick", false, 0.5f)
}

data class Weapon(
    val type: WeaponType,
    var ammoInMag: Int,
    val maxMag: Int,
    var reserveAmmo: Int,
    val damage: Int,
    val fireRateMs: Long,
    val reloadTimeMs: Long,
    var lastFiredTime: Long = 0L,
    var isReloading: Boolean = false,
    var reloadStartTime: Long = 0L
) {
    val canFire: Boolean
        get() = ammoInMag > 0 && !isReloading

    companion object {
        fun createDefaultPistol() = Weapon(
            type = WeaponType.SILENCED_PISTOL,
            ammoInMag = 12,
            maxMag = 12,
            reserveAmmo = 36,
            damage = 38,
            fireRateMs = 280,
            reloadTimeMs = 1200
        )

        fun createStunner() = Weapon(
            type = WeaponType.ARC_STUNNER,
            ammoInMag = 1,
            maxMag = 1,
            reserveAmmo = 8,
            damage = 100, // Non-lethal knockout
            fireRateMs = 1000,
            reloadTimeMs = 1500
        )

        fun createSmg() = Weapon(
            type = WeaponType.CYBER_SMG,
            ammoInMag = 30,
            maxMag = 30,
            reserveAmmo = 90,
            damage = 22,
            fireRateMs = 95,
            reloadTimeMs = 1800
        )

        fun createEmpGrenade() = Weapon(
            type = WeaponType.EMP_GRENADE,
            ammoInMag = 3,
            maxMag = 3,
            reserveAmmo = 3,
            damage = 60,
            fireRateMs = 800,
            reloadTimeMs = 500
        )

        fun createCyberdeck() = Weapon(
            type = WeaponType.CYBERDECK,
            ammoInMag = 100,
            maxMag = 100,
            reserveAmmo = 100,
            damage = 0,
            fireRateMs = 500,
            reloadTimeMs = 0
        )

        fun createLockpick() = Weapon(
            type = WeaponType.LOCKPICK,
            ammoInMag = 5,
            maxMag = 5,
            reserveAmmo = 5,
            damage = 0,
            fireRateMs = 500,
            reloadTimeMs = 0
        )
    }
}

enum class AugmentationType(
    val title: String,
    val subtitle: String,
    val description: String,
    val energyCostPerSec: Float,
    val energyCostInstant: Float,
    val cooldownMs: Long
) {
    ACTIVE_CAMO(
        "Ghost Camouflage",
        "Optical Cloak",
        "Refracts light to reduce visual detection by 90%. Consumes 12 energy/sec.",
        energyCostPerSec = 12.0f,
        energyCostInstant = 0f,
        cooldownMs = 2000L
    ),
    CYBER_OPTICS(
        "Cybernetic Optics",
        "Thermal & Security HUD",
        "Highlights enemies, cameras, laser grids, and loot through walls.",
        energyCostPerSec = 6.0f,
        energyCostInstant = 0f,
        cooldownMs = 1000L
    ),
    DASH_THRUSTERS(
        "Kinetic Thrusters",
        "Rapid Dash",
        "Propels player forward instantly in a burst of speed. Clears laser grids.",
        energyCostPerSec = 0f,
        energyCostInstant = 25.0f,
        cooldownMs = 2500L
    ),
    NEURAL_OVERCLOCK(
        "Neural Overclock",
        "Reflex Dilation",
        "Dilates perception by 50% for 4.5 seconds for precision aiming and evasion.",
        energyCostPerSec = 0f,
        energyCostInstant = 40.0f,
        cooldownMs = 8000L
    ),
    SUBDERMAL_PLATING(
        "Subdermal Plating",
        "Ballistic Armor",
        "Passive bio-weave reduces incoming kinetic and shock damage by 35%.",
        energyCostPerSec = 0f,
        energyCostInstant = 0f,
        cooldownMs = 0L
    ),
    AUDIO_DAMPENER(
        "Acoustic Dampener",
        "Silent Steps",
        "Passive leg dampers eliminate footstep audio and reduce sprint noise by 80%.",
        energyCostPerSec = 0f,
        energyCostInstant = 0f,
        cooldownMs = 0L
    )
}

enum class ItemType {
    HEALTH_STIM,
    ARMOR_PLATE,
    ENERGY_BATTERY,
    EMP_CELL,
    KEYCARD_BLUE,
    KEYCARD_RED,
    EVIDENCE_SLATE,
    FACTION_CONTRACT
}

data class InventoryItem(
    val id: String,
    val type: ItemType,
    val name: String,
    val description: String,
    var count: Int = 1
)

enum class EnemyType(val maxHp: Int, val maxArmor: Int, val speed: Float, val detectionFov: Float) {
    SECURITY_GUARD(60, 0, 2.2f, 75f),
    HEAVY_ENFORCER(130, 80, 1.8f, 65f),
    SECURITY_DRONE(45, 0, 3.4f, 85f),
    SURVEILLANCE_CAMERA(30, 0, 0f, 90f),
    CEILING_TURRET(100, 40, 0f, 80f),
    AEGIS_PALADIN(400, 250, 2.6f, 100f) // Spire Boss
}

enum class EnemyAiState {
    IDLE,
    PATROL,
    INVESTIGATING,
    ALERT,
    COMBAT,
    UNCONSCIOUS,
    DEAD,
    HACKED_FRIENDLY
}

data class Enemy(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float, // height / elevation
    var z: Float,
    var yaw: Float,
    var hp: Int = type.maxHp,
    var armor: Int = type.maxArmor,
    var state: EnemyAiState = EnemyAiState.PATROL,
    var alertLevel: Float = 0f, // 0.0 to 100.0
    var targetX: Float = x,
    var targetZ: Float = z,
    val patrolPoints: List<Pair<Float, Float>> = emptyList(),
    var currentPatrolIndex: Int = 0,
    var stateTimer: Float = 0f,
    var shootCooldown: Float = 0f,
    var isBoss: Boolean = false,
    var bossPhase: Int = 1,
    var bossShieldActive: Boolean = false,
    var isPoweredOff: Boolean = false
)

enum class TileType(val isSolid: Boolean, val blocksSight: Boolean, val glyphChar: Char) {
    EMPTY(false, false, ' '),
    FLOOR(false, false, '.'),
    WALL_CONCRETE(true, true, '#'),
    WALL_GLASS(true, false, '|'),
    WALL_CORP_PANEL(true, true, 'H'),
    DOOR_CLOSED(true, true, '+'),
    DOOR_OPEN(false, false, '/'),
    DOOR_LOCKED_BLUE(true, true, 'B'),
    DOOR_LOCKED_RED(true, true, 'R'),
    VENT_SHAFT(false, false, '='),
    TERMINAL(true, false, '$'),
    CHEST_LOOT(true, false, '8'),
    LASER_TRIPWIRE(false, false, '!'),
    CHECKPOINT_TERMINAL(true, false, '&'),
    SPIRE_ELEVATOR(false, false, '%'),
    GHOST_INDEX_MAINFRAME(true, false, '@'),
    EXTRACTION_ZONE(false, false, '*')
}

data class MapTile(
    val type: TileType,
    val height: Float = 1.0f,
    val color: Int = 0xFF00FF00.toInt(),
    val metadata: String = ""
)

enum class DistrictId(val level: Int, val title: String, val theme: String, val size: Int) {
    DISTRICT_01(1, "District 01: Neon Spires", "Infiltration & Outskirts", 8),
    DISTRICT_02(2, "District 02: Slum Grid", "Drone Alley & Black Market", 10),
    DISTRICT_03(3, "District 03: Industrial Core", "Heavy Enforcers & Laser Arrays", 12),
    DISTRICT_04(4, "District 04: Corporate Plaza", "Aegis High-Sec & Biometrics", 14),
    DISTRICT_05(5, "District 05: Census Megaspire", "The Aegis Paladin & Ghost Index", 16)
}

enum class EndingChoice(val title: String, val description: String, val scoreBonus: Int) {
    LEAK("Leak to Public", "Broadcast encrypted citizen records. Sparks an underground uprising.", 5000),
    SELL("Sell to Syndicate", "Trade the Ghost Index on the black market for maximum untraceable credits.", 4000),
    SURRENDER("Surrender to Megacorp", "Return the Index for corporate immunity and executive clearance.", 2500),
    DESTROY("Destroy Forever", "Wipe the database permanently. No one controls the city's ghosts.", 6000)
}

data class MissionReport(
    val districtLevel: Int,
    val districtName: String,
    val score: Int,
    val stealthRating: String,
    val alarmsTriggered: Int,
    val enemiesKilled: Int,
    val enemiesStunned: Int,
    val terminalsHacked: Int,
    val evidenceFound: Int,
    val timeElapsedSeconds: Long,
    val endingChoice: EndingChoice?,
    val isVictory: Boolean
)
