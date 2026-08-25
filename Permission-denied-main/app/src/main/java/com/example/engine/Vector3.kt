package com.example.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float): Vector3 {
        x = nx
        y = ny
        z = nz
        return this
    }

    fun add(v: Vector3): Vector3 = Vector3(x + v.x, y + v.y, z + v.z)
    fun sub(v: Vector3): Vector3 = Vector3(x - v.x, y - v.y, z - v.z)
    fun scale(s: Float): Vector3 = Vector3(x * s, y * s, z * s)

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun distanceTo(v: Vector3): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distance2DTo(tx: Float, tz: Float): Float {
        val dx = x - tx
        val dz = z - tz
        return sqrt(dx * dx + dz * dz)
    }

    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0.0001f) Vector3(x / len, y / len, z / len) else Vector3(0f, 0f, 0f)
    }
}

object MathUtils {
    const val PI = Math.PI.toFloat()
    const val TWO_PI = (Math.PI * 2.0).toFloat()
    const val DEG_TO_RAD = PI / 180f
    const val RAD_TO_DEG = 180f / PI

    fun clamp(value: Float, min: Float, max: Float): Float = when {
        value < min -> min
        value > max -> max
        else -> value
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * clamp(t, 0f, 1f)

    fun angleDifference(a: Float, b: Float): Float {
        var diff = (b - a + 180f) % 360f - 180f
        if (diff < -180f) diff += 360f
        return diff
    }

    fun distance2D(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        val dx = x1 - x2
        val dz = z1 - z2
        return sqrt(dx * dx + dz * dz)
    }
}
