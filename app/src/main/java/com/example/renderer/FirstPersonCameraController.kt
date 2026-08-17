package com.example.renderer

import android.opengl.Matrix
import android.view.MotionEvent
import kotlin.math.cos
import kotlin.math.sin

/**
 * First-person 3D camera controller for OpenGL ES 3D scenes.
 * Interprets multi-touch screen gestures for:
 * - Look-around: Dragging across the view (or right-hand screen region) adjusts pitch and yaw.
 * - Movement: Dragging on the movement region (or two-finger drag / left-hand region) translates
 *   the camera along its local forward and strafe axes.
 */
class FirstPersonCameraController(
    var posX: Float = 0.0f,
    var posY: Float = 1.6f,
    var posZ: Float = 4.5f,
    var yaw: Float = 0.0f,
    var pitch: Float = 0.0f
) {
    var moveSpeed: Float = 4.5f
    var lookSensitivity: Float = 0.22f
    var isEnabled: Boolean = true

    // Target velocity inputs [-1..1]
    var forwardInput: Float = 0.0f
    var strafeInput: Float = 0.0f
    var elevateInput: Float = 0.0f

    // Touch tracking state
    private var primaryPointerId = -1
    private var secondaryPointerId = -1
    private var lastLookTouchX = 0f
    private var lastLookTouchY = 0f
    private var moveStartX = 0f
    private var moveStartY = 0f
    private var isMoving = false

    // Initial default state for reset
    private var defaultX = posX
    private var defaultY = posY
    private var defaultZ = posZ
    private var defaultYaw = yaw
    private var defaultPitch = pitch

    fun setDefaultTransform(x: Float, y: Float, z: Float, initialYaw: Float, initialPitch: Float) {
        defaultX = x
        defaultY = y
        defaultZ = z
        defaultYaw = initialYaw
        defaultPitch = initialPitch
        reset()
    }

    fun reset() {
        posX = defaultX
        posY = defaultY
        posZ = defaultZ
        yaw = defaultYaw
        pitch = defaultPitch
        forwardInput = 0f
        strafeInput = 0f
        elevateInput = 0f
        isMoving = false
        primaryPointerId = -1
        secondaryPointerId = -1
    }

    /**
     * Updates camera position based on active movement velocity inputs.
     */
    fun update(deltaSec: Float) {
        if (!isEnabled || deltaSec <= 0f) return

        val clampedDelta = deltaSec.coerceIn(0.001f, 0.5f)
        if (forwardInput != 0f || strafeInput != 0f || elevateInput != 0f) {
            val yawRad = Math.toRadians(yaw.toDouble()).toFloat()

            // Forward vector on XZ plane
            val fwdX = sin(yawRad)
            val fwdZ = cos(yawRad)

            // Right strafe vector on XZ plane
            val rightX = cos(yawRad)
            val rightZ = -sin(yawRad)

            val moveDistance = moveSpeed * clampedDelta
            posX += (fwdX * forwardInput + rightX * strafeInput) * moveDistance
            posZ += (fwdZ * forwardInput + rightZ * strafeInput) * moveDistance
            posY += elevateInput * moveDistance
        }
    }

    /**
     * Adjusts camera orientation with clamping on vertical pitch (-85° to 85°).
     */
    fun rotate(deltaYaw: Float, deltaPitch: Float) {
        if (!isEnabled) return
        yaw = (yaw + deltaYaw) % 360f
        if (yaw < 0f) yaw += 360f

        pitch = (pitch + deltaPitch).coerceIn(-85.0f, 85.0f)
    }

    /**
     * Translates camera relative to current heading.
     */
    fun moveRelative(forward: Float, strafe: Float, elevate: Float = 0f) {
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val fwdX = sin(yawRad)
        val fwdZ = cos(yawRad)
        val rightX = cos(yawRad)
        val rightZ = -sin(yawRad)

        posX += (fwdX * forward + rightX * strafe)
        posZ += (fwdZ * forward + rightZ * strafe)
        posY += elevate
    }

    /**
     * Computes the OpenGL ES LookAt view matrix using current position, yaw, and pitch.
     */
    fun computeViewMatrix(outViewMatrix: FloatArray) {
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()

        val dirX = sin(yawRad) * cos(pitchRad)
        val dirY = sin(pitchRad)
        val dirZ = cos(yawRad) * cos(pitchRad)

        Matrix.setLookAtM(
            outViewMatrix, 0,
            posX, posY, posZ,
            posX + dirX, posY + dirY, posZ + dirZ,
            0.0f, 1.0f, 0.0f
        )
    }

    /**
     * Interprets touch events for intuitive 3D navigation:
     * - Left side of screen (x < width * 0.45): Virtual movement thumbstick (Drag to move forward/backward/strafe)
     * - Right side of screen (x >= width * 0.45): Drag to look around (yaw & pitch rotation)
     */
    fun handleTouchEvent(event: MotionEvent, viewWidth: Int, viewHeight: Int): Boolean {
        if (!isEnabled || viewWidth <= 0 || viewHeight <= 0) return false

        val actionMasked = event.actionMasked
        val pointerIndex = event.actionIndex

        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                if (x < viewWidth * 0.45f) {
                    // Movement touch zone
                    if (secondaryPointerId == -1) {
                        secondaryPointerId = pointerId
                        moveStartX = x
                        moveStartY = y
                        isMoving = true
                    }
                } else {
                    // Look-around touch zone
                    if (primaryPointerId == -1) {
                        primaryPointerId = pointerId
                        lastLookTouchX = x
                        lastLookTouchY = y
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerCount = event.pointerCount
                for (i in 0 until pointerCount) {
                    val pId = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    if (pId == primaryPointerId) {
                        // Look around
                        val deltaX = x - lastLookTouchX
                        val deltaY = y - lastLookTouchY
                        lastLookTouchX = x
                        lastLookTouchY = y

                        rotate(deltaX * lookSensitivity, -deltaY * lookSensitivity)
                    } else if (pId == secondaryPointerId) {
                        // Movement
                        val maxDrag = (viewWidth * 0.15f).coerceAtLeast(60f)
                        val dx = (x - moveStartX).coerceIn(-maxDrag, maxDrag)
                        val dy = (y - moveStartY).coerceIn(-maxDrag, maxDrag)

                        strafeInput = dx / maxDrag
                        forwardInput = -dy / maxDrag
                    }
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == primaryPointerId) {
                    primaryPointerId = -1
                }
                if (pointerId == secondaryPointerId) {
                    secondaryPointerId = -1
                    forwardInput = 0.0f
                    strafeInput = 0.0f
                    isMoving = false
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                primaryPointerId = -1
                secondaryPointerId = -1
                forwardInput = 0.0f
                strafeInput = 0.0f
                isMoving = false
                return true
            }
        }
        return false
    }
}
