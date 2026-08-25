package com.example.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Encapsulates vertex data, normals, texture coordinates, and index buffers for 3D primitives.
 */
class Mesh3D(
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val texCoordBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int
) {
    fun release() {
        vertexBuffer.clear()
        normalBuffer.clear()
        texCoordBuffer.clear()
        indexBuffer.clear()
    }
}

object Primitives3DFactory {

    private fun createFloatBuffer(array: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(array)
                position(0)
            }
    }

    private fun createShortBuffer(array: ShortArray): ShortBuffer {
        return ByteBuffer.allocateDirect(array.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(array)
                position(0)
            }
    }

    /**
     * Creates a standard unit 3D Cube with normals and UVs (-0.5 to 0.5 on all axes).
     */
    fun createCubeMesh(size: Float = 1.0f): Mesh3D {
        val s = size * 0.5f

        val positions = floatArrayOf(
            // Front face
            -s, -s,  s,   s, -s,  s,   s,  s,  s,  -s,  s,  s,
            // Back face
             s, -s, -s,  -s, -s, -s,  -s,  s, -s,   s,  s, -s,
            // Top face
            -s,  s,  s,   s,  s,  s,   s,  s, -s,  -s,  s, -s,
            // Bottom face
            -s, -s, -s,   s, -s, -s,   s, -s,  s,  -s, -s,  s,
            // Right face
             s, -s,  s,   s, -s, -s,   s,  s, -s,   s,  s,  s,
            // Left face
            -s, -s, -s,  -s, -s,  s,  -s,  s,  s,  -s,  s, -s
        )

        val normals = floatArrayOf(
            // Front
            0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,
            // Back
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            // Top
            0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
            // Right
            1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,
            // Left
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f
        )

        val texCoords = floatArrayOf(
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f
        )

        val indices = shortArrayOf(
            0, 1, 2,     0, 2, 3,     // Front
            4, 5, 6,     4, 6, 7,     // Back
            8, 9, 10,    8, 10, 11,   // Top
            12, 13, 14,  12, 14, 15,  // Bottom
            16, 17, 18,  16, 18, 19,  // Right
            20, 21, 22,  20, 22, 23   // Left
        )

        return Mesh3D(
            createFloatBuffer(positions),
            createFloatBuffer(normals),
            createFloatBuffer(texCoords),
            createShortBuffer(indices),
            indices.size
        )
    }

    /**
     * Creates a 3D Pyramid / Diamond (ideal for Cyber Security Drones and energy beacons).
     */
    fun createPyramidMesh(width: Float = 1.0f, height: Float = 1.2f): Mesh3D {
        val hw = width * 0.5f
        val hh = height * 0.5f

        val positions = floatArrayOf(
            // 4 Side Triangles + Base (2 Triangles)
            // Front Face
            0f, hh, 0f,   -hw, -hh,  hw,    hw, -hh,  hw,
            // Right Face
            0f, hh, 0f,    hw, -hh,  hw,    hw, -hh, -hw,
            // Back Face
            0f, hh, 0f,    hw, -hh, -hw,   -hw, -hh, -hw,
            // Left Face
            0f, hh, 0f,   -hw, -hh, -hw,   -hw, -hh,  hw,
            // Base Face
            -hw, -hh, -hw,   hw, -hh, -hw,   hw, -hh,  hw,  -hw, -hh,  hw
        )

        val normals = floatArrayOf(
            // Front (approximate normal)
            0f, 0.5f, 0.86f,   0f, 0.5f, 0.86f,   0f, 0.5f, 0.86f,
            // Right
            0.86f, 0.5f, 0f,   0.86f, 0.5f, 0f,   0.86f, 0.5f, 0f,
            // Back
            0f, 0.5f, -0.86f,  0f, 0.5f, -0.86f,  0f, 0.5f, -0.86f,
            // Left
            -0.86f, 0.5f, 0f,  -0.86f, 0.5f, 0f,  -0.86f, 0.5f, 0f,
            // Base
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f
        )

        val texCoords = floatArrayOf(
            0.5f, 1f,  0f, 0f,  1f, 0f,
            0.5f, 1f,  0f, 0f,  1f, 0f,
            0.5f, 1f,  0f, 0f,  1f, 0f,
            0.5f, 1f,  0f, 0f,  1f, 0f,
            0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f
        )

        val indices = shortArrayOf(
            0, 1, 2,
            3, 4, 5,
            6, 7, 8,
            9, 10, 11,
            12, 13, 14,  12, 14, 15
        )

        return Mesh3D(
            createFloatBuffer(positions),
            createFloatBuffer(normals),
            createFloatBuffer(texCoords),
            createShortBuffer(indices),
            indices.size
        )
    }

    /**
     * Creates a 3D Cylinder / Column mesh (for server stacks, power conduits, pillars).
     */
    fun createCylinderMesh(radius: Float = 0.5f, height: Float = 1.5f, segments: Int = 16): Mesh3D {
        val halfH = height * 0.5f
        val posList = mutableListOf<Float>()
        val normList = mutableListOf<Float>()
        val uvList = mutableListOf<Float>()
        val idxList = mutableListOf<Short>()

        // Side vertices
        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * (2f * Math.PI.toFloat())
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            val nx = cos(angle)
            val nz = sin(angle)
            val u = i.toFloat() / segments

            // Top vertex
            posList.addAll(listOf(x, halfH, z))
            normList.addAll(listOf(nx, 0f, nz))
            uvList.addAll(listOf(u, 1f))

            // Bottom vertex
            posList.addAll(listOf(x, -halfH, z))
            normList.addAll(listOf(nx, 0f, nz))
            uvList.addAll(listOf(u, 0f))
        }

        // Side indices
        for (i in 0 until segments) {
            val top1 = (i * 2).toShort()
            val bot1 = (i * 2 + 1).toShort()
            val top2 = ((i + 1) * 2).toShort()
            val bot2 = ((i + 1) * 2 + 1).toShort()

            idxList.addAll(listOf(top1, bot1, top2))
            idxList.addAll(listOf(top2, bot1, bot2))
        }

        return Mesh3D(
            createFloatBuffer(posList.toFloatArray()),
            createFloatBuffer(normList.toFloatArray()),
            createFloatBuffer(uvList.toFloatArray()),
            createShortBuffer(idxList.toShortArray()),
            idxList.size
        )
    }

    /**
     * Creates a 3D Sphere mesh (for cyber cores, glowing plasma orbs, holographic globes).
     */
    fun createSphereMesh(radius: Float = 0.6f, stacks: Int = 12, slices: Int = 16): Mesh3D {
        val posList = mutableListOf<Float>()
        val normList = mutableListOf<Float>()
        val uvList = mutableListOf<Float>()
        val idxList = mutableListOf<Short>()

        for (i in 0..stacks) {
            val v = i.toFloat() / stacks
            val phi = v * Math.PI.toFloat()

            for (j in 0..slices) {
                val u = j.toFloat() / slices
                val theta = u * (2f * Math.PI.toFloat())

                val x = cos(theta) * sin(phi)
                val y = cos(phi)
                val z = sin(theta) * sin(phi)

                posList.addAll(listOf(x * radius, y * radius, z * radius))
                normList.addAll(listOf(x, y, z))
                uvList.addAll(listOf(u, v))
            }
        }

        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val first = (i * (slices + 1) + j).toShort()
                val second = (first + slices + 1).toShort()

                idxList.addAll(listOf(first, second, (first + 1).toShort()))
                idxList.addAll(listOf(second, (second + 1).toShort(), (first + 1).toShort()))
            }
        }

        return Mesh3D(
            createFloatBuffer(posList.toFloatArray()),
            createFloatBuffer(normList.toFloatArray()),
            createFloatBuffer(uvList.toFloatArray()),
            createShortBuffer(idxList.toShortArray()),
            idxList.size
        )
    }

    /**
     * Creates a 3D Torus mesh (for holographic orbital rings, HUD rings, security portals).
     */
    fun createTorusMesh(mainRadius: Float = 0.7f, tubeRadius: Float = 0.2f, mainSegments: Int = 16, tubeSegments: Int = 12): Mesh3D {
        val posList = mutableListOf<Float>()
        val normList = mutableListOf<Float>()
        val uvList = mutableListOf<Float>()
        val idxList = mutableListOf<Short>()

        for (i in 0..mainSegments) {
            val u = i.toFloat() / mainSegments
            val theta = u * (2f * Math.PI.toFloat())
            val cosTheta = cos(theta)
            val sinTheta = sin(theta)

            for (j in 0..tubeSegments) {
                val v = j.toFloat() / tubeSegments
                val phi = v * (2f * Math.PI.toFloat())
                val cosPhi = cos(phi)
                val sinPhi = sin(phi)

                val x = (mainRadius + tubeRadius * cosPhi) * cosTheta
                val y = tubeRadius * sinPhi
                val z = (mainRadius + tubeRadius * cosPhi) * sinTheta

                val nx = cosPhi * cosTheta
                val ny = sinPhi
                val nz = cosPhi * sinTheta

                posList.addAll(listOf(x, y, z))
                normList.addAll(listOf(nx, ny, nz))
                uvList.addAll(listOf(u, v))
            }
        }

        for (i in 0 until mainSegments) {
            for (j in 0 until tubeSegments) {
                val first = (i * (tubeSegments + 1) + j).toShort()
                val second = (first + tubeSegments + 1).toShort()

                idxList.addAll(listOf(first, second, (first + 1).toShort()))
                idxList.addAll(listOf(second, (second + 1).toShort(), (first + 1).toShort()))
            }
        }

        return Mesh3D(
            createFloatBuffer(posList.toFloatArray()),
            createFloatBuffer(normList.toFloatArray()),
            createFloatBuffer(uvList.toFloatArray()),
            createShortBuffer(idxList.toShortArray()),
            idxList.size
        )
    }

    /**
     * Fullscreen quad covering NDC coordinates [-1, 1] for post-processing shaders.
     */
    fun createFullscreenQuad(): Mesh3D {
        val positions = floatArrayOf(
            -1f, -1f, 0f,
             1f, -1f, 0f,
             1f,  1f, 0f,
            -1f,  1f, 0f
        )

        val normals = floatArrayOf(
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f
        )

        val texCoords = floatArrayOf(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        )

        val indices = shortArrayOf(
            0, 1, 2,
            0, 2, 3
        )

        return Mesh3D(
            createFloatBuffer(positions),
            createFloatBuffer(normals),
            createFloatBuffer(texCoords),
            createShortBuffer(indices),
            indices.size
        )
    }
}
