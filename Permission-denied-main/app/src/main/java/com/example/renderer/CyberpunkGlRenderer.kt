package com.example.renderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.example.engine.GameEngine
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * GLSurfaceView.Renderer implementation that renders 3D primitives with an
 * ASCII-art post-processing shader to achieve the cyberpunk aesthetic.
 */
class CyberpunkGlRenderer : GLSurfaceView.Renderer {

    // 3D Primitives
    private lateinit var cubeMesh: Mesh3D
    private lateinit var pyramidMesh: Mesh3D
    private lateinit var cylinderMesh: Mesh3D
    private lateinit var sphereMesh: Mesh3D
    private lateinit var torusMesh: Mesh3D
    private lateinit var quadMesh: Mesh3D

    // Shaders & Offscreen FBO
    private val primitiveShader = Primitive3DShader()
    private val asciiPostShader = CyberpunkAsciiPostShader()
    private val fboHelper = FramebufferHelper()

    // Matrix buffers (reused to prevent GC allocations)
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val normalMatrix = FloatArray(16)
    private val scratchMatrix = FloatArray(16)

    // Viewport dimensions
    var surfaceWidth: Int = 1280
        private set
    var surfaceHeight: Int = 720
        private set

    // Configuration & State
    var engine: GameEngine? = null
    var showcaseMode: Boolean = false
    var asciiRamp: String = "cyber"
    var ansiMode: String = "GAME"
    var filterMode: String = "BOX"
    var isThermalVision: Boolean = false
    var scanlineIntensity: Float = 0.4f
    var bloomGlow: Float = 1.0f
    var chromaticAberration: Float = 1.0f

    // Camera Controller for first-person look-around and 3D navigation
    val cameraController = FirstPersonCameraController(
        posX = 0.0f,
        posY = 1.6f,
        posZ = 5.5f,
        yaw = 0.0f,
        pitch = -8.0f
    )

    private var startTimeMs = SystemClock.uptimeMillis()
    private var lastFrameMs = startTimeMs

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Initialize OpenGL states
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glDisable(GLES20.GL_BLEND)

        // Initialize 3D Meshes
        cubeMesh = Primitives3DFactory.createCubeMesh()
        pyramidMesh = Primitives3DFactory.createPyramidMesh()
        cylinderMesh = Primitives3DFactory.createCylinderMesh()
        sphereMesh = Primitives3DFactory.createSphereMesh()
        torusMesh = Primitives3DFactory.createTorusMesh()
        quadMesh = Primitives3DFactory.createFullscreenQuad()

        // Initialize Shaders
        primitiveShader.initialize()
        asciiPostShader.initialize()

