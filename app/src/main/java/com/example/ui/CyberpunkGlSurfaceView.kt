package com.example.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.example.engine.GameEngine
import com.example.renderer.CyberpunkGlRenderer

/**
 * Custom GLSurfaceView implementation that renders 3D primitives (cubes, cylinders,
 * pyramids, spheres, orbital toruses, and 3D district environments) with a real-time
 * ASCII-art style post-processing shader to achieve the cyberpunk aesthetic.
 */
class CyberpunkGlSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val glRenderer = CyberpunkGlRenderer()

    var engine: GameEngine?
        get() = glRenderer.engine
        set(value) {
            glRenderer.engine = value
        }

    var showcaseMode: Boolean
        get() = glRenderer.showcaseMode
        set(value) {
            glRenderer.showcaseMode = value
        }

    var asciiRamp: String
        get() = glRenderer.asciiRamp
        set(value) {
            glRenderer.asciiRamp = value
        }

    var ansiMode: String
        get() = glRenderer.ansiMode
        set(value) {
            glRenderer.ansiMode = value
        }

    var filterMode: String
        get() = glRenderer.filterMode
        set(value) {
            glRenderer.filterMode = value
        }

    var isThermalVision: Boolean
        get() = glRenderer.isThermalVision
        set(value) {
            glRenderer.isThermalVision = value
        }

    var scanlineIntensity: Float
        get() = glRenderer.scanlineIntensity
        set(value) {
            glRenderer.scanlineIntensity = value
        }

    var bloomGlow: Float
        get() = glRenderer.bloomGlow
        set(value) {
            glRenderer.bloomGlow = value
        }

    var chromaticAberration: Float
        get() = glRenderer.chromaticAberration
        set(value) {
            glRenderer.chromaticAberration = value
        }

    val cameraController: com.example.renderer.FirstPersonCameraController
        get() = glRenderer.cameraController

    init {
        // Use OpenGL ES 2.0 / 3.0 context
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(glRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        val handled = glRenderer.cameraController.handleTouchEvent(event, width, height)
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            performClick()
        }
        return handled || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        queueEvent {
            glRenderer.release()
        }
    }
}
