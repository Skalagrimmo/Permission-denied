package com.example.engine

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.WeaponType

/**
 * High-fidelity tactile haptic feedback engine for in-game events:
 * detection buildup, alarms, hacking successes/failures, weapon recoil,
 * damage shockwaves, and tactical cybernetics interactions.
 */
class GameHapticsEngine private constructor(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isHapticsEnabled: Boolean = true

    // --- Threat & Detection Patterns ---

    /**
     * Detection warning when enemy is investigating / suspicious
     */
    fun onSuspiciousPing() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(35L, 80)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(35L)
        }
    }

    /**
     * Urgent threat heartbeat pulse when detection meter rises above 60%
     */
    fun onDetectionWarning() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Heartbeat double pulse
            val timings = longArrayOf(0, 45, 60, 65)
            val amplitudes = intArrayOf(0, 140, 0, 200)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 70, 70), -1)
        }
    }

    /**
     * Heavy siren vibration when district alarm triggers
     */
    fun onAlarmTriggered() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 80, 50, 80, 50, 120)
            val amplitudes = intArrayOf(0, 220, 0, 220, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 80, 50, 80, 50, 120), -1)
        }
    }

    // --- Cyber Hacking Patterns ---

    /**
     * Crisp tactile click when capturing a sub-net hacking node
     */
    fun onHackNodeCaptured() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20L, 120))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20L)
        }
    }

    /**
     * Ascending 3-stage celebration burst on successful terminal hack
     */
    fun onHackSuccess() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 40, 40, 40, 90)
            val amplitudes = intArrayOf(0, 100, 0, 170, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 30, 40, 40, 40, 90), -1)
        }
    }

    /**
     * Harsh jarring pulse on hacking lock-out or failure
     */
    fun onHackFailed() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 150, 60, 180)
            val amplitudes = intArrayOf(0, 240, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 150, 60, 180), -1)
        }
    }

    // --- Combat & Damage Shockwaves ---

    /**
     * Player damage impact feedback proportional to severity
     */
    fun onPlayerDamage(isShieldAbsorb: Boolean, amount: Int) {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isShieldAbsorb) {
                // Shield absorption: crisp metallic bounce
                val timings = longArrayOf(0, 40, 30, 30)
                val amplitudes = intArrayOf(0, 120, 0, 80)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                // Heavy direct health damage shockwave
                val intensity = (150 + amount * 3).coerceIn(150, 255)
                val duration = (60L + amount * 2L).coerceIn(60L, 220L)
                val timings = longArrayOf(0, duration / 2, 20, duration / 2)
                val amplitudes = intArrayOf(0, intensity, 0, intensity - 30)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (isShieldAbsorb) 40L else 120L)
        }
    }

    /**
     * Weapon recoil kickback pattern
     */
    fun onWeaponFired(weaponType: WeaponType) {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (weaponType) {
                WeaponType.SILENCED_PISTOL -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(22L, 90))
                }
                WeaponType.CYBER_SMG -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(15L, 160))
                }
                WeaponType.ARC_STUNNER -> {
                    val timings = longArrayOf(0, 25, 20, 35)
                    val amplitudes = intArrayOf(0, 180, 0, 220)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                WeaponType.EMP_GRENADE -> {
                    val timings = longArrayOf(0, 40, 20, 80, 30, 110)
                    val amplitudes = intArrayOf(0, 100, 0, 200, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                else -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(20L, 100))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25L)
        }
    }

    /**
     * Takedown physical impact
     */
    fun onTakedownImpact() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 40, 90)
            val amplitudes = intArrayOf(0, 180, 0, 240)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 40, 90), -1)
        }
    }

    // --- Tactical Interactions ---

    /**
     * Dash kinetic surge
     */
    fun onDashSurge() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 20, 60)
            val amplitudes = intArrayOf(0, 110, 0, 230)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(70L)
        }
    }

    /**
     * Optical cloak engagement
     */
    fun onCloakToggle(isCloaked: Boolean) {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(18L, if (isCloaked) 70 else 110))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20L)
        }
    }

    /**
     * Keycard door access
     */
    fun onKeycardUnlocked() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 30, 35)
            val amplitudes = intArrayOf(0, 100, 0, 190)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 20, 30, 35), -1)
        }
    }

    /**
     * Loot item container collection
     */
    fun onLootCollected() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(15L, 80))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15L)
        }
    }

    /**
     * Standard UI button click
     */
    fun onUiClick() {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(12L, 60))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(12L)
        }
    }

    companion object {
        @Volatile
        private var instance: GameHapticsEngine? = null

        fun getInstance(context: Context): GameHapticsEngine {
            return instance ?: synchronized(this) {
                instance ?: GameHapticsEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
