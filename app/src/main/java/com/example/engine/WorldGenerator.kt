package com.example.engine

import com.example.model.*
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class GeneratedWorld(
    val district: DistrictId,
    val seed: Long,
    val width: Int,
    val height: Int,
    val grid: Array<Array<TileType>>,
    val heights: Array<FloatArray>,
    val colors: Array<IntArray>,
    val spawnX: Float,
    val spawnZ: Float,
    val extractionX: Float,
    val extractionZ: Float,
    val spireCoreX: Float,
    val spireCoreZ: Float,
    val enemies: MutableList<Enemy>,
    val terminals: MutableList<TerminalObject>,
    val lootChests: MutableList<LootChestObject>,
    val doors: MutableList<DoorObject>,
    val evidenceCount: Int,
    val contractsCount: Int,
    val rooms: List<DistrictRoom> = emptyList()
)

enum class DistrictRoomType(val displayName: String, val baseSecurity: Int) {
    SAFEHOUSE_INSERTION("Safehouse Insertion Point", 1),
    SECURITY_COMMAND_HUB("Security Command Hub", 3),
    SERVER_MAINFRAME_VAULT("Server Mainframe Vault", 4),
    CYBERNETICS_LAB("Cybernetics Research Lab", 2),
    ARMORY_SUPPLY_CACHE("Armory Supply Cache", 3),
    MAINTENANCE_VENT_GRID("Maintenance & Vent Chasm", 1),
    EXECUTIVE_OFFICE("Executive Corporate Suite", 4),
    PATROL_PLAZA("Surveillance Atrium", 2),
    MEGASPIRE_SANCTUM("Megaspire Core Sanctum", 5),
    EXTRACTION_ROOFTOP("Extraction Rooftop Helipad", 1)
}

data class DistrictRoom(
    val id: Int,
    val type: DistrictRoomType,
    val x: Int,
    val z: Int,
    val width: Int,
    val height: Int,
    val centerX: Int = x + width / 2,
    val centerZ: Int = z + height / 2
) {
    fun intersects(other: DistrictRoom, padding: Int = 1): Boolean {
        return x - padding < other.x + other.width &&
                x + width + padding > other.x &&
                z - padding < other.z + other.height &&
                z + height + padding > other.z
    }
}

data class TerminalObject(
    val id: Int,
    val x: Float,
    val z: Float,
    val sectorName: String,
    val securityIceLevel: Int, // 1 to 5
    var isHacked: Boolean = false,
    val controlsTurrets: Boolean = true,
    val controlsCameras: Boolean = true,
    val controlsPower: Boolean = true
)

data class LootChestObject(
    val id: Int,
    val x: Float,
    val z: Float,
    val itemType: ItemType,
    val count: Int,
    var isOpened: Boolean = false
)

data class DoorObject(
    val id: Int,
    val x: Int,
    val z: Int,
    val requiredKeycard: ItemType?, // null if unlocked
    var isOpen: Boolean = false,
    var isLocked: Boolean = requiredKeycard != null
)

object WorldGenerator {

    data class DistrictThemePalette(
        val wallConcreteColor: Int,
        val wallGlassColor: Int,
        val wallCorpColor: Int,
        val floorColor: Int,
        val ventColor: Int,
        val accentColor: Int
    )

