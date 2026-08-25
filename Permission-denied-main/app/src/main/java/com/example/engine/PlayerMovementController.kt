package com.example.engine

import com.example.model.TileType
import kotlin.math.*

/**
 * Robust 3D Movement and Collision Controller for First-Person Cyberpunk Navigation.
 *
 * Provides:
 * - Direct vector calculation for forward/backward and strafe translation based on camera heading.
 * - Non-inverted first-person view rotation (yaw and pitch orientation).
 * - Multi-input normalization for analog Joystick and WASD / arrow keyboard controls.
 * - Continuous circle-to-AABB collision detection against the ASCII tile grid.
 * - Smooth axis-sliding collision resolution along walls and obstacles to prevent snagging.
 */
class PlayerMovementController(
    val playerRadius: Float = 0.28f,
    val walkSpeed: Float = 3.0f,
    val sprintSpeed: Float = 4.8f,
    val crouchSpeed: Float = 1.5f,
    val minPitch: Float = -45.0f,
    val maxPitch: Float = 45.0f
) {

    /**
     * Normalizes 2D input vectors (e.g. from joystick or diagonal WASD keys)
     * so diagonal speed does not exceed 1.0.
     */
    fun normalizeInput(rawX: Float, rawZ: Float): Pair<Float, Float> {
        val len = sqrt(rawX * rawX + rawZ * rawZ)
        return if (len > 1.0f) {
            Pair(rawX / len, rawZ / len)
        } else {
            Pair(rawX, rawZ)
        }
    }

    /**
     * Computes the world-space translation vector (vx, vz) from local movement inputs
     * (moveX = strafe right/left, moveZ = forward/backward) and camera yaw heading.
     *
     * Heading Coordinate System:
     * - Yaw = 0° looks towards +Z (forwardX = 0, forwardZ = 1)
     * - Yaw = 90° looks towards +X (forwardX = 1, forwardZ = 0)
     * - Strafe Right (+X local) is perpendicular clockwise to forward heading.
     */
    fun computeVelocity(
        moveX: Float,
        moveZ: Float,
        yawDegrees: Float,
        speed: Float,
        deltaSec: Float
    ): Pair<Float, Float> {
        val (normX, normZ) = normalizeInput(moveX, moveZ)
        val radYaw = yawDegrees * MathUtils.DEG_TO_RAD

        val forwardX = sin(radYaw)
        val forwardZ = cos(radYaw)
        val rightX = cos(radYaw)
        val rightZ = -sin(radYaw)

        val vx = (forwardX * normZ + rightX * normX) * speed * deltaSec
        val vz = (forwardZ * normZ + rightZ * normX) * speed * deltaSec

        return Pair(vx, vz)
    }

    /**
     * Updates view rotation with strict non-inverted controls and vertical pitch clamping.
     *
     * - Positive deltaYaw (swiping right) rotates view clockwise to the right.
     * - Negative deltaYaw (swiping left) rotates view counter-clockwise to the left.
     * - Positive deltaPitch (swiping up) tilts view upwards.
     * - Negative deltaPitch (swiping down) tilts view downwards.
     */
    fun updateViewAngles(
        currentYaw: Float,
        currentPitch: Float,
        deltaYaw: Float,
        deltaPitch: Float
    ): Pair<Float, Float> {
        var newYaw = (currentYaw + deltaYaw) % 360f
        if (newYaw < 0f) newYaw += 360f

        val newPitch = (currentPitch + deltaPitch).coerceIn(minPitch, maxPitch)
        return Pair(newYaw, newPitch)
    }

    /**
     * Resolves collision against the ASCII world grid using swept sliding resolution.
     * Checks circular player volume against surrounding solid AABB grid tiles.
     *
     * Returns the updated (x, z) coordinates safely positioned without intersecting any solid tiles.
     */
    fun resolveGridMovement(
        startX: Float,
        startZ: Float,
        vx: Float,
        vz: Float,
        world: GeneratedWorld
    ): Pair<Float, Float> {
        var resolvedX = startX
        var resolvedZ = startZ

        // 1. Try sliding along X axis
        if (abs(vx) > 0.0001f) {
            val targetX = (resolvedX + vx).coerceIn(playerRadius, world.width - playerRadius)
            if (!checkCollisionWithWorld(targetX, resolvedZ, world)) {
                resolvedX = targetX
            } else {
                // Micro-step collision resolution towards wall edge
                resolvedX = findClosestClearPositionOnAxis(resolvedX, targetX, resolvedZ, isXAxis = true, world)
            }
        }

        // 2. Try sliding along Z axis
        if (abs(vz) > 0.0001f) {
            val targetZ = (resolvedZ + vz).coerceIn(playerRadius, world.height - playerRadius)
            if (!checkCollisionWithWorld(resolvedX, targetZ, world)) {
                resolvedZ = targetZ
            } else {
                // Micro-step collision resolution towards wall edge
                resolvedZ = findClosestClearPositionOnAxis(resolvedZ, targetZ, resolvedX, isXAxis = false, world)
            }
        }

        // 3. Final safety boundary clamp
        val clampedX = resolvedX.coerceIn(playerRadius, world.width - playerRadius)
        val clampedZ = resolvedZ.coerceIn(playerRadius, world.height - playerRadius)

        return Pair(clampedX, clampedZ)
    }

    /**
     * Checks if a circular bounding area at (x, z) intersects any solid tile in the world grid.
     */
    fun checkCollisionWithWorld(x: Float, z: Float, world: GeneratedWorld): Boolean {
        val minTileX = max(0, floor(x - playerRadius).toInt())
        val maxTileX = min(world.width - 1, floor(x + playerRadius).toInt())
        val minTileZ = max(0, floor(z - playerRadius).toInt())
        val maxTileZ = min(world.height - 1, floor(z + playerRadius).toInt())

        for (tx in minTileX..maxTileX) {
            for (tz in minTileZ..maxTileZ) {
                val tile = world.grid[tx][tz]
                if (tile.isSolid) {
                    if (circleIntersectsTileAABB(x, z, playerRadius, tx, tz)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Tests intersection between a circle at (cx, cz) with radius [r]
     * and a 1.0x1.0 square tile at integer coordinates (tileX, tileZ).
     */
    private fun circleIntersectsTileAABB(
        cx: Float,
        cz: Float,
        r: Float,
        tileX: Int,
        tileZ: Int
    ): Boolean {
        val tileMinX = tileX.toFloat()
        val tileMaxX = tileX + 1.0f
        val tileMinZ = tileZ.toFloat()
        val tileMaxZ = tileZ + 1.0f

        // Closest point on the AABB to the circle center
        val closestX = cx.coerceIn(tileMinX, tileMaxX)
        val closestZ = cz.coerceIn(tileMinZ, tileMaxZ)

        val distX = cx - closestX
        val distZ = cz - closestZ
        val distSq = distX * distX + distZ * distZ

        return distSq < (r * r)
    }

    /**
     * Performs a binary search step to find the furthest clear position along an axis
     * up to the wall boundary without intersecting the solid tile.
     */
    private fun findClosestClearPositionOnAxis(
        startVal: Float,
        targetVal: Float,
        otherAxisVal: Float,
        isXAxis: Boolean,
        world: GeneratedWorld
    ): Float {
        var low = 0.0f
        var high = 1.0f
        var bestPos = startVal

        for (i in 0 until 4) {
            val mid = (low + high) * 0.5f
            val testPos = startVal + (targetVal - startVal) * mid
            val testX = if (isXAxis) testPos else otherAxisVal
            val testZ = if (isXAxis) otherAxisVal else testPos

            if (!checkCollisionWithWorld(testX, testZ, world)) {
                bestPos = testPos
                low = mid
            } else {
                high = mid
            }
        }
        return bestPos
    }
}
