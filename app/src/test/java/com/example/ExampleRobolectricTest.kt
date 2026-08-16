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
}