    private fun getPaletteForDistrict(district: DistrictId): DistrictThemePalette {
        return when (district) {
            DistrictId.DISTRICT_01 -> DistrictThemePalette(
                wallConcreteColor = 0xFF142436.toInt(),
                wallGlassColor = 0xFF00E5FF.toInt(),
                wallCorpColor = 0xFF1F354D.toInt(),
                floorColor = 0xFF080E17.toInt(),
                ventColor = 0xFF00B4D8.toInt(),
                accentColor = 0xFF00FFCC.toInt()
            )
            DistrictId.DISTRICT_02 -> DistrictThemePalette(
                wallConcreteColor = 0xFF212519.toInt(),
                wallGlassColor = 0xFF39FF14.toInt(),
                wallCorpColor = 0xFF2D361E.toInt(),
                floorColor = 0xFF0A0F08.toInt(),
                ventColor = 0xFF70E000.toInt(),
                accentColor = 0xFF55FF33.toInt()
            )
            DistrictId.DISTRICT_03 -> DistrictThemePalette(
                wallConcreteColor = 0xFF332014.toInt(),
                wallGlassColor = 0xFFFF9E00.toInt(),
                wallCorpColor = 0xFF422817.toInt(),
                floorColor = 0xFF120B07.toInt(),
                ventColor = 0xFFFF6000.toInt(),
                accentColor = 0xFFFF7700.toInt()
            )
            DistrictId.DISTRICT_04 -> DistrictThemePalette(
                wallConcreteColor = 0xFF1A1F3B.toInt(),
                wallGlassColor = 0xFF7000FF.toInt(),
                wallCorpColor = 0xFF262C54.toInt(),
                floorColor = 0xFF0A0B1A.toInt(),
                ventColor = 0xFF9D4EDD.toInt(),
                accentColor = 0xFFBD00FF.toInt()
            )
            DistrictId.DISTRICT_05 -> DistrictThemePalette(
                wallConcreteColor = 0xFF330C19.toInt(),
                wallGlassColor = 0xFFFF0055.toInt(),
                wallCorpColor = 0xFF4A1024.toInt(),
                floorColor = 0xFF140409.toInt(),
                ventColor = 0xFFFF007F.toInt(),
                accentColor = 0xFFFF0055.toInt()
            )
        }
    }

