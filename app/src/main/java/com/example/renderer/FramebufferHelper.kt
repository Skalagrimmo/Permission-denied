package com.example.renderer

import android.opengl.GLES20
import android.util.Log

/**
 * Manages an offscreen Framebuffer Object (FBO) with color texture and depth renderbuffer.
 */
class FramebufferHelper {
    companion object {
        private const val TAG = "FramebufferHelper"
    }

    var framebufferId: Int = 0
        private set
    var colorTextureId: Int = 0
        private set
    var depthRenderbufferId: Int = 0
        private set

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    fun setup(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        if (width == w && height == h && framebufferId != 0) return

        release()

        width = w
        height = h

        // 1. Generate Framebuffer
        val fbos = IntArray(1)
        GLES20.glGenFramebuffers(1, fbos, 0)
        framebufferId = fbos[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)

        // 2. Generate Color Texture Attachment
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        colorTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            colorTextureId,
            0
        )

        // 3. Generate Depth Renderbuffer
        val rbos = IntArray(1)
        GLES20.glGenRenderbuffers(1, rbos, 0)
        depthRenderbufferId = rbos[0]
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRenderbufferId)
        GLES20.glRenderbufferStorage(
            GLES20.GL_RENDERBUFFER,
            GLES20.GL_DEPTH_COMPONENT16,
            width,
            height
        )
        GLES20.glFramebufferRenderbuffer(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_RENDERBUFFER,
            depthRenderbufferId
        )

        // 4. Verify FBO completeness
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer not complete! Status: $status")
        }

        // Unbind back to default framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        if (framebufferId != 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId)
            GLES20.glViewport(0, 0, width, height)
        }
    }

    fun unbind(defaultWidth: Int, defaultHeight: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, defaultWidth, defaultHeight)
    }

    fun release() {
        if (framebufferId != 0) {
            val fbos = intArrayOf(framebufferId)
            GLES20.glDeleteFramebuffers(1, fbos, 0)
            framebufferId = 0
        }
        if (colorTextureId != 0) {
            val textures = intArrayOf(colorTextureId)
            GLES20.glDeleteTextures(1, textures, 0)
            colorTextureId = 0
        }
        if (depthRenderbufferId != 0) {
            val rbos = intArrayOf(depthRenderbufferId)
            GLES20.glDeleteRenderbuffers(1, rbos, 0)
            depthRenderbufferId = 0
        }
        width = 0
        height = 0
    }
}
