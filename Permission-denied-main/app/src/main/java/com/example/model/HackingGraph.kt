package com.example.model

import java.util.Random
import kotlin.math.max
import kotlin.math.min

enum class HackNodeType(val label: String, val baseIceHp: Int, val isRoot: Boolean, val isAccessPoint: Boolean) {
    ACCESS_PORT("PORT", 0, false, true),
    DATA_BUFFER("BUFFER", 20, false, false),
    ICE_FIREWALL("ICE", 50, false, false),
    SECURITY_GATEWAY("GATEWAY", 70, false, false),
    CORE_MAINFRAME("CORE", 100, true, false)
}

data class HackNode(
    val id: Int,
    val type: HackNodeType,
    val x: Float, // 0.0 to 1.0 grid layout
    val y: Float,
    var currentHp: Int = type.baseIceHp,
    var isCaptured: Boolean = type.isAccessPoint,
    var isUnderAttack: Boolean = false,
    val connectedNodeIds: MutableList<Int> = mutableListOf()
)

data class HackingSession(
    val terminalId: Int,
    val sectorName: String,
    val iceLevel: Int,
    val nodes: List<HackNode>,
    var traceProgress: Float = 0f, // 0 to 100%
    val traceSpeedPerSec: Float = 6.0f + (iceLevel * 2.5f),
    var isSuccess: Boolean = false,
    var isFailed: Boolean = false,
    var selectedNodeId: Int = 0
) {
    fun attemptBreach(nodeId: Int, playerBreachPower: Int = 30): Boolean {
        if (isSuccess || isFailed) return false
        val targetNode = nodes.find { it.id == nodeId } ?: return false
        if (targetNode.isCaptured) return false

        // Must be adjacent to an already captured node
        val canReach = targetNode.connectedNodeIds.any { id ->
            nodes.find { it.id == id }?.isCaptured == true
        }
        if (!canReach) return false

        targetNode.currentHp -= playerBreachPower
        if (targetNode.currentHp <= 0) {
            targetNode.currentHp = 0
            targetNode.isCaptured = true
            if (targetNode.type.isRoot) {
                isSuccess = true
            }
        }
        return true
    }

    fun updateTick(deltaSec: Float) {
        if (isSuccess || isFailed) return
        traceProgress += traceSpeedPerSec * deltaSec
        if (traceProgress >= 100f) {
            traceProgress = 100f
            isFailed = true
        }
    }

    companion object {
        fun createSession(terminalId: Int, sectorName: String, iceLevel: Int): HackingSession {
            val rng = Random(terminalId.toLong() * 31337L)
            val nodes = mutableListOf<HackNode>()

            // Simple 5-8 node network graph
            val nodeCount = 5 + min(3, iceLevel)
            val access = HackNode(0, HackNodeType.ACCESS_PORT, 0.15f, 0.5f, isCaptured = true)
            nodes.add(access)

            val core = HackNode(nodeCount - 1, HackNodeType.CORE_MAINFRAME, 0.85f, 0.5f, currentHp = 60 + iceLevel * 20)

            for (i in 1 until nodeCount - 1) {
                val t = i.toFloat() / (nodeCount - 1)
                val nodeType = if (rng.nextBoolean()) HackNodeType.ICE_FIREWALL else HackNodeType.DATA_BUFFER
                val yOffset = if (i % 2 == 1) 0.25f + rng.nextFloat() * 0.15f else 0.65f + rng.nextFloat() * 0.15f
                nodes.add(
                    HackNode(
                        id = i,
                        type = nodeType,
                        x = 0.2f + t * 0.5f,
                        y = yOffset,
                        currentHp = nodeType.baseIceHp + (iceLevel * 10)
                    )
                )
            }
            nodes.add(core)

            // Connect nodes
            for (i in 0 until nodes.size - 1) {
                nodes[i].connectedNodeIds.add(nodes[i + 1].id)
                nodes[i + 1].connectedNodeIds.add(nodes[i].id)
            }
            // Add cross link if more than 4
            if (nodes.size >= 5) {
                nodes[1].connectedNodeIds.add(nodes[3].id)
                nodes[3].connectedNodeIds.add(nodes[1].id)
            }

            return HackingSession(
                terminalId = terminalId,
                sectorName = sectorName,
                iceLevel = iceLevel,
                nodes = nodes,
                selectedNodeId = 1
            )
        }
    }
}
