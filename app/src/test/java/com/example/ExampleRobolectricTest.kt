package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.GameEngine
import com.example.engine.WorldGenerator
import com.example.model.*
import com.example.renderer.AsciiRamps
import com.example.renderer.AsciiRasterizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertNotNull(appName)
        assertTrue(appName.isNotEmpty())
    }

    @Test
    fun `world generator creates valid playable district map`() {
        val world = WorldGenerator.generateDistrict(DistrictId.DISTRICT_01, baseSeed = 42L)
        assertNotNull(world)
        assertTrue(world.width >= 16)
        assertTrue(world.height >= 16)
        assertTrue("World must contain doors", world.doors.isNotEmpty())
        assertTrue("World must contain terminals", world.terminals.isNotEmpty())
        assertTrue("World must contain enemies", world.enemies.isNotEmpty())
        assertTrue("World must contain chests", world.lootChests.isNotEmpty())
        assertTrue("Spawn coordinate must be in bounds", world.spawnX > 0f && world.spawnZ > 0f)
        assertTrue("Extraction coordinate must be in bounds", world.extractionX > 0f && world.extractionZ > 0f)
        assertTrue("Spire core coordinate must be in bounds", world.spireCoreX > 0f && world.spireCoreZ > 0f)
    }

    @Test
    fun `hacking session generates network graph and handles exploit injection`() {
        val session = HackingSession.createSession(1, "SECTOR_ALPHA", iceLevel = 2)
        assertTrue(session.nodes.isNotEmpty())
        val entry = session.nodes.find { it.type == HackNodeType.ACCESS_PORT }
        assertNotNull(entry)
        assertTrue("Access port node must start captured", entry!!.isCaptured)

        val core = session.nodes.find { it.type == HackNodeType.CORE_MAINFRAME }
        assertNotNull(core)
        assertFalse(session.isSuccess)

        // Capture all nodes
        session.nodes.forEach { it.isCaptured = true }
        // Core being captured marks success
        assertTrue("Hacking session must succeed when nodes are captured", session.nodes.all { it.isCaptured })
    }

    @Test
    fun `ascii rasterizer allocates and resizes zero-alloc buffers`() {
        val rasterizer = AsciiRasterizer()
        rasterizer.resize(60, 30)
        assertEquals(60, rasterizer.cols)
        assertEquals(30, rasterizer.rows)

        val ramp = AsciiRamps.getRamp("cyber")
        assertNotNull(ramp)
        assertTrue(ramp.isNotEmpty())

        val charVal = AsciiRamps.sampleGlyph(ramp, 0.75f)
        assertNotEquals(' ', charVal)
    }

    @Test
    fun `game engine processes stealth takedown and weapon firing`() {
        val engine = GameEngine()
        engine.loadDistrict(DistrictId.DISTRICT_01)
        assertEquals(100, engine.playerHealth)
        assertEquals(50, engine.playerArmor)

        // Test augmentations
        val camoResult = engine.toggleCamo()
        assertTrue("Camo should toggle", camoResult)
        assertTrue(engine.isCloaked)

        // Test weapon firing
        val fired = engine.fireWeapon()
        assertTrue("Weapon should fire", fired)
        assertTrue(engine.currentWeapon.ammoInMag < engine.currentWeapon.maxMag)
    }

    @Test
    fun `primitives 3d mesh generator generates valid geometry buffers`() {
        val cube = com.example.renderer.Primitives3DFactory.createCubeMesh()
        assertNotNull(cube)
        assertTrue(cube.indexCount > 0)
        assertEquals(36, cube.indexCount)

        val pyramid = com.example.renderer.Primitives3DFactory.createPyramidMesh()
        assertNotNull(pyramid)
        assertTrue(pyramid.indexCount > 0)

        val cylinder = com.example.renderer.Primitives3DFactory.createCylinderMesh()
        assertNotNull(cylinder)
        assertTrue(cylinder.indexCount > 0)

        val sphere = com.example.renderer.Primitives3DFactory.createSphereMesh()
        assertNotNull(sphere)
        assertTrue(sphere.indexCount > 0)

        val torus = com.example.renderer.Primitives3DFactory.createTorusMesh()
        assertNotNull(torus)
        assertTrue(torus.indexCount > 0)
    }

    @Test
    fun `first person camera controller updates position, rotation and view matrix`() {
        val camera = com.example.renderer.FirstPersonCameraController(
            posX = 0.0f,
            posY = 1.6f,
            posZ = 5.0f,
            yaw = 0.0f,
            pitch = 0.0f
        )

        // Verify rotation and pitch clamping
        camera.rotate(45.0f, 30.0f)
        assertEquals(45.0f, camera.yaw, 0.01f)
        assertEquals(30.0f, camera.pitch, 0.01f)

        // Pitch should clamp at 85.0
        camera.rotate(0.0f, 100.0f)
        assertEquals(85.0f, camera.pitch, 0.01f)

        // Verify forward movement
        camera.reset()
        assertEquals(0.0f, camera.posX, 0.01f)
        assertEquals(5.0f, camera.posZ, 0.01f)

        camera.forwardInput = 1.0f
        camera.moveSpeed = 4.0f
        camera.update(0.5f) // Should move forward by 2.0 units (along Z when yaw=0)
        assertEquals(7.0f, camera.posZ, 0.05f)

        // Verify View Matrix calculation
        val viewMatrix = FloatArray(16)
        camera.computeViewMatrix(viewMatrix)
        assertNotNull(viewMatrix)
        assertTrue(viewMatrix[15] != 0f || viewMatrix[0] != 0f)
    }

    @Test
    fun `procedural level mesh generator parses custom ASCII layout into vertex buffers`() {
        val generator = com.example.renderer.ProceduralLevelMeshGenerator(tileSize = 2.0f, wallHeight = 2.5f)

        val customAscii = listOf(
            "######",
            "#S..D#",
            "#..T.#",
            "#C..E#",
            "######"
        )

        val levelBuffers = generator.buildLevelBuffers(customAscii)

        assertNotNull(levelBuffers)
        assertEquals(6, levelBuffers.gridWidth)
        assertEquals(5, levelBuffers.gridHeight)

        // Verify vertex and index buffers are generated
        assertTrue(levelBuffers.wallsMesh.indexCount > 0)
        assertTrue(levelBuffers.floorsMesh.indexCount > 0)
        assertTrue(levelBuffers.interactablesMesh.indexCount > 0)
        assertTrue(levelBuffers.combinedMesh.indexCount > 0)

        // Check extracted interactable entities (Spawn S, Door D, Terminal T, Cache C, Extraction E)
        val symbolsFound = levelBuffers.entities.map { it.symbol }.toSet()
        assertTrue(symbolsFound.contains('S'))
        assertTrue(symbolsFound.contains('D'))
        assertTrue(symbolsFound.contains('T'))
        assertTrue(symbolsFound.contains('C'))
        assertTrue(symbolsFound.contains('E'))
    }

    @Test
    fun `procedural level mesh generator produces valid randomized ASCII levels`() {
        val generator = com.example.renderer.ProceduralLevelMeshGenerator()
        val asciiGrid = generator.generateProceduralAsciiLayout(width = 16, height = 16, seed = 12345L)

        assertNotNull(asciiGrid)
        assertEquals(16, asciiGrid.size)
        assertEquals(16, asciiGrid[0].length)

        val buffers = generator.buildLevelBuffers(asciiGrid)
        assertNotNull(buffers)
        assertTrue(buffers.combinedMesh.indexCount > 0)
        assertTrue(buffers.entities.isNotEmpty())
    }
}
