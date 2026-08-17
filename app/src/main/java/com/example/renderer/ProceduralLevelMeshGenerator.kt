package com.example.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.Random

/**
 * Level entity metadata extracted from ASCII layout symbols.
 */
data class LevelEntityLocation(
    val symbol: Char,
    val typeName: String,
    val gridX: Int,
    val gridZ: Int,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float
)

/**
 * Container holding the generated OpenGL ES 3D vertex buffers for a level layout.
 */
data class LevelMeshBuffers(
    val wallsMesh: Mesh3D,
    val floorsMesh: Mesh3D,
    val interactablesMesh: Mesh3D,
    val combinedMesh: Mesh3D,
    val asciiLayout: List<String>,
    val gridWidth: Int,
    val gridHeight: Int,
    val tileSize: Float,
    val entities: List<LevelEntityLocation>
) {
    fun release() {
        wallsMesh.release()
        floorsMesh.release()
        interactablesMesh.release()
        combinedMesh.release()
    }
}

/**
 * Procedural level mesh generator that produces complete OpenGL ES vertex, normal,
 * texture coordinate, and index buffers from ASCII character layouts.
 *
 * Distinct ASCII Symbol Legend:
 * ==============================
 * Walls:
 *   '#' : Standard Reinforced Concrete Wall
 *   '=' : Heavy Corporate Security Blast Wall
 *   'G' : Holographic / Reinforced Neon Glass Wall
 *   'B' : Server Rack / Mainframe Terminal Bank Wall
 *
 * Floors:
 *   '.' : Standard Steel Tech-Tile Floor
 *   '_' : Illuminated Neon Pathway / Data Conduit Floor
 *   '~' : Drainage Chasm / Coolant Grating (Lowered Trench)
 *   ':' : Elevated Industrial Catwalk / High Platform
 *
 * Interactable Objects & Sector Anchors:
 *   'D' : Electronic Blast Door Gate
 *   'T' : Security Access Terminal Console
 *   'C' : Military Supply / Ammo Loot Cache
 *   'E' : Rooftop Extraction Helipad Beacon
 *   'S' : Operative Insertion Point / Spawn Pod
 *   'P' : Overhead Surveillance Drone Turret Pylon
 *   'L' : High-Voltage Laser Tripwire Grid
 *   ' ' : Empty Void / Unused Chasm
 */
