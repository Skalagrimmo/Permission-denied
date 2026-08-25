package com.example.model

enum class NotificationLevel {
    INFO,
    SUCCESS,
    WARNING,
    CRITICAL
}

/**
 * High-contrast ASCII-styled status and telemetry notification payload.
 */
data class HudNotification(
    val id: Long = System.nanoTime(),
    val tag: String,
    val message: String,
    val level: NotificationLevel = NotificationLevel.INFO,
    val timestampText: String = "",
    var remainingLifetimeSec: Float = 4.0f,
    val initialLifetimeSec: Float = 4.0f
)
