package com.example.renderer

object AsciiRamps {
    const val CLASSIC = " .:-=+*#%@"
    const val DENSE = " .,:;i1tfLCG08@"
    const val CYBER = " ·:;|/+=<>[]{}#*X%@"
    const val BLOCKS = " ░▒▓█"

    fun getRamp(name: String, seed: Long = 1337L): String {
        return when (name.lowercase()) {
            "classic" -> CLASSIC
            "dense" -> DENSE
            "blocks" -> BLOCKS
            "custom" -> generateCustomRamp(seed)
            else -> CYBER
        }
    }

    fun generateCustomRamp(seed: Long): String {
        val basePool = listOf(' ', '.', ':', ';', '~', '^', '=', '+', '*', 'x', '#', '%', '$', '@', '&')
        val rng = java.util.Random(seed)
        val shuffled = basePool.shuffled(rng).distinct()
        return " " + shuffled.take(9).joinToString("") + "@"
    }

    fun sampleGlyph(ramp: String, intensity: Float): Char {
        val clamped = intensity.coerceIn(0f, 1f)
        val index = (clamped * (ramp.length - 1)).toInt().coerceIn(0, ramp.length - 1)
        return ramp[index]
    }
}

object AnsiPalette {
    // 16 Standard ANSI Colors (RGBA)
    val ANSI_16 = intArrayOf(
        0xFF000000.toInt(), // Black
        0xFF800000.toInt(), // Maroon
        0xFF008000.toInt(), // Green
        0xFF808000.toInt(), // Olive
        0xFF000080.toInt(), // Navy
        0xFF800080.toInt(), // Purple
        0xFF008080.toInt(), // Teal
        0xFFC0C0C0.toInt(), // Silver
        0xFF808080.toInt(), // Gray
        0xFFFF0000.toInt(), // Red
        0xFF00FF00.toInt(), // Lime
        0xFFFFFF00.toInt(), // Yellow
        0xFF0000FF.toInt(), // Blue
        0xFFFF00FF.toInt(), // Fuchsia
        0xFF00FFFF.toInt(), // Aqua
        0xFFFFFFFF.toInt()  // White
    )

    // Cyberpunk Game Palette
    val GAME_PALETTE = intArrayOf(
        0xFF050811.toInt(), // Deep Void
        0xFF00F0FF.toInt(), // Neon Cyan
        0xFF00FF66.toInt(), // Phosphor Green
        0xFFFF0077.toInt(), // Neon Magenta
        0xFFFFB800.toInt(), // Warning Amber
        0xFFFF1744.toInt(), // Cyber Red
        0xFF7C4DFF.toInt(), // Deep Violet
        0xFFE0E6ED.toInt(), // Terminal White
        0xFF1A2634.toInt(), // Slate Wall
        0xFF37474F.toInt()  // Metal Door
    )

    fun quantizeColor(color: Int, mode: String): Int {
        val a = (color ushr 24) and 0xFF
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF

        return when (mode.uppercase()) {
            "ANSI_16" -> findNearestColor(r, g, b, ANSI_16, a)
            "ANSI_256" -> quantizeAnsi256(r, g, b, a)
            else -> color // Default game palette
        }
    }

    private fun findNearestColor(r: Int, g: Int, b: Int, palette: IntArray, alpha: Int): Int {
        var minDistance = Int.MAX_VALUE
        var bestColor = palette[0]
        for (c in palette) {
            val cr = (c ushr 16) and 0xFF
            val cg = (c ushr 8) and 0xFF
            val cb = c and 0xFF
            val dist = (r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb)
            if (dist < minDistance) {
                minDistance = dist
                bestColor = c
            }
        }
        return (alpha shl 24) or (bestColor and 0x00FFFFFF)
    }

    private fun quantizeAnsi256(r: Int, g: Int, b: Int, alpha: Int): Int {
        // 6x6x6 color cube
        val qr = (r * 5 + 127) / 255
        val qg = (g * 5 + 127) / 255
        val qb = (b * 5 + 127) / 255
        val rr = if (qr > 0) qr * 40 + 55 else 0
        val gg = if (qg > 0) qg * 40 + 55 else 0
        val bb = if (qb > 0) qb * 40 + 55 else 0
        return (alpha shl 24) or (rr shl 16) or (gg shl 8) or bb
    }
}