class ProceduralLevelMeshGenerator(
    val tileSize: Float = 2.0f,
    val wallHeight: Float = 2.4f
) {

    companion object {
        const val SYM_WALL_CONCRETE = '#'
        const val SYM_WALL_HEAVY = '='
        const val SYM_WALL_GLASS = 'G'
        const val SYM_WALL_SERVER = 'B'

        const val SYM_FLOOR_STANDARD = '.'
        const val SYM_FLOOR_CONDUIT = '_'
        const val SYM_FLOOR_CHASM = '~'
        const val SYM_FLOOR_CATWALK = ':'

        const val SYM_DOOR = 'D'
        const val SYM_TERMINAL = 'T'
        const val SYM_CACHE = 'C'
        const val SYM_EXTRACTION = 'E'
        const val SYM_SPAWN = 'S'
        const val SYM_TURRET_PYLON = 'P'
        const val SYM_LASER_GRID = 'L'
        const val SYM_EMPTY = ' '
    }

    /**
     * Procedurally generates a randomized ASCII room-and-corridor floor plan.
     */
    fun generateProceduralAsciiLayout(
        width: Int = 24,
        height: Int = 24,
        seed: Long = System.currentTimeMillis()
    ): List<String> {
        val rand = Random(seed)
        val grid = Array(height) { CharArray(width) { SYM_WALL_CONCRETE } }

        data class Room(val x: Int, val z: Int, val w: Int, val h: Int) {
            val centerX = x + w / 2
            val centerZ = z + h / 2
            fun intersects(o: Room, pad: Int = 1): Boolean {
                return x - pad < o.x + o.w && x + w + pad > o.x &&
                        z - pad < o.z + o.h && z + h + pad > o.z
            }
        }

        val rooms = mutableListOf<Room>()
        val roomCount = rand.nextInt(4) + 5

        for (attempt in 0 until 40) {
            if (rooms.size >= roomCount) break
            val rw = rand.nextInt(4) + 4
            val rh = rand.nextInt(4) + 4
            val rx = rand.nextInt(width - rw - 2) + 1
            val rz = rand.nextInt(height - rh - 2) + 1
            val newRoom = Room(rx, rz, rw, rh)

            if (rooms.none { it.intersects(newRoom) }) {
                rooms.add(newRoom)
                // Carve room floors
                for (z in rz until rz + rh) {
                    for (x in rx until rx + rw) {
                        val isBorder = x == rx || x == rx + rw - 1 || z == rz || z == rz + rh - 1
                        if (isBorder && rand.nextFloat() < 0.25f) {
                            grid[z][x] = SYM_WALL_SERVER
                        } else if ((x + z) % 3 == 0) {
                            grid[z][x] = SYM_FLOOR_CONDUIT
                        } else {
                            grid[z][x] = SYM_FLOOR_STANDARD
                        }
                    }
                }
            }
        }

        // Carve corridors connecting rooms
        for (i in 0 until rooms.size - 1) {
            val rA = rooms[i]
            val rB = rooms[i + 1]
            var cx = rA.centerX
            var cz = rA.centerZ

            while (cx != rB.centerX) {
                grid[cz][cx] = if (rand.nextFloat() < 0.15f) SYM_FLOOR_CATWALK else SYM_FLOOR_STANDARD
                cx += if (rB.centerX > cx) 1 else -1
            }
            while (cz != rB.centerZ) {
                grid[cz][cx] = if (rand.nextFloat() < 0.15f) SYM_FLOOR_CATWALK else SYM_FLOOR_STANDARD
                cz += if (rB.centerZ > cz) 1 else -1
            }
        }

        // Place Spawn point in first room
        if (rooms.isNotEmpty()) {
            val sRoom = rooms.first()
            grid[sRoom.centerZ][sRoom.centerX] = SYM_SPAWN

            // Place Extraction point in last room
            val eRoom = rooms.last()
            grid[eRoom.centerZ][eRoom.centerX] = SYM_EXTRACTION
        }

        // Place Doors, Terminals, Loot Caches, and Hazards
        for (room in rooms) {
            if (rand.nextFloat() < 0.7f && room != rooms.first()) {
                val tx = (room.x + 1).coerceAtMost(room.x + room.w - 2)
                val tz = (room.z + 1).coerceAtMost(room.z + room.h - 2)
                grid[tz][tx] = SYM_TERMINAL
            }
            if (rand.nextFloat() < 0.6f) {
                val cx = (room.x + room.w - 2).coerceAtLeast(room.x + 1)
                val cz = (room.z + room.h - 2).coerceAtLeast(room.z + 1)
                grid[cz][cx] = SYM_CACHE
            }
            if (rand.nextFloat() < 0.4f) {
                val px = room.centerX
                val pz = room.z + 1
                if (grid[pz][px] == SYM_FLOOR_STANDARD || grid[pz][px] == SYM_FLOOR_CONDUIT) {
                    grid[pz][px] = SYM_TURRET_PYLON
                }
            }
        }

        // Add some glass walls & heavy blast walls along borders
        for (z in 0 until height) {
            for (x in 0 until width) {
                if (grid[z][x] == SYM_WALL_CONCRETE) {
                    val nFloor = (x > 0 && isFloor(grid[z][x - 1])) ||
                            (x < width - 1 && isFloor(grid[z][x + 1])) ||
                            (z > 0 && isFloor(grid[z - 1][x])) ||
                            (z < height - 1 && isFloor(grid[z + 1][x]))
                    if (nFloor && rand.nextFloat() < 0.15f) {
                        grid[z][x] = SYM_WALL_GLASS
                    } else if (nFloor && rand.nextFloat() < 0.10f) {
                        grid[z][x] = SYM_WALL_HEAVY
                    }
                }
            }
        }

        return grid.map { String(it) }
    }

    private fun isFloor(c: Char): Boolean {
        return c == SYM_FLOOR_STANDARD || c == SYM_FLOOR_CONDUIT ||
                c == SYM_FLOOR_CATWALK || c == SYM_FLOOR_CHASM ||
                c == SYM_SPAWN || c == SYM_EXTRACTION ||
                c == SYM_TERMINAL || c == SYM_CACHE
    }

    /**
     * Parses an ASCII level layout into fully constructed OpenGL 3D vertex buffers.
     */
    fun buildLevelBuffers(asciiLayout: List<String>): LevelMeshBuffers {
        val gridHeight = asciiLayout.size
        val gridWidth = if (gridHeight > 0) asciiLayout.maxOf { it.length } else 0

        val wallVerts = mutableListOf<Float>()
        val wallNorms = mutableListOf<Float>()
        val wallUvs = mutableListOf<Float>()
        val wallIndices = mutableListOf<Short>()

        val floorVerts = mutableListOf<Float>()
        val floorNorms = mutableListOf<Float>()
        val floorUvs = mutableListOf<Float>()
        val floorIndices = mutableListOf<Short>()

        val itemVerts = mutableListOf<Float>()
        val itemNorms = mutableListOf<Float>()
        val itemUvs = mutableListOf<Float>()
        val itemIndices = mutableListOf<Short>()

        val combVerts = mutableListOf<Float>()
        val combNorms = mutableListOf<Float>()
        val combUvs = mutableListOf<Float>()
        val combIndices = mutableListOf<Short>()

        val entities = mutableListOf<LevelEntityLocation>()

        val halfW = (gridWidth * tileSize) * 0.5f
        val halfH = (gridHeight * tileSize) * 0.5f

        for (z in 0 until gridHeight) {
            val line = asciiLayout[z]
            for (x in 0 until line.length) {
                val symbol = line[x]
                if (symbol == SYM_EMPTY) continue

                val worldX = (x * tileSize) - halfW + (tileSize * 0.5f)
                val worldZ = (z * tileSize) - halfH + (tileSize * 0.5f)

                when (symbol) {
                    SYM_WALL_CONCRETE, SYM_WALL_HEAVY, SYM_WALL_GLASS, SYM_WALL_SERVER -> {
                        val h = when (symbol) {
                            SYM_WALL_HEAVY -> wallHeight * 1.25f
                            SYM_WALL_GLASS -> wallHeight * 0.95f
                            SYM_WALL_SERVER -> wallHeight * 1.10f
                            else -> wallHeight
                        }
                        appendBoxGeometry(
                            wallVerts, wallNorms, wallUvs, wallIndices,
                            worldX, h * 0.5f, worldZ,
                            tileSize, h, tileSize
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, h * 0.5f, worldZ,
                            tileSize, h, tileSize
                        )
                    }

                    SYM_FLOOR_STANDARD, SYM_FLOOR_CONDUIT, SYM_FLOOR_CHASM, SYM_FLOOR_CATWALK -> {
                        val (floorY, floorThickness) = when (symbol) {
                            SYM_FLOOR_CATWALK -> Pair(0.4f, 0.12f)
                            SYM_FLOOR_CHASM -> Pair(-0.4f, 0.10f)
                            SYM_FLOOR_CONDUIT -> Pair(0.02f, 0.08f)
                            else -> Pair(0.0f, 0.08f)
                        }
                        appendBoxGeometry(
                            floorVerts, floorNorms, floorUvs, floorIndices,
                            worldX, floorY - (floorThickness * 0.5f), worldZ,
                            tileSize, floorThickness, tileSize
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, floorY - (floorThickness * 0.5f), worldZ,
                            tileSize, floorThickness, tileSize
                        )
                    }

                    SYM_DOOR -> {
                        // Floor base under door
                        appendBoxGeometry(
                            floorVerts, floorNorms, floorUvs, floorIndices,
                            worldX, -0.04f, worldZ, tileSize, 0.08f, tileSize
                        )
                        // Blast Door frame geometry
                        appendBoxGeometry(
                            itemVerts, itemNorms, itemUvs, itemIndices,
                            worldX, wallHeight * 0.45f, worldZ,
                            tileSize * 0.9f, wallHeight * 0.9f, tileSize * 0.25f
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, wallHeight * 0.45f, worldZ,
                            tileSize * 0.9f, wallHeight * 0.9f, tileSize * 0.25f
                        )
                        entities.add(LevelEntityLocation(symbol, "Security Door", x, z, worldX, 0f, worldZ))
                    }

                    SYM_TERMINAL -> {
                        // Floor base
                        appendBoxGeometry(
                            floorVerts, floorNorms, floorUvs, floorIndices,
                            worldX, -0.04f, worldZ, tileSize, 0.08f, tileSize
                        )
                        // Terminal Console Pillar
                        appendBoxGeometry(
                            itemVerts, itemNorms, itemUvs, itemIndices,
                            worldX, 0.6f, worldZ,
                            0.6f, 1.2f, 0.6f
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, 0.6f, worldZ,
                            0.6f, 1.2f, 0.6f
                        )
                        entities.add(LevelEntityLocation(symbol, "Cyber Terminal", x, z, worldX, 0.6f, worldZ))
                    }

                    SYM_CACHE -> {
                        // Floor base
                        appendBoxGeometry(
                            floorVerts, floorNorms, floorUvs, floorIndices,
                            worldX, -0.04f, worldZ, tileSize, 0.08f, tileSize
                        )
                        // Supply Cache Container
                        appendBoxGeometry(
                            itemVerts, itemNorms, itemUvs, itemIndices,
                            worldX, 0.35f, worldZ,
                            0.9f, 0.7f, 0.9f
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, 0.35f, worldZ,
                            0.9f, 0.7f, 0.9f
                        )
                        entities.add(LevelEntityLocation(symbol, "Supply Cache", x, z, worldX, 0.35f, worldZ))
                    }

                    SYM_EXTRACTION -> {
                        // Elevated extraction pad
                        appendBoxGeometry(
                            itemVerts, itemNorms, itemUvs, itemIndices,
                            worldX, 0.1f, worldZ,
                            tileSize * 0.95f, 0.2f, tileSize * 0.95f
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, 0.1f, worldZ,
                            tileSize * 0.95f, 0.2f, tileSize * 0.95f
                        )
                        entities.add(LevelEntityLocation(symbol, "Extraction Helipad", x, z, worldX, 0.1f, worldZ))
                    }

                    SYM_SPAWN -> {
                        // Spawn platform
                        appendBoxGeometry(
                            floorVerts, floorNorms, floorUvs, floorIndices,
                            worldX, 0.05f, worldZ,
                            tileSize * 0.9f, 0.1f, tileSize * 0.9f
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, 0.05f, worldZ,
                            tileSize * 0.9f, 0.1f, tileSize * 0.9f
                        )
                        entities.add(LevelEntityLocation(symbol, "Insertion Point", x, z, worldX, 0.05f, worldZ))
                    }

                    SYM_TURRET_PYLON -> {
                        // Floor base
                        appendBoxGeometry(
                            floorVerts, floorNorms, floorUvs, floorIndices,
                            worldX, -0.04f, worldZ, tileSize, 0.08f, tileSize
                        )
                        // Ceiling Turret Pylon
                        appendBoxGeometry(
                            itemVerts, itemNorms, itemUvs, itemIndices,
                            worldX, wallHeight - 0.4f, worldZ,
                            0.5f, 0.8f, 0.5f
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, wallHeight - 0.4f, worldZ,
                            0.5f, 0.8f, 0.5f
                        )
                        entities.add(LevelEntityLocation(symbol, "Turret Pylon", x, z, worldX, wallHeight - 0.4f, worldZ))
                    }

                    SYM_LASER_GRID -> {
                        // Floor base
                        appendBoxGeometry(
                            floorVerts, floorNorms, floorUvs, floorIndices,
                            worldX, -0.04f, worldZ, tileSize, 0.08f, tileSize
                        )
                        // Glowing laser barrier column
                        appendBoxGeometry(
                            itemVerts, itemNorms, itemUvs, itemIndices,
                            worldX, wallHeight * 0.5f, worldZ,
                            0.2f, wallHeight, 0.2f
                        )
                        appendBoxGeometry(
                            combVerts, combNorms, combUvs, combIndices,
                            worldX, wallHeight * 0.5f, worldZ,
                            0.2f, wallHeight, 0.2f
                        )
                        entities.add(LevelEntityLocation(symbol, "Laser Grid", x, z, worldX, wallHeight * 0.5f, worldZ))
                    }
                }
            }
        }

        return LevelMeshBuffers(
            wallsMesh = buildMesh(wallVerts, wallNorms, wallUvs, wallIndices),
            floorsMesh = buildMesh(floorVerts, floorNorms, floorUvs, floorIndices),
            interactablesMesh = buildMesh(itemVerts, itemNorms, itemUvs, itemIndices),
            combinedMesh = buildMesh(combVerts, combNorms, combUvs, combIndices),
            asciiLayout = asciiLayout,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            tileSize = tileSize,
            entities = entities
        )
    }

    /**
     * Appends a 3D box (6 faces, 24 vertices, 36 indices) with normals and UVs into dynamic vertex lists.
     */
    private fun appendBoxGeometry(
        verts: MutableList<Float>,
        norms: MutableList<Float>,
        uvs: MutableList<Float>,
        indices: MutableList<Short>,
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f

        val startVertex = (verts.size / 3).toShort()

        // 24 Vertex Positions (4 per face x 6 faces)
        val facePositions = floatArrayOf(
            // Front (Z+)
            cx - hx, cy - hy, cz + hz,   cx + hx, cy - hy, cz + hz,   cx + hx, cy + hy, cz + hz,   cx - hx, cy + hy, cz + hz,
            // Back (Z-)
            cx + hx, cy - hy, cz - hz,   cx - hx, cy - hy, cz - hz,   cx - hx, cy + hy, cz - hz,   cx + hx, cy + hy, cz - hz,
            // Top (Y+)
            cx - hx, cy + hy, cz + hz,   cx + hx, cy + hy, cz + hz,   cx + hx, cy + hy, cz - hz,   cx - hx, cy + hy, cz - hz,
            // Bottom (Y-)
            cx - hx, cy - hy, cz - hz,   cx + hx, cy - hy, cz - hz,   cx + hx, cy - hy, cz + hz,   cx - hx, cy - hy, cz + hz,
            // Right (X+)
            cx + hx, cy - hy, cz + hz,   cx + hx, cy - hy, cz - hz,   cx + hx, cy + hy, cz - hz,   cx + hx, cy + hy, cz + hz,
            // Left (X-)
            cx - hx, cy - hy, cz - hz,   cx - hx, cy - hy, cz + hz,   cx - hx, cy + hy, cz + hz,   cx - hx, cy + hy, cz - hz
        )

        // Normals per face
        val faceNormals = floatArrayOf(
            // Front
            0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,
            // Back
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            // Top
            0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
            // Right
            1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,
            // Left
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f
        )

        // UVs
        val faceUvs = floatArrayOf(
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f
        )

        for (v in facePositions) verts.add(v)
        for (n in faceNormals) norms.add(n)
        for (u in faceUvs) uvs.add(u)

        // 36 Indices (2 triangles per face)
        val faceIndexOffsets = shortArrayOf(
            0, 1, 2,  0, 2, 3,
            4, 5, 6,  4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        )

        for (idx in faceIndexOffsets) {
            indices.add((startVertex + idx).toShort())
        }
    }

    private fun buildMesh(
        verts: List<Float>,
        norms: List<Float>,
        uvs: List<Float>,
        indices: List<Short>
    ): Mesh3D {
        val vertArr = verts.toFloatArray()
        val normArr = norms.toFloatArray()
        val uvArr = uvs.toFloatArray()
        val idxArr = indices.toShortArray()

        val vBuf = ByteBuffer.allocateDirect(vertArr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertArr); position(0) }

        val nBuf = ByteBuffer.allocateDirect(normArr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(normArr); position(0) }

        val uBuf = ByteBuffer.allocateDirect(uvArr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(uvArr); position(0) }

        val iBuf = ByteBuffer.allocateDirect(idxArr.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply { put(idxArr); position(0) }

        return Mesh3D(
            vertexBuffer = vBuf,
            normalBuffer = nBuf,
            texCoordBuffer = uBuf,
            indexBuffer = iBuf,
            indexCount = idxArr.size
        )
    }
}