    fun generateDistrict(district: DistrictId, baseSeed: Long = 1337L): GeneratedWorld {
        val finalSeed = baseSeed + (district.level * 7919L)
        val rng = Random(finalSeed)
        val palette = getPaletteForDistrict(district)

        // District size dimension based on level
        val mapDim = when (district) {
            DistrictId.DISTRICT_01 -> 36
            DistrictId.DISTRICT_02 -> 42
            DistrictId.DISTRICT_03 -> 48
            DistrictId.DISTRICT_04 -> 54
            DistrictId.DISTRICT_05 -> 60
        }
        val width = mapDim
        val height = mapDim

        // Initialize grid, heights, colors
        val grid = Array(width) { Array(height) { TileType.WALL_CONCRETE } }
        val heights = Array(width) { FloatArray(height) { 2.5f } }
        val colors = Array(width) { IntArray(height) { palette.wallConcreteColor } }

        val rooms = mutableListOf<DistrictRoom>()
        val enemies = mutableListOf<Enemy>()
        val terminals = mutableListOf<TerminalObject>()
        val lootChests = mutableListOf<LootChestObject>()
        val doors = mutableListOf<DoorObject>()

        var doorIdCounter = 1
        var terminalIdCounter = 1
        var lootIdCounter = 1
        var enemyIdCounter = 1

        // 1. Procedural Room Partitioning & Placement
        val targetRoomCount = 8 + district.level * 2
        val minRoomSize = 5
        val maxRoomSize = 9

        // Always place Safehouse at top-left sector
        val safehouse = DistrictRoom(
            id = 1,
            type = DistrictRoomType.SAFEHOUSE_INSERTION,
            x = 2,
            z = 2,
            width = 6,
            height = 6
        )
        rooms.add(safehouse)

        // Always place Megaspire Sanctum in bottom-right / central-high-sec sector
        val spireSize = 7
        val spireSanctum = DistrictRoom(
            id = 2,
            type = DistrictRoomType.MEGASPIRE_SANCTUM,
            x = width - spireSize - 3,
            z = height - spireSize - 3,
            width = spireSize,
            height = spireSize
        )
        rooms.add(spireSanctum)

        // Always place Extraction Rooftop at designated perimeter
        val extractionRoom = DistrictRoom(
            id = 3,
            type = DistrictRoomType.EXTRACTION_ROOFTOP,
            x = width - 8,
            z = 2,
            width = 6,
            height = 6
        )
        rooms.add(extractionRoom)

        // Place Intermediate Themed Rooms across the district
        val roomTypesToAssign = mutableListOf(
            DistrictRoomType.SECURITY_COMMAND_HUB,
            DistrictRoomType.SERVER_MAINFRAME_VAULT,
            DistrictRoomType.CYBERNETICS_LAB,
            DistrictRoomType.ARMORY_SUPPLY_CACHE,
            DistrictRoomType.MAINTENANCE_VENT_GRID,
            DistrictRoomType.EXECUTIVE_OFFICE,
            DistrictRoomType.PATROL_PLAZA
        )

        var attempts = 0
        var nextRoomId = 4
        while (rooms.size < targetRoomCount && attempts < 250) {
            attempts++
            val rw = minRoomSize + rng.nextInt(maxRoomSize - minRoomSize + 1)
            val rh = minRoomSize + rng.nextInt(maxRoomSize - minRoomSize + 1)
            val rx = 2 + rng.nextInt(width - rw - 4)
            val rz = 2 + rng.nextInt(height - rh - 4)

            val newRoom = DistrictRoom(
                id = nextRoomId,
                type = if (roomTypesToAssign.isNotEmpty()) roomTypesToAssign.removeAt(0) else DistrictRoomType.PATROL_PLAZA,
                x = rx,
                z = rz,
                width = rw,
                height = rh
            )

            // Ensure rooms don't overlap, maintaining clear corridor space
            val hasOverlap = rooms.any { it.intersects(newRoom, padding = 2) }
            if (!hasOverlap) {
                rooms.add(newRoom)
                nextRoomId++
            }
        }

        // 2. Carve Rooms into Floor & Interior Features
        for (room in rooms) {
            carveRoom(room, grid, heights, colors, palette, rng)
        }

        // 3. Connect Rooms via Interconnected Corridor Spanning Network & Secondary Loops
        val connectedPairs = mutableSetOf<Pair<Int, Int>>()

        // Prim's/Kruskal-like nearest neighbor spanning tree for 100% connectivity guarantee
        val unconnectedRooms = rooms.toMutableList()
        val connectedRooms = mutableListOf(unconnectedRooms.removeAt(0))

        while (unconnectedRooms.isNotEmpty()) {
            var closestDistance = Double.MAX_VALUE
            var bestConnected: DistrictRoom? = null
            var bestUnconnected: DistrictRoom? = null

            for (cRoom in connectedRooms) {
                for (uRoom in unconnectedRooms) {
                    val dist = distanceSq(cRoom.centerX, cRoom.centerZ, uRoom.centerX, uRoom.centerZ)
                    if (dist < closestDistance) {
                        closestDistance = dist
                        bestConnected = cRoom
                        bestUnconnected = uRoom
                    }
                }
            }

            if (bestConnected != null && bestUnconnected != null) {
                carveCorridor(bestConnected, bestUnconnected, grid, heights, colors, palette, rng)
                connectedPairs.add(Pair(bestConnected.id, bestUnconnected.id))
                connectedRooms.add(bestUnconnected)
                unconnectedRooms.remove(bestUnconnected)
            }
        }

        // Add 25-35% secondary corridors for alternative flanking & infiltration routes
        for (i in 0 until rooms.size) {
            for (j in i + 1 until rooms.size) {
                val rA = rooms[i]
                val rB = rooms[j]
                val dist = sqrt(distanceSq(rA.centerX, rA.centerZ, rB.centerX, rB.centerZ))
                if (dist < 18.0 && !connectedPairs.contains(Pair(rA.id, rB.id)) && !connectedPairs.contains(Pair(rB.id, rA.id))) {
                    if (rng.nextFloat() < 0.35f) {
                        carveCorridor(rA, rB, grid, heights, colors, palette, rng)
                        connectedPairs.add(Pair(rA.id, rB.id))
                    }
                }
            }
        }

        // 4. Carve Stealth Ventilation Ducts (Parallel bypasses)
        carveVentilationNetwork(rooms, grid, heights, colors, palette, rng)

        // 5. Place Doors, Keycards, Terminals, Obstacles & Hazards
        var keycardBluePlaced = false
        var keycardRedPlaced = false

        for (room in rooms) {
            when (room.type) {
                DistrictRoomType.SAFEHOUSE_INSERTION -> {
                    // Safehouse entrance
                    val doorX = room.x + room.width - 1
                    val doorZ = room.centerZ
                    grid[doorX][doorZ] = TileType.DOOR_OPEN
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, null, isOpen = true, isLocked = false))

                    // Starting supply crates
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 1).toFloat(), ItemType.HEALTH_STIM, 2))
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 2).toFloat(), ItemType.ENERGY_BATTERY, 2))
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 3).toFloat(), ItemType.EMP_CELL, 1))
                }

                DistrictRoomType.MEGASPIRE_SANCTUM -> {
                    // Sealed Red Biometric Blast Door
                    val doorX = room.x
                    val doorZ = room.centerZ
                    grid[doorX][doorZ] = TileType.DOOR_LOCKED_RED
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, ItemType.KEYCARD_RED, isOpen = false, isLocked = true))

                    // Ghost Index Mainframe in center
                    grid[room.centerX][room.centerZ] = TileType.GHOST_INDEX_MAINFRAME

                    // Laser Tripwire grid surrounding the core
                    if (room.centerX - 1 >= 0 && grid[room.centerX - 1][room.centerZ] == TileType.FLOOR) {
                        grid[room.centerX - 1][room.centerZ] = TileType.LASER_TRIPWIRE
                    }
                    if (room.centerX + 1 < width && grid[room.centerX + 1][room.centerZ] == TileType.FLOOR) {
                        grid[room.centerX + 1][room.centerZ] = TileType.LASER_TRIPWIRE
                    }

                    // Boss: Aegis Paladin in Sanctum
                    enemies.add(
                        Enemy(
                            id = enemyIdCounter++,
                            type = EnemyType.AEGIS_PALADIN,
                            x = (room.centerX).toFloat(),
                            y = 0.5f,
                            z = (room.centerZ + 1).toFloat(),
                            yaw = 180f,
                            hp = 350 + district.level * 50,
                            armor = 200 + district.level * 30,
                            isBoss = true,
                            bossPhase = 1,
                            bossShieldActive = true,
                            patrolPoints = listOf(
                                Pair(room.centerX.toFloat(), (room.centerZ + 1).toFloat()),
                                Pair((room.centerX - 1).toFloat(), (room.centerZ - 1).toFloat()),
                                Pair((room.centerX + 1).toFloat(), (room.centerZ - 1).toFloat())
                            )
                        )
                    )

                    // Ceiling Defense Turret
                    enemies.add(
                        Enemy(
                            id = enemyIdCounter++,
                            type = EnemyType.CEILING_TURRET,
                            x = (room.x + 1).toFloat(),
                            y = 2.4f,
                            z = (room.z + 1).toFloat(),
                            yaw = 45f,
                            hp = 120,
                            armor = 50
                        )
                    )
                }

                DistrictRoomType.SECURITY_COMMAND_HUB -> {
                    // Blue Keycard Door
                    val doorX = room.x + room.width / 2
                    val doorZ = room.z
                    grid[doorX][doorZ] = TileType.DOOR_LOCKED_BLUE
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, ItemType.KEYCARD_BLUE, isOpen = false, isLocked = true))

                    // Security override terminal
                    grid[room.centerX][room.centerZ] = TileType.TERMINAL
                    terminals.add(
                        TerminalObject(
                            id = terminalIdCounter++,
                            x = room.centerX.toFloat(),
                            z = room.centerZ.toFloat(),
                            sectorName = "Sector Sec-Override [L${district.level}]",
                            securityIceLevel = min(5, 1 + district.level)
                        )
                    )

                    // High-value Keycard Red stored in security terminal safe
                    if (!keycardRedPlaced) {
                        lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 1).toFloat(), ItemType.KEYCARD_RED, 1))
                        keycardRedPlaced = true
                    }

                    // Security Guard patrolling hub
                    enemies.add(
                        Enemy(
                            id = enemyIdCounter++,
                            type = if (district.level >= 3) EnemyType.HEAVY_ENFORCER else EnemyType.SECURITY_GUARD,
                            x = (room.centerX).toFloat(),
                            y = 0.5f,
                            z = (room.centerZ + 1).toFloat(),
                            yaw = rng.nextFloat() * 360f,
                            hp = 80 + district.level * 15,
                            armor = 20 + district.level * 10,
                            patrolPoints = listOf(
                                Pair((room.x + 1).toFloat(), (room.z + 1).toFloat()),
                                Pair((room.x + room.width - 2).toFloat(), (room.z + room.height - 2).toFloat())
                            )
                        )
                    )
                }

                DistrictRoomType.ARMORY_SUPPLY_CACHE -> {
                    // Closed Door
                    val doorX = room.centerX
                    val doorZ = room.z
                    grid[doorX][doorZ] = TileType.DOOR_CLOSED
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, null, isOpen = false, isLocked = false))

                    // Armory Cache: Blue Keycard, Armor, Stim
                    if (!keycardBluePlaced) {
                        lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 1).toFloat(), ItemType.KEYCARD_BLUE, 1))
                        keycardBluePlaced = true
                    }
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + room.width - 2).toFloat(), (room.z + 1).toFloat(), ItemType.ARMOR_PLATE, 2))
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + room.height - 2).toFloat(), ItemType.EMP_CELL, 2))
                }

                DistrictRoomType.SERVER_MAINFRAME_VAULT -> {
                    // Glass partition walls and ICE Terminal
                    val doorX = room.centerX
                    val doorZ = room.z + room.height - 1
                    grid[doorX][doorZ] = TileType.DOOR_CLOSED
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, null, isOpen = false, isLocked = false))

                    grid[room.centerX][room.centerZ] = TileType.TERMINAL
                    terminals.add(
                        TerminalObject(
                            id = terminalIdCounter++,
                            x = room.centerX.toFloat(),
                            z = room.centerZ.toFloat(),
                            sectorName = "Sub-Net Node [${room.id}]",
                            securityIceLevel = min(5, 2 + district.level / 2)
                        )
                    )

                    // Evidence Slate & Contract
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 1).toFloat(), ItemType.EVIDENCE_SLATE, 1))
                    if (rng.nextBoolean()) {
                        lootChests.add(LootChestObject(lootIdCounter++, (room.x + room.width - 2).toFloat(), (room.z + room.height - 2).toFloat(), ItemType.FACTION_CONTRACT, 1))
                    }

                    // Security Camera
                    enemies.add(
                        Enemy(
                            id = enemyIdCounter++,
                            type = EnemyType.SURVEILLANCE_CAMERA,
                            x = (room.x + 1).toFloat(),
                            y = 2.2f,
                            z = (room.z + 1).toFloat(),
                            yaw = 135f,
                            hp = 30,
                            armor = 0
                        )
                    )
                }

                DistrictRoomType.CYBERNETICS_LAB -> {
                    // Glass observation wall on one side
                    for (gx in room.x + 1 until room.x + room.width - 1) {
                        if (gx % 2 == 0) {
                            grid[gx][room.z] = TileType.WALL_GLASS
                            heights[gx][room.z] = 2.2f
                            colors[gx][room.z] = palette.wallGlassColor
                        }
                    }
                    val doorX = room.x
                    val doorZ = room.centerZ
                    grid[doorX][doorZ] = TileType.DOOR_CLOSED
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, null, isOpen = false, isLocked = false))

                    lootChests.add(LootChestObject(lootIdCounter++, (room.centerX).toFloat(), (room.centerZ).toFloat(), ItemType.HEALTH_STIM, 2))
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 1).toFloat(), ItemType.EVIDENCE_SLATE, 1))

                    // Patrolling Drone
                    enemies.add(
                        Enemy(
                            id = enemyIdCounter++,
                            type = EnemyType.SECURITY_DRONE,
                            x = (room.centerX).toFloat(),
                            y = 1.2f,
                            z = (room.centerZ).toFloat(),
                            yaw = rng.nextFloat() * 360f,
                            hp = 45,
                            armor = 0,
                            patrolPoints = listOf(
                                Pair(room.centerX.toFloat(), (room.z + 1).toFloat()),
                                Pair(room.centerX.toFloat(), (room.z + room.height - 2).toFloat())
                            )
                        )
                    )
                }

                DistrictRoomType.EXECUTIVE_OFFICE -> {
                    // Corporate panelling & Faction Contracts
                    val doorX = room.centerX
                    val doorZ = room.z
                    grid[doorX][doorZ] = TileType.DOOR_CLOSED
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, null, isOpen = false, isLocked = false))

                    lootChests.add(LootChestObject(lootIdCounter++, (room.centerX).toFloat(), (room.centerZ).toFloat(), ItemType.FACTION_CONTRACT, 1))
                    lootChests.add(LootChestObject(lootIdCounter++, (room.x + 1).toFloat(), (room.z + 1).toFloat(), ItemType.ARMOR_PLATE, 1))

                    // Heavy Enforcer
                    enemies.add(
                        Enemy(
                            id = enemyIdCounter++,
                            type = EnemyType.HEAVY_ENFORCER,
                            x = (room.centerX).toFloat(),
                            y = 0.5f,
                            z = (room.centerZ).toFloat(),
                            yaw = rng.nextFloat() * 360f,
                            hp = 130,
                            armor = 80
                        )
                    )
                }

                DistrictRoomType.PATROL_PLAZA, DistrictRoomType.MAINTENANCE_VENT_GRID -> {
                    // Open doorway
                    val doorX = room.centerX
                    val doorZ = room.z
                    grid[doorX][doorZ] = TileType.DOOR_OPEN
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, null, isOpen = true, isLocked = false))

                    // Laser Tripwire obstacle in plaza
                    if (rng.nextFloat() < 0.5f) {
                        grid[room.centerX][room.centerZ] = TileType.LASER_TRIPWIRE
                    }

                    // Security Guard patrol
                    if (rng.nextFloat() < 0.65f) {
                        enemies.add(
                            Enemy(
                                id = enemyIdCounter++,
                                type = EnemyType.SECURITY_GUARD,
                                x = (room.centerX).toFloat(),
                                y = 0.5f,
                                z = (room.centerZ).toFloat(),
                                yaw = rng.nextFloat() * 360f,
                                hp = 60,
                                armor = 0,
                                patrolPoints = listOf(
                                    Pair((room.x + 1).toFloat(), (room.z + 1).toFloat()),
                                    Pair((room.x + room.width - 2).toFloat(), (room.z + room.height - 2).toFloat())
                                )
                            )
                        )
                    }
                }

                DistrictRoomType.EXTRACTION_ROOFTOP -> {
                    // Rooftop landing zone
                    val doorX = room.x
                    val doorZ = room.centerZ
                    grid[doorX][doorZ] = TileType.DOOR_OPEN
                    doors.add(DoorObject(doorIdCounter++, doorX, doorZ, null, isOpen = true, isLocked = false))
                }
            }
        }

        // Guaranteed fallback if Blue / Red keycards weren't placed in specific room types
        if (!keycardBluePlaced) {
            val fallbackRoom = rooms.find { it.type != DistrictRoomType.SAFEHOUSE_INSERTION && it.type != DistrictRoomType.MEGASPIRE_SANCTUM } ?: rooms[1]
            lootChests.add(LootChestObject(lootIdCounter++, (fallbackRoom.x + 1).toFloat(), (fallbackRoom.z + 1).toFloat(), ItemType.KEYCARD_BLUE, 1))
        }
        if (!keycardRedPlaced) {
            val fallbackRoom = rooms.find { it.type != DistrictRoomType.SAFEHOUSE_INSERTION && it.type != DistrictRoomType.MEGASPIRE_SANCTUM } ?: rooms[2]
            lootChests.add(LootChestObject(lootIdCounter++, (fallbackRoom.x + fallbackRoom.width - 2).toFloat(), (fallbackRoom.z + fallbackRoom.height - 2).toFloat(), ItemType.KEYCARD_RED, 1))
        }

        // Guaranteed Extraction Zone Tile on rooftop
        val extractionX = extractionRoom.centerX.toFloat()
        val extractionZ = extractionRoom.centerZ.toFloat()
        grid[extractionX.toInt()][extractionZ.toInt()] = TileType.EXTRACTION_ZONE

        val spawnX = safehouse.centerX.toFloat()
        val spawnZ = safehouse.centerZ.toFloat()
        val spireX = spireSanctum.centerX.toFloat()
        val spireZ = spireSanctum.centerZ.toFloat()

        var evidenceCount = 0
        var contractsCount = 0
        lootChests.forEach {
            if (it.itemType == ItemType.EVIDENCE_SLATE) evidenceCount++
            if (it.itemType == ItemType.FACTION_CONTRACT) contractsCount++
        }

        return GeneratedWorld(
            district = district,
            seed = finalSeed,
            width = width,
            height = height,
            grid = grid,
            heights = heights,
            colors = colors,
            spawnX = spawnX,
            spawnZ = spawnZ,
            extractionX = extractionX,
            extractionZ = extractionZ,
            spireCoreX = spireX,
            spireCoreZ = spireZ,
            enemies = enemies,
            terminals = terminals,
            lootChests = lootChests,
            doors = doors,
            evidenceCount = max(1, evidenceCount),
            contractsCount = max(1, contractsCount),
            rooms = rooms
        )
    }

    private fun carveRoom(
        room: DistrictRoom,
        grid: Array<Array<TileType>>,
        heights: Array<FloatArray>,
        colors: Array<IntArray>,
        palette: DistrictThemePalette,
        rng: Random
    ) {
        val wallHeight = when (room.type) {
            DistrictRoomType.MEGASPIRE_SANCTUM -> 4.5f
            DistrictRoomType.EXECUTIVE_OFFICE -> 3.2f
            DistrictRoomType.SECURITY_COMMAND_HUB -> 3.0f
            DistrictRoomType.SERVER_MAINFRAME_VAULT -> 2.8f
            else -> 2.4f
        }

        val wallColor = when (room.type) {
            DistrictRoomType.MEGASPIRE_SANCTUM -> palette.wallGlassColor
            DistrictRoomType.EXECUTIVE_OFFICE -> palette.wallCorpColor
            DistrictRoomType.SERVER_MAINFRAME_VAULT -> palette.wallCorpColor
            else -> palette.wallConcreteColor
        }

        for (x in room.x until room.x + room.width) {
            for (z in room.z until room.z + room.height) {
                if (x !in 0 until grid.size || z !in 0 until grid[0].size) continue

                val isPerimeter = (x == room.x || x == room.x + room.width - 1 || z == room.z || z == room.z + room.height - 1)
                if (isPerimeter) {
                    // Outer room wall
                    grid[x][z] = if (room.type == DistrictRoomType.EXECUTIVE_OFFICE && (x + z) % 3 == 0) {
                        TileType.WALL_CORP_PANEL
                    } else {
                        TileType.WALL_CONCRETE
                    }
                    heights[x][z] = wallHeight
                    colors[x][z] = wallColor
                } else {
                    // Inner floor
                    grid[x][z] = TileType.FLOOR
                    heights[x][z] = 0.0f
                    colors[x][z] = palette.floorColor
                }
            }
        }
    }

    private fun carveCorridor(
        roomA: DistrictRoom,
        roomB: DistrictRoom,
        grid: Array<Array<TileType>>,
        heights: Array<FloatArray>,
        colors: Array<IntArray>,
        palette: DistrictThemePalette,
        rng: Random
    ) {
        var curX = roomA.centerX
        var curZ = roomA.centerZ
        val targetX = roomB.centerX
        val targetZ = roomB.centerZ

        // Choose L-shape order (horizontal then vertical, or vertical then horizontal)
        val horizontalFirst = rng.nextBoolean()

        if (horizontalFirst) {
            // Horizontal line
            while (curX != targetX) {
                carveCorridorTile(curX, curZ, grid, heights, colors, palette)
                curX += if (targetX > curX) 1 else -1
            }
            // Vertical line
            while (curZ != targetZ) {
                carveCorridorTile(curX, curZ, grid, heights, colors, palette)
                curZ += if (targetZ > curZ) 1 else -1
            }
        } else {
            // Vertical line
            while (curZ != targetZ) {
                carveCorridorTile(curX, curZ, grid, heights, colors, palette)
                curZ += if (targetZ > curZ) 1 else -1
            }
            // Horizontal line
            while (curX != targetX) {
                carveCorridorTile(curX, curZ, grid, heights, colors, palette)
                curX += if (targetX > curX) 1 else -1
            }
        }
    }

    private fun carveCorridorTile(
        x: Int,
        z: Int,
        grid: Array<Array<TileType>>,
        heights: Array<FloatArray>,
        colors: Array<IntArray>,
        palette: DistrictThemePalette
    ) {
        if (x in 1 until grid.size - 1 && z in 1 until grid[0].size - 1) {
            if (grid[x][z] == TileType.WALL_CONCRETE || grid[x][z] == TileType.WALL_CORP_PANEL) {
                grid[x][z] = TileType.FLOOR
                heights[x][z] = 0.0f
                colors[x][z] = palette.floorColor
            }
        }
    }

    private fun carveVentilationNetwork(
        rooms: List<DistrictRoom>,
        grid: Array<Array<TileType>>,
        heights: Array<FloatArray>,
        colors: Array<IntArray>,
        palette: DistrictThemePalette,
        rng: Random
    ) {
        // Find adjacent pairs of rooms and carve parallel crawlspace vents
        for (i in 0 until rooms.size - 1) {
            val rA = rooms[i]
            val rB = rooms[i + 1]

            if (rA.type == DistrictRoomType.MAINTENANCE_VENT_GRID || rB.type == DistrictRoomType.MAINTENANCE_VENT_GRID || rng.nextFloat() < 0.4f) {
                var vx = rA.x + 1
                var vz = rA.z + 1
                val tvx = rB.x + 1
                val tvz = rB.z + 1

                while (vx != tvx) {
                    if (vx in 1 until grid.size - 1 && vz in 1 until grid[0].size - 1) {
                        if (grid[vx][vz] == TileType.WALL_CONCRETE) {
                            grid[vx][vz] = TileType.VENT_SHAFT
                            heights[vx][vz] = 0.0f
                            colors[vx][vz] = palette.ventColor
                        }
                    }
                    vx += if (tvx > vx) 1 else -1
                }
                while (vz != tvz) {
                    if (vx in 1 until grid.size - 1 && vz in 1 until grid[0].size - 1) {
                        if (grid[vx][vz] == TileType.WALL_CONCRETE) {
                            grid[vx][vz] = TileType.VENT_SHAFT
                            heights[vx][vz] = 0.0f
                            colors[vx][vz] = palette.ventColor
                        }
                    }
                    vz += if (tvz > vz) 1 else -1
                }
            }
        }
    }

    private fun distanceSq(x1: Int, z1: Int, x2: Int, z2: Int): Double {
        val dx = (x1 - x2).toDouble()
        val dz = (z1 - z2).toDouble()
        return dx * dx + dz * dz
    }
}
