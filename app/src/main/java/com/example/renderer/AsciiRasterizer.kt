package com.example.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.engine.GeneratedWorld
import com.example.engine.MathUtils
import com.example.model.*
import kotlin.math.*

class AsciiRasterizer(
    var cols: Int = 80,
    var rows: Int = 45
) {
    var ramp: String = AsciiRamps.CYBER
    var colorMode: String = "GAME"
    var filterMode: String = "BOX"

    // Zero-allocation frame buffer arrays for CPU-GPU pipeline
    var glyphBuffer = CharArray(cols * rows)
        private set
    var colorBuffer = IntArray(cols * rows)
        private set
    var bgColorBuffer = IntArray(cols * rows)
        private set
    var depthBuffer = FloatArray(cols)
        private set

    // Raycast constants
    private val fov = 75f * MathUtils.DEG_TO_RAD
    private val maxRayDist = 24.0f

    // Rendering paint
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = 18f
        isFakeBoldText = true
    }
    private val bgPaint = Paint().apply {
        color = 0xFF050811.toInt()
        style = Paint.Style.FILL
    }
    private val fxPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val singleCharBuffer = CharArray(1)

    // Rain particles
    private val maxRainDrops = 60
    private val rainX = FloatArray(maxRainDrops)
    private val rainY = FloatArray(maxRainDrops)
    private val rainSpeed = FloatArray(maxRainDrops)

    init {
        val rng = java.util.Random(42)
        for (i in 0 until maxRainDrops) {
            rainX[i] = rng.nextFloat()
            rainY[i] = rng.nextFloat()
            rainSpeed[i] = 1.2f + rng.nextFloat() * 1.8f
        }
    }

    fun resize(newCols: Int, newRows: Int) {
        if (newCols != cols || newRows != rows) {
            cols = newCols.coerceIn(40, 140)
            rows = newRows.coerceIn(24, 80)
            glyphBuffer = CharArray(cols * rows)
            colorBuffer = IntArray(cols * rows)
            bgColorBuffer = IntArray(cols * rows)
            depthBuffer = FloatArray(cols)
        }
    }

    fun rasterizeScene(
        world: GeneratedWorld,
        playerX: Float,
        playerY: Float,
        playerZ: Float,
        playerYaw: Float,
        playerPitch: Float,
        isThermalVision: Boolean,
        isCloaked: Boolean,
        muzzleFlashAlpha: Float,
        damageFlashAlpha: Float,
        deltaSec: Float
    ) {
        val halfFov = fov * 0.5f
        val radYaw = playerYaw * MathUtils.DEG_TO_RAD

        // 1. Raycast Wall Columns
        for (c in 0 until cols) {
            val rayAngle = radYaw - halfFov + (c.toFloat() / cols.toFloat()) * fov
            val rayDirX = sin(rayAngle)
            val rayDirZ = cos(rayAngle)

            // DDA Raycasting
            var mapX = playerX.toInt()
            var mapZ = playerZ.toInt()

            val deltaDistX = if (abs(rayDirX) < 1e-6f) 1e30f else abs(1.0f / rayDirX)
            val deltaDistZ = if (abs(rayDirZ) < 1e-6f) 1e30f else abs(1.0f / rayDirZ)

            var stepX: Int
            var sideDistX: Float
            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (playerX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0f - playerX) * deltaDistX
            }

            var stepZ: Int
            var sideDistZ: Float
            if (rayDirZ < 0) {
                stepZ = -1
                sideDistZ = (playerZ - mapZ) * deltaDistZ
            } else {
                stepZ = 1
                sideDistZ = (mapZ + 1.0f - playerZ) * deltaDistZ
            }

            var hit = false
            var side = 0 // 0 for X wall, 1 for Z wall
            var hitTile = TileType.EMPTY
            var hitColor = 0xFF00FFCC.toInt()
            var hitHeight = 1.0f
            var distance = 0.0f

            while (!hit && distance < maxRayDist) {
                if (sideDistX < sideDistZ) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistZ += deltaDistZ
                    mapZ += stepZ
                    side = 1
                }

                if (mapX in 0 until world.width && mapZ in 0 until world.height) {
                    val tile = world.grid[mapX][mapZ]
                    if (tile.isSolid) {
                        hit = true
                        hitTile = tile
                        hitColor = world.colors[mapX][mapZ]
                        hitHeight = world.heights[mapX][mapZ]
                    }
                } else {
                    hit = true
                    hitTile = TileType.WALL_CONCRETE
                    hitColor = 0xFF1A1F2C.toInt()
                    hitHeight = 2.0f
                }
            }

            distance = if (side == 0) {
                (mapX - playerX + (1 - stepX) / 2) / rayDirX
            } else {
                (mapZ - playerZ + (1 - stepZ) / 2) / rayDirZ
            }
            // Fish-eye correction
            val correctedDist = distance * cos(rayAngle - radYaw)
            depthBuffer[c] = max(0.2f, correctedDist)

            // Calculate Projected Wall Height
            val wallScreenHeight = (rows.toFloat() / (correctedDist * 0.9f)) * hitHeight
            val pitchOffset = (playerPitch / 45.0f) * (rows * 0.4f)
            val horizon = (rows / 2.0f) + pitchOffset

            val wallTop = (horizon - wallScreenHeight * 0.5f).toInt().coerceIn(0, rows)
            val wallBottom = (horizon + wallScreenHeight * 0.5f).toInt().coerceIn(0, rows)

            // Render Column Cells
            for (r in 0 until rows) {
                val idx = r * cols + c
                if (r < wallTop) {
                    // Ceiling / Cyber Sky
                    val ceilDist = (horizon - r) / (rows * 0.5f)
                    glyphBuffer[idx] = if (ceilDist > 0.6f && (c + r) % 8 == 0) '*' else ' '
                    colorBuffer[idx] = AnsiPalette.quantizeColor(0xFF0D1B2A.toInt(), colorMode)
                    bgColorBuffer[idx] = 0xFF050811.toInt()
                } else if (r in wallTop until wallBottom) {
                    // Wall
                    val normalChar = if (side == 0) '|' else '-'
                    val depthFactor = (1.0f - (correctedDist / maxRayDist)).coerceIn(0.1f, 1.0f)
                    val lightMod = if (side == 1) 0.85f else 1.0f

                    val glyphChar = when (hitTile) {
                        TileType.DOOR_CLOSED, TileType.DOOR_LOCKED_BLUE, TileType.DOOR_LOCKED_RED -> '+'
                        TileType.DOOR_OPEN -> '/'
                        TileType.TERMINAL -> '$'
                        TileType.GHOST_INDEX_MAINFRAME -> '@'
                        TileType.CHEST_LOOT -> '8'
                        TileType.LASER_TRIPWIRE -> '!'
                        else -> {
                            if (filterMode == "CINEMATIC" && (r == wallTop || r == wallBottom - 1)) {
                                normalChar
                            } else {
                                AsciiRamps.sampleGlyph(ramp, depthFactor * lightMod)
                            }
                        }
                    }
                    glyphBuffer[idx] = glyphChar

                    var finalColor = if (isThermalVision) {
                        0xFF00E5FF.toInt()
                    } else {
                        hitColor
                    }

                    // Background tint for special interactive elements
                    val bgTint = when (hitTile) {
                        TileType.DOOR_LOCKED_RED -> 0x44FF0033.toInt()
                        TileType.DOOR_LOCKED_BLUE -> 0x440077FF.toInt()
                        TileType.TERMINAL -> 0x3300FF99.toInt()
                        TileType.GHOST_INDEX_MAINFRAME -> 0x55FF0055.toInt()
                        TileType.LASER_TRIPWIRE -> 0x44FF3300.toInt()
                        else -> 0x00000000
                    }
                    bgColorBuffer[idx] = bgTint

                    // Apply depth darkening
                    val rCol = ((((finalColor ushr 16) and 0xFF) * depthFactor * lightMod).toInt()).coerceIn(0, 255)
                    val gCol = ((((finalColor ushr 8) and 0xFF) * depthFactor * lightMod).toInt()).coerceIn(0, 255)
                    val bCol = (((finalColor and 0xFF) * depthFactor * lightMod).toInt()).coerceIn(0, 255)
                    finalColor = (0xFF shl 24) or (rCol shl 16) or (gCol shl 8) or bCol

                    colorBuffer[idx] = AnsiPalette.quantizeColor(finalColor, colorMode)
                } else {
                    // Floor
                    val floorDist = (r - horizon) / (rows * 0.5f)
                    val floorIntensity = (0.25f / (floorDist + 0.1f)).coerceIn(0.05f, 0.6f)
                    val floorChar = if ((c + r) % 4 == 0) '.' else ' '
                    glyphBuffer[idx] = floorChar
                    val fCol = (((0x1A * floorIntensity).toInt()) shl 16) or
                            (((0x35 * floorIntensity).toInt()) shl 8) or
                            ((0x44 * floorIntensity).toInt())
                    colorBuffer[idx] = AnsiPalette.quantizeColor((0xFF shl 24) or fCol, colorMode)
                    bgColorBuffer[idx] = 0xFF050811.toInt()
                }
            }
        }

        // 2. Sprite Rasterization: Enemies & Boss
        val visibleEnemies = world.enemies.filter { it.hp > 0 || it.state == EnemyAiState.UNCONSCIOUS }
            .sortedByDescending { MathUtils.distance2D(playerX, playerZ, it.x, it.z) }

        for (enemy in visibleEnemies) {
            val ex = enemy.x - playerX
            val ez = enemy.z - playerZ

            // Rotate relative to player view
            val invDet = 1.0f // normalized
            val transformX = cos(radYaw) * ex - sin(radYaw) * ez
            val transformY = sin(radYaw) * ex + cos(radYaw) * ez

            if (transformY > 0.3f && transformY < maxRayDist) {
                val enemyScreenX = ((cols * 0.5f) * (1.0f + transformX / transformY)).toInt()
                val spriteHeight = (rows.toFloat() / transformY) * (if (enemy.isBoss) 2.2f else 1.2f)
                val pitchOffset = (playerPitch / 45.0f) * (rows * 0.4f)
                val enemyScreenY = ((rows * 0.5f) + pitchOffset).toInt()

                val drawStartY = (enemyScreenY - spriteHeight * 0.5f).toInt().coerceIn(0, rows - 1)
                val drawEndY = (enemyScreenY + spriteHeight * 0.5f).toInt().coerceIn(0, rows - 1)
                val spriteWidth = (spriteHeight * 0.6f).toInt().coerceAtLeast(1)
                val drawStartX = (enemyScreenX - spriteWidth / 2).coerceIn(0, cols - 1)
                val drawEndX = (enemyScreenX + spriteWidth / 2).coerceIn(0, cols - 1)

                val enemyGlyph = when (enemy.type) {
                    EnemyType.AEGIS_PALADIN -> 'Ω'
                    EnemyType.HEAVY_ENFORCER -> 'E'
                    EnemyType.SECURITY_DRONE -> '^'
                    EnemyType.CEILING_TURRET -> 'T'
                    EnemyType.SURVEILLANCE_CAMERA -> 'C'
                    else -> 'G'
                }

                val baseEnemyColor = when {
                    isThermalVision -> 0xFFFF2A00.toInt() // Heat vision highlight
                    enemy.state == EnemyAiState.COMBAT -> 0xFFFF0055.toInt()
                    enemy.state == EnemyAiState.ALERT || enemy.state == EnemyAiState.INVESTIGATING -> 0xFFFFB800.toInt()
                    enemy.state == EnemyAiState.UNCONSCIOUS -> 0xFF4A6572.toInt()
                    enemy.isBoss -> 0xFFFF0033.toInt()
                    else -> 0xFF00FF66.toInt()
                }

                for (sx in drawStartX..drawEndX) {
                    if (sx in 0 until cols && transformY < depthBuffer[sx]) {
                        for (sy in drawStartY..drawEndY) {
                            val idx = sy * cols + sx
                            glyphBuffer[idx] = enemyGlyph
                            colorBuffer[idx] = AnsiPalette.quantizeColor(baseEnemyColor, colorMode)
                        }
                    }
                }
            }
        }

        // 3. Update Rain Particles
        for (i in 0 until maxRainDrops) {
            rainY[i] += rainSpeed[i] * deltaSec
            if (rainY[i] > 1.0f) {
                rainY[i] = 0f
            }
        }
    }

    fun drawToCanvas(
        canvas: Canvas,
        screenWidth: Float,
        screenHeight: Float,
        muzzleFlashAlpha: Float,
        damageAlpha: Float,
        isThermalVision: Boolean,
        isCloaked: Boolean
    ) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        val cellWidth = screenWidth / cols.toFloat()
        val cellHeight = screenHeight / rows.toFloat()
        textPaint.textSize = cellHeight * 1.15f

        // Draw Background Tints and Glyphs
        for (r in 0 until rows) {
            val cellTop = r * cellHeight
            val yPos = (r + 1) * cellHeight - (cellHeight * 0.2f)
            for (c in 0 until cols) {
                val idx = r * cols + c
                val cellLeft = c * cellWidth

                val bgCol = bgColorBuffer[idx]
                if (bgCol != 0 && bgCol != 0xFF050811.toInt()) {
                    fxPaint.color = bgCol
                    fxPaint.style = Paint.Style.FILL
                    canvas.drawRect(cellLeft, cellTop, cellLeft + cellWidth, cellTop + cellHeight, fxPaint)
                }

                val ch = glyphBuffer[idx]
                if (ch != ' ') {
                    textPaint.color = colorBuffer[idx]
                    singleCharBuffer[0] = ch
                    canvas.drawText(singleCharBuffer, 0, 1, cellLeft, yPos, textPaint)
                }
            }
        }

        // Draw FX: Cyber Rain
        fxPaint.color = 0x4400F0FF.toInt()
        fxPaint.strokeWidth = 1.5f
        for (i in 0 until maxRainDrops) {
            val rx = rainX[i] * screenWidth
            val ry = rainY[i] * screenHeight
            canvas.drawLine(rx, ry, rx - 3f, ry + 12f, fxPaint)
        }

        // Draw FX: Thermal Scanlines / Cyber Vignette
        if (isThermalVision) {
            fxPaint.color = 0x2200E5FF.toInt()
            fxPaint.style = Paint.Style.STROKE
            fxPaint.strokeWidth = 1.0f
            var ly = 0f
            while (ly < screenHeight) {
                canvas.drawLine(0f, ly, screenWidth, ly, fxPaint)
                ly += 8f
            }
        }

        // Draw FX: Muzzle Flash Light Pulse
        if (muzzleFlashAlpha > 0.05f) {
            val flashColor = ((muzzleFlashAlpha * 180).toInt().coerceIn(0, 255) shl 24) or 0x00FFF275
            fxPaint.color = flashColor
            fxPaint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, screenWidth, screenHeight, fxPaint)
        }

        // Draw FX: Damage Red Vignette
        if (damageAlpha > 0.05f) {
            val dmgColor = ((damageAlpha * 190).toInt().coerceIn(0, 255) shl 24) or 0x00FF1744
            fxPaint.color = dmgColor
            fxPaint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, screenWidth, screenHeight, fxPaint)
        }

        // Draw FX: Ghost Camo Shimmer
        if (isCloaked) {
            fxPaint.color = 0x3300FF99.toInt()
            fxPaint.style = Paint.Style.STROKE
            fxPaint.strokeWidth = 4f
            canvas.drawRect(4f, 4f, screenWidth - 4f, screenHeight - 4f, fxPaint)
        }
    }
}