        startTimeMs = SystemClock.uptimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)

        // Setup FBO for offscreen 3D primitive pass
        fboHelper.setup(surfaceWidth, surfaceHeight)

        // Setup 3D Projection Matrix (Field of view 70 degrees)
        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 70.0f, aspect, 0.1f, 100.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowMs = SystemClock.uptimeMillis()
        val deltaSec = ((nowMs - lastFrameMs) / 1000.0f).coerceIn(0.001f, 0.05f)
        lastFrameMs = nowMs
        val timeSec = (nowMs - startTimeMs) / 1000.0f

        cameraController.update(deltaSec)

        // ==========================================
        // PASS 1: RENDER 3D PRIMITIVES TO OFFSCREEN FBO
        // ==========================================
        fboHelper.bind()

        GLES20.glClearColor(0.02f, 0.04f, 0.08f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        primitiveShader.use()

        val eng = engine
        if (eng != null && !showcaseMode) {
            renderInGame3DWorld(eng, timeSec)
        } else {
            renderCyberpunkPrimitivesShowcase(timeSec)
        }

        fboHelper.unbind(surfaceWidth, surfaceHeight)

        // ==========================================
        // PASS 2: ASCII-ART POST-PROCESSING SHADER
        // ==========================================
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        // Target character grid columns & rows based on resolution
        val targetCols = when (filterMode) {
            "NEAREST" -> (surfaceWidth / 18).toFloat().coerceIn(40f, 75f)
            "CINEMATIC" -> (surfaceWidth / 11).toFloat().coerceIn(70f, 120f)
            else -> (surfaceWidth / 13).toFloat().coerceIn(55f, 95f) // BOX
        }
        val targetRows = (targetCols * surfaceHeight / surfaceWidth).coerceIn(24f, 65f)

        val ansiModeInt = when (ansiMode.uppercase()) {
            "ANSI_16" -> 1
            "GAME" -> 2
            else -> 0 // RGB
        }

        val effectiveThermal = isThermalVision || (eng?.isThermalVision == true)
        val effectiveGlow = if (eng?.muzzleFlashAlpha ?: 0f > 0f) bloomGlow + 1.5f else bloomGlow
        val effectiveChromatic = if (eng?.damageFlashAlpha ?: 0f > 0f) chromaticAberration + 2.5f else chromaticAberration

        asciiPostShader.use(
            sceneTexId = fboHelper.colorTextureId,
            screenWidth = surfaceWidth.toFloat(),
            screenHeight = surfaceHeight.toFloat(),
            gridCols = targetCols,
            gridRows = targetRows,
            timeSec = timeSec,
            scanline = scanlineIntensity,
            bloom = effectiveGlow,
            chromatic = effectiveChromatic,
            isThermal = effectiveThermal,
            ansiModeInt = ansiModeInt
        )

        drawQuad(asciiPostShader.aPositionLoc, asciiPostShader.aTexCoordLoc)
    }

    /**
     * Renders a 3D Showcase of Rotating Cyberpunk Primitives with dynamic lighting,
     * neon conduits, pulsating energy spheres, spinning drone pyramids, and orbital rings.
     */
    private fun renderCyberpunkPrimitivesShowcase(timeSec: Float) {
        // Compute View Matrix from First-Person Camera Controller
        cameraController.computeViewMatrix(viewMatrix)

        val eyeX = cameraController.posX
        val eyeY = cameraController.posY
        val eyeZ = cameraController.posZ

        // Neon Dynamic Light
        val lightX = cos(timeSec * 1.2f) * 4f
        val lightY = 3.5f
        val lightZ = sin(timeSec * 1.2f) * 4f
        setLightUniforms(
            eyeX, eyeY, eyeZ,
            lightX, lightY, lightZ,
            lightR = 0.0f, lightG = 0.94f, lightB = 1.0f, // Neon Cyan Light
            ambientR = 0.08f, ambientG = 0.12f, ambientB = 0.20f
        )

        // 1. Grid Floor Plane (Tiled Ground Primitives)
        for (gx in -4..4) {
            for (gz in -4..4) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, gx * 1.8f, -1.2f, gz * 1.8f)
                Matrix.scaleM(modelMatrix, 0, 1.7f, 0.1f, 1.7f)
                val isNeonTile = (gx + gz) % 2 == 0
                val matColor = if (isNeonTile) floatArrayOf(0.05f, 0.08f, 0.15f, 1f) else floatArrayOf(0.02f, 0.04f, 0.09f, 1f)
                val emissiveColor = if (isNeonTile) floatArrayOf(0.0f, 0.12f, 0.25f) else floatArrayOf(0f, 0f, 0f)
                drawPrimitive(cubeMesh, matColor, emissiveColor, wireframe = 1f, timeSec = timeSec)
            }
        }

        // 2. Central Cyber Core (Cylinder with glowing energy bands)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.2f, 0f)
        Matrix.rotateM(modelMatrix, 0, timeSec * 30f, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.9f, 2.4f, 0.9f)
        drawPrimitive(
            cylinderMesh,
            matColor = floatArrayOf(0.12f, 0.16f, 0.25f, 1f),
            emissiveColor = floatArrayOf(0.0f, 0.4f, 0.6f),
            wireframe = 1f,
            timeSec = timeSec
        )

        // 3. Central Holographic Plasma Orb (Sphere)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 1.9f + sin(timeSec * 2.5f) * 0.15f, 0f)
        Matrix.rotateM(modelMatrix, 0, timeSec * 60f, 1f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.5f, 0.5f, 0.5f)
        drawPrimitive(
            sphereMesh,
            matColor = floatArrayOf(0.0f, 1.0f, 0.5f, 1f),
            emissiveColor = floatArrayOf(0.0f, 0.9f, 0.4f),
            wireframe = 1f,
            timeSec = timeSec
        )

        // 4. Orbital Ring (Torus)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.5f, 0f)
        Matrix.rotateM(modelMatrix, 0, timeSec * -45f, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 35f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 1.6f, 1.6f, 1.6f)
        drawPrimitive(
            torusMesh,
            matColor = floatArrayOf(1.0f, 0.0f, 0.45f, 1f),
            emissiveColor = floatArrayOf(0.8f, 0.0f, 0.35f),
            wireframe = 1f,
            timeSec = timeSec
        )

        // 5. Patrolling Security Drones (Pyramids orbiting center)
        for (i in 0 until 3) {
            val droneAngle = timeSec * 1.1f + (i * 2.094f)
            val dx = cos(droneAngle) * 2.6f
            val dy = 0.8f + sin(timeSec * 3f + i) * 0.3f
            val dz = sin(droneAngle) * 2.6f

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, dx, dy, dz)
            Matrix.rotateM(modelMatrix, 0, -droneAngle * 57.29f + 90f, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, 180f, 1f, 0f, 0f) // Point down
            Matrix.scaleM(modelMatrix, 0, 0.5f, 0.7f, 0.5f)
            drawPrimitive(
                pyramidMesh,
                matColor = floatArrayOf(0.85f, 0.1f, 0.2f, 1f),
                emissiveColor = floatArrayOf(0.9f, 0.15f, 0.25f),
                wireframe = 0f,
                timeSec = timeSec
            )
        }

        // 6. Cyberpunk Server Cubes (Outer corners)
        val cornerPositions = arrayOf(
            floatArrayOf(-2.8f, 0.0f, -2.8f),
            floatArrayOf(2.8f, 0.0f, -2.8f),
            floatArrayOf(-2.8f, 0.0f, 2.8f),
            floatArrayOf(2.8f, 0.0f, 2.8f)
        )

        for ((idx, pos) in cornerPositions.withIndex()) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, pos[0], pos[1], pos[2])
            Matrix.rotateM(modelMatrix, 0, (idx * 90f) + timeSec * 15f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.8f, 1.6f, 0.8f)
            drawPrimitive(
                cubeMesh,
                matColor = floatArrayOf(0.15f, 0.22f, 0.32f, 1f),
                emissiveColor = if (idx % 2 == 0) floatArrayOf(0.0f, 0.4f, 0.7f) else floatArrayOf(0.7f, 0.4f, 0.0f),
                wireframe = 1f,
                timeSec = timeSec
            )
        }
    }

    /**
     * Renders the in-game cyberpunk district world in 3D:
     * - Sector floor tiles, modular wall cubes, doors, security terminals
     * - Patrolling drone pyramids, enemies, item pickups, laser tripwires
     */
    private fun renderInGame3DWorld(eng: GameEngine, timeSec: Float) {
        val px = eng.playerX
        val py = eng.playerY
        val pz = eng.playerZ
        val yawRad = Math.toRadians(eng.playerYaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(eng.playerPitch.toDouble()).toFloat()

        val dirX = sin(yawRad) * cos(pitchRad)
        val dirY = sin(pitchRad)
        val dirZ = cos(yawRad) * cos(pitchRad)

        Matrix.setLookAtM(
            viewMatrix, 0,
            px, py, pz,
            px + dirX, py + dirY, pz + dirZ,
            0f, 1f, 0f
        )

        // Light setup (Player tactical flashlight + district ambient)
        setLightUniforms(
            px, py, pz,
            px, py, pz,
            lightR = 0.0f, lightG = 0.94f, lightB = 1.0f,
            ambientR = 0.15f, ambientG = 0.18f, ambientB = 0.28f
        )

        val world = eng.world
        val grid = world.grid
        val mapW = world.width
        val mapH = world.height

        val minX = (px - 14).toInt().coerceAtLeast(0)
        val maxX = (px + 14).toInt().coerceAtMost(mapW - 1)
        val minZ = (pz - 14).toInt().coerceAtLeast(0)
        val maxZ = (pz + 14).toInt().coerceAtMost(mapH - 1)

        // 1. Floor & Ceiling
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val tile = grid[x][z]
                if (!tile.isSolid) {
                    // Floor
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, x + 0.5f, 0.0f, z + 0.5f)
                    Matrix.scaleM(modelMatrix, 0, 1.0f, 0.05f, 1.0f)
                    drawPrimitive(cubeMesh, floatArrayOf(0.08f, 0.12f, 0.18f, 1f), floatArrayOf(0f, 0.02f, 0.05f), 1f, timeSec)

                    // Ceiling
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, x + 0.5f, 2.0f, z + 0.5f)
                    Matrix.scaleM(modelMatrix, 0, 1.0f, 0.05f, 1.0f)
                    drawPrimitive(cubeMesh, floatArrayOf(0.04f, 0.06f, 0.10f, 1f), floatArrayOf(0f, 0f, 0f), 1f, timeSec)
                }
            }
        }

        // 2. Wall Cubes, Terminals, Doors
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val tile = grid[x][z]
                when (tile) {
                    com.example.model.TileType.WALL_CONCRETE, com.example.model.TileType.WALL_CORP_PANEL -> {
                        // Solid Wall Cube
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, x + 0.5f, 1.0f, z + 0.5f)
                        Matrix.scaleM(modelMatrix, 0, 1.0f, 2.0f, 1.0f)
                        drawPrimitive(cubeMesh, floatArrayOf(0.20f, 0.28f, 0.38f, 1f), floatArrayOf(0.0f, 0.1f, 0.2f), 1f, timeSec)
                    }
                    com.example.model.TileType.WALL_GLASS -> {
                        // Cyan Holographic Glass Wall
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, x + 0.5f, 1.0f, z + 0.5f)
                        Matrix.scaleM(modelMatrix, 0, 0.95f, 2.0f, 0.95f)
                        drawPrimitive(cubeMesh, floatArrayOf(0.0f, 0.8f, 1.0f, 0.5f), floatArrayOf(0.0f, 0.9f, 1.0f), 1f, timeSec)
                    }
                    com.example.model.TileType.DOOR_CLOSED, com.example.model.TileType.DOOR_LOCKED_BLUE, com.example.model.TileType.DOOR_LOCKED_RED -> {
                        // Security Door
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, x + 0.5f, 1.0f, z + 0.5f)
                        Matrix.scaleM(modelMatrix, 0, 0.9f, 1.9f, 0.3f)
                        val emitCol = if (tile == com.example.model.TileType.DOOR_LOCKED_RED) floatArrayOf(1.0f, 0.1f, 0.1f) else floatArrayOf(0.0f, 0.7f, 1.0f)
                        drawPrimitive(cubeMesh, floatArrayOf(0.35f, 0.4f, 0.45f, 1f), emitCol, 0f, timeSec)
                    }
                    com.example.model.TileType.TERMINAL, com.example.model.TileType.GHOST_INDEX_MAINFRAME -> {
                        // Terminal / Core
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, x + 0.5f, 0.8f, z + 0.5f)
                        Matrix.scaleM(modelMatrix, 0, 0.6f, 1.6f, 0.6f)
                        drawPrimitive(cubeMesh, floatArrayOf(0.1f, 0.2f, 0.3f, 1f), floatArrayOf(0.0f, 0.8f, 0.9f), 1f, timeSec)
                    }
                    com.example.model.TileType.LASER_TRIPWIRE -> {
                        // Laser Tripwire Beam
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, x + 0.5f, 0.5f, z + 0.5f)
                        Matrix.scaleM(modelMatrix, 0, 0.1f, 0.1f, 1.0f)
                        drawPrimitive(cylinderMesh, floatArrayOf(1.0f, 0.0f, 0.3f, 1f), floatArrayOf(1.0f, 0.1f, 0.2f), 0f, timeSec)
                    }
                    else -> {}
                }
            }
        }

        // 3. Enemies & Drones
        for (enemy in world.enemies) {
            if (enemy.hp > 0 && enemy.state != com.example.model.EnemyAiState.DEAD) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, enemy.x, 0.9f, enemy.z)
                if (enemy.type == com.example.model.EnemyType.SECURITY_DRONE) {
                    // Drone: Flying Spinning Pyramid
                    Matrix.rotateM(modelMatrix, 0, timeSec * 90f, 0f, 1f, 0f)
                    Matrix.rotateM(modelMatrix, 0, 180f, 1f, 0f, 0f)
                    Matrix.scaleM(modelMatrix, 0, 0.55f, 0.65f, 0.55f)
                    drawPrimitive(pyramidMesh, floatArrayOf(0.9f, 0.1f, 0.2f, 1f), floatArrayOf(1.0f, 0.2f, 0.2f), 0f, timeSec)
                } else if (enemy.isBoss) {
                    // Boss Paladin: Giant Cylinder + Emissive Core
                    Matrix.rotateM(modelMatrix, 0, enemy.yaw, 0f, 1f, 0f)
                    Matrix.scaleM(modelMatrix, 0, 0.8f, 1.8f, 0.8f)
                    drawPrimitive(cylinderMesh, floatArrayOf(0.9f, 0.1f, 0.4f, 1f), floatArrayOf(1.0f, 0.0f, 0.5f), 1f, timeSec)
                } else {
                    // Security Guard: Torso Cube + Visor Cylinder
                    Matrix.scaleM(modelMatrix, 0, 0.5f, 1.3f, 0.5f)
                    drawPrimitive(cubeMesh, floatArrayOf(0.3f, 0.15f, 0.15f, 1f), floatArrayOf(0.8f, 0.1f, 0.1f), 0f, timeSec)
                }
            }
        }

        // 4. Interactive Pickups / Loot Chests
        for (item in world.lootChests) {
            if (!item.isOpened) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, item.x, 0.5f + sin(timeSec * 3f) * 0.1f, item.z)
                Matrix.rotateM(modelMatrix, 0, timeSec * 60f, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.35f, 0.35f, 0.35f)
                drawPrimitive(sphereMesh, floatArrayOf(0.0f, 0.9f, 0.9f, 1f), floatArrayOf(0.0f, 1.0f, 0.7f), 1f, timeSec)
            }
        }
    }

    private fun setLightUniforms(
        camX: Float, camY: Float, camZ: Float,
        lightX: Float, lightY: Float, lightZ: Float,
        lightR: Float, lightG: Float, lightB: Float,
        ambientR: Float, ambientG: Float, ambientB: Float
    ) {
        GLES20.glUniform3f(primitiveShader.uCameraPosLoc, camX, camY, camZ)
        GLES20.glUniform3f(primitiveShader.uLightPosLoc, lightX, lightY, lightZ)
        GLES20.glUniform3f(primitiveShader.uLightColorLoc, lightR, lightG, lightB)
        GLES20.glUniform3f(primitiveShader.uAmbientColorLoc, ambientR, ambientG, ambientB)
    }

    private fun drawPrimitive(
        mesh: Mesh3D,
        matColor: FloatArray,
        emissiveColor: FloatArray,
        wireframe: Float,
        timeSec: Float
    ) {
        // Compute Normal Matrix (transpose of inverse of model matrix)
        Matrix.invertM(scratchMatrix, 0, modelMatrix, 0)
        Matrix.transposeM(normalMatrix, 0, scratchMatrix, 0)

        // Set Matrix Uniforms
        GLES20.glUniformMatrix4fv(primitiveShader.uModelMatrixLoc, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(primitiveShader.uViewMatrixLoc, 1, false, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(primitiveShader.uProjectionMatrixLoc, 1, false, projectionMatrix, 0)
        GLES20.glUniformMatrix4fv(primitiveShader.uNormalMatrixLoc, 1, false, normalMatrix, 0)

        // Material uniforms
        GLES20.glUniform4fv(primitiveShader.uMaterialColorLoc, 1, matColor, 0)
        GLES20.glUniform3fv(primitiveShader.uEmissiveColorLoc, 1, emissiveColor, 0)
        GLES20.glUniform1f(primitiveShader.uSpecularPowerLoc, 32.0f)
        GLES20.glUniform1f(primitiveShader.uWireframeGridLoc, wireframe)
        GLES20.glUniform1f(primitiveShader.uTimeLoc, timeSec)

        // Vertex Attributes
        if (primitiveShader.aPositionLoc >= 0) {
            GLES20.glEnableVertexAttribArray(primitiveShader.aPositionLoc)
            GLES20.glVertexAttribPointer(primitiveShader.aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)
        }

        if (primitiveShader.aNormalLoc >= 0) {
            GLES20.glEnableVertexAttribArray(primitiveShader.aNormalLoc)
            GLES20.glVertexAttribPointer(primitiveShader.aNormalLoc, 3, GLES20.GL_FLOAT, false, 0, mesh.normalBuffer)
        }

        if (primitiveShader.aTexCoordLoc >= 0) {
            GLES20.glEnableVertexAttribArray(primitiveShader.aTexCoordLoc)
            GLES20.glVertexAttribPointer(primitiveShader.aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, mesh.texCoordBuffer)
        }

        // Draw Elements
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        if (primitiveShader.aPositionLoc >= 0) GLES20.glDisableVertexAttribArray(primitiveShader.aPositionLoc)
        if (primitiveShader.aNormalLoc >= 0) GLES20.glDisableVertexAttribArray(primitiveShader.aNormalLoc)
        if (primitiveShader.aTexCoordLoc >= 0) GLES20.glDisableVertexAttribArray(primitiveShader.aTexCoordLoc)
    }

    private fun drawQuad(aPosLoc: Int, aTexLoc: Int) {
        if (aPosLoc >= 0) {
            GLES20.glEnableVertexAttribArray(aPosLoc)
            GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, quadMesh.vertexBuffer)
        }

        if (aTexLoc >= 0) {
            GLES20.glEnableVertexAttribArray(aTexLoc)
            GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, quadMesh.texCoordBuffer)
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, quadMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, quadMesh.indexBuffer)

        if (aPosLoc >= 0) GLES20.glDisableVertexAttribArray(aPosLoc)
        if (aTexLoc >= 0) GLES20.glDisableVertexAttribArray(aTexLoc)
    }

    fun release() {
        fboHelper.release()
        primitiveShader.release()
        asciiPostShader.release()
    }
}
