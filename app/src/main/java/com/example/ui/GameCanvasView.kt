package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.example.engine.GameEngine
import com.example.renderer.AsciiRasterizer

class GameCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rasterizer = AsciiRasterizer()
    var engine: GameEngine? = null
    var asciiRamp: String = "cyber"
        set(value) {
            field = value
            rasterizer.ramp = com.example.renderer.AsciiRamps.getRamp(value)
        }
    var ansiMode: String = "GAME"
        set(value) {
            field = value
            rasterizer.colorMode = value
        }
    var filterMode: String = "BOX"
        set(value) {
            field = value
            rasterizer.filterMode = value
        }

    private var lastFrameTimeNs = System.nanoTime()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // Adaptive resolution for 60fps on Mali-400 / low-end hardware
            val targetCols = when (filterMode) {
                "NEAREST" -> (w / 16).coerceIn(40, 80)
                "CINEMATIC" -> (w / 10).coerceIn(60, 110)
                else -> (w / 12).coerceIn(50, 95) // BOX
            }
            val targetRows = (targetCols * h / w).coerceIn(24, 60)
            rasterizer.resize(targetCols, targetRows)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val deltaSec = ((now - lastFrameTimeNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        lastFrameTimeNs = now

        val eng = engine
        if (eng != null && width > 0 && height > 0) {
            rasterizer.rasterizeScene(
                world = eng.world,
                playerX = eng.playerX,
                playerY = eng.playerY,
                playerZ = eng.playerZ,
                playerYaw = eng.playerYaw,
                playerPitch = eng.playerPitch,
                isThermalVision = eng.isThermalVision,
                isCloaked = eng.isCloaked,
                muzzleFlashAlpha = eng.muzzleFlashAlpha,
                damageFlashAlpha = eng.damageFlashAlpha,
                deltaSec = deltaSec
            )

            rasterizer.drawToCanvas(
                canvas = canvas,
                screenWidth = width.toFloat(),
                screenHeight = height.toFloat(),
                muzzleFlashAlpha = eng.muzzleFlashAlpha,
                damageAlpha = eng.damageFlashAlpha,
                isThermalVision = eng.isThermalVision,
                isCloaked = eng.isCloaked
            )
        }

        // Trigger continuous 60fps frame loop
        postInvalidateOnAnimation()
    }
}
