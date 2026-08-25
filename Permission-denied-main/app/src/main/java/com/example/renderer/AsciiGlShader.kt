package com.example.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * High-performance OpenGL ES character-based shader pipeline.
 * Features per-character foreground color, background tinting, font texture atlas,
 * CRT scanlines, cyber bloom glow, and thermal vision post-processing.
 */
class AsciiGlShader(
    val context: Context? = null
) {
    companion object {
        private const val TAG = "AsciiGlShader"

        const val VERTEX_SHADER_SOURCE = """#version 300 es
precision highp float;

layout(location = 0) in vec2 a_Position;
layout(location = 1) in vec2 a_TexCoord;
layout(location = 2) in float a_GlyphIndex;
layout(location = 3) in vec4 a_FgColor;
layout(location = 4) in vec4 a_BgColor;
layout(location = 5) in vec4 a_FxParams;

uniform mat4 u_MVPMatrix;
uniform vec2 u_GridDim;
uniform vec2 u_AtlasGridDim;
uniform float u_Time;

out vec2 v_AtlasUV;
out vec4 v_FgColor;
out vec4 v_BgColor;
out vec2 v_ScreenUV;
out vec4 v_FxParams;

void main() {
    float charCode = clamp(a_GlyphIndex, 0.0, 255.0);
    float atlasCols = u_AtlasGridDim.x;
    float atlasRows = u_AtlasGridDim.y;

    float cellX = mod(charCode, atlasCols);
    float cellY = floor(charCode / atlasCols);

    vec2 cellSize = vec2(1.0 / atlasCols, 1.0 / atlasRows);
    vec2 glyphOffset = vec2(cellX * cellSize.x, cellY * cellSize.y);

    vec2 padding = vec2(0.001, 0.001);
    vec2 clampedTexCoord = clamp(a_TexCoord, padding, vec2(1.0) - padding);

    v_AtlasUV = glyphOffset + (clampedTexCoord * cellSize);
    v_FgColor = a_FgColor;
    v_BgColor = a_BgColor;
    v_FxParams = a_FxParams;
    v_ScreenUV = (a_Position * 0.5) + vec2(0.5, 0.5);

    vec2 pos = a_Position;
    if (a_FxParams.y > 0.0) {
        float jitter = sin(u_Time * 45.0 + pos.y * 30.0) * a_FxParams.y * 0.004;
        pos.x += jitter;
    }

    gl_Position = u_MVPMatrix * vec4(pos, 0.0, 1.0);
}
"""

        const val FRAGMENT_SHADER_SOURCE = """#version 300 es
precision highp float;

in vec2 v_AtlasUV;
in vec4 v_FgColor;
in vec4 v_BgColor;
in vec2 v_ScreenUV;
in vec4 v_FxParams;

uniform sampler2D u_GlyphAtlas;
uniform vec2 u_ScreenResolution;
uniform float u_ScanlineIntensity;
uniform float u_BloomGlow;
uniform float u_VignetteStrength;
uniform float u_Time;
uniform int u_ThermalVisionMode;

out vec4 out_FragColor;

void main() {
    float glyphAlpha = texture(u_GlyphAtlas, v_AtlasUV).r;

    // Subtle Chromatic Aberration sampling on active characters
    vec3 glyphAlphaRgb = vec3(glyphAlpha);
    if (v_FxParams.x > 0.0) {
        float chromShift = 0.0010 * v_FxParams.x;
        float glyphAlphaR = texture(u_GlyphAtlas, v_AtlasUV + vec2(chromShift, 0.0)).r;
        float glyphAlphaB = texture(u_GlyphAtlas, v_AtlasUV - vec2(chromShift, 0.0)).r;
        glyphAlphaRgb = vec3(glyphAlphaR, glyphAlpha, glyphAlphaB);
        glyphAlpha = (glyphAlphaR + glyphAlpha + glyphAlphaB) * 0.3333;
    }

    vec4 fg = v_FgColor;
    vec4 bg = v_BgColor;

    float brightnessBoost = 1.0 + (v_FxParams.x * u_BloomGlow * 0.6);
    vec3 boostedFgRgb = fg.rgb * brightnessBoost;

    vec3 compositeRgb = mix(bg.rgb, boostedFgRgb, glyphAlphaRgb * fg.a);
    float compositeAlpha = max(bg.a, glyphAlpha * fg.a);

    if (u_ThermalVisionMode == 1) {
        float luma = dot(compositeRgb, vec3(0.299, 0.587, 0.114));
        vec3 thermalCyan = vec3(0.0, 0.9, 1.0);
        vec3 thermalHot = vec3(1.0, 0.15, 0.3);
        compositeRgb = mix(thermalCyan * luma, thermalHot * (luma * 1.5), smoothstep(0.4, 0.9, luma));
    }

    // Subtle CRT Scanlines with Cathode Refresh Beam
    if (u_ScanlineIntensity > 0.0) {
        float linePattern = sin(v_ScreenUV.y * u_ScreenResolution.y * 3.14159265) * 0.5 + 0.5;
        float rollBeam = sin(v_ScreenUV.y * 5.0 - u_Time * 2.0) * 0.5 + 0.5;
        float scanlineFactor = mix(1.0, (0.84 + 0.16 * linePattern) * (0.97 + 0.03 * rollBeam), u_ScanlineIntensity);
        compositeRgb *= scanlineFactor;
    }

    if (u_VignetteStrength > 0.0) {
        vec2 uv = v_ScreenUV * (vec2(1.0) - v_ScreenUV.yx);
        float vig = uv.x * uv.y * 15.0;
        vig = clamp(pow(vig, u_VignetteStrength * 0.25), 0.0, 1.0);
        compositeRgb *= vig;
    }

    out_FragColor = vec4(compositeRgb, compositeAlpha);
}
"""
    }

    var programId: Int = 0
        private set
    var atlasTextureId: Int = 0
        private set

    // Uniform locations
    private var uMVPMatrixLoc: Int = -1
    private var uGridDimLoc: Int = -1
    private var uAtlasGridDimLoc: Int = -1
    private var uTimeLoc: Int = -1
    private var uGlyphAtlasLoc: Int = -1
    private var uScreenResolutionLoc: Int = -1
    private var uScanlineIntensityLoc: Int = -1
    private var uBloomGlowLoc: Int = -1
    private var uVignetteStrengthLoc: Int = -1
    private var uThermalVisionModeLoc: Int = -1

    var isInitialized = false
        private set

    fun initialize(): Boolean {
        if (isInitialized) return true

        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE)

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Failed to compile character shaders")
            return false
        }

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Shader Link Error: " + GLES30.glGetProgramInfoLog(programId))
            GLES30.glDeleteProgram(programId)
            programId = 0
            return false
        }

        // Cache uniform locations
        uMVPMatrixLoc = GLES30.glGetUniformLocation(programId, "u_MVPMatrix")
        uGridDimLoc = GLES30.glGetUniformLocation(programId, "u_GridDim")
        uAtlasGridDimLoc = GLES30.glGetUniformLocation(programId, "u_AtlasGridDim")
        uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time")
        uGlyphAtlasLoc = GLES30.glGetUniformLocation(programId, "u_GlyphAtlas")
        uScreenResolutionLoc = GLES30.glGetUniformLocation(programId, "u_ScreenResolution")
        uScanlineIntensityLoc = GLES30.glGetUniformLocation(programId, "u_ScanlineIntensity")
        uBloomGlowLoc = GLES30.glGetUniformLocation(programId, "u_BloomGlow")
        uVignetteStrengthLoc = GLES30.glGetUniformLocation(programId, "u_VignetteStrength")
        uThermalVisionModeLoc = GLES30.glGetUniformLocation(programId, "u_ThermalVisionMode")

        // Build 16x16 Monospace Glyph Texture Atlas
        atlasTextureId = generateGlyphAtlasTexture()

        isInitialized = true
        return true
    }

    /**
     * Creates a 512x512 bitmap containing a 16x16 grid of all 256 ASCII / CP437 monospace glyphs.
     */
    fun generateGlyphAtlasBitmap(): Bitmap {
        val atlasSize = 512
        val cellSize = atlasSize / 16 // 32x32 pixels per glyph
        val bitmap = Bitmap.createBitmap(atlasSize, atlasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = cellSize * 0.78f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val fontMetrics = paint.fontMetrics
        val textBaseline = (cellSize - (fontMetrics.ascent + fontMetrics.descent)) / 2.0f

        val charBuf = CharArray(1)
        for (i in 0 until 256) {
            val cellX = (i % 16) * cellSize
            val cellY = (i / 16) * cellSize

            charBuf[0] = if (i < 32 || i == 127) ' ' else i.toChar()
            val drawX = cellX + cellSize * 0.5f
            val drawY = cellY + textBaseline
            canvas.drawText(charBuf, 0, 1, drawX, drawY, paint)
        }

        return bitmap
    }

    private fun generateGlyphAtlasTexture(): Int {
        val bitmap = generateGlyphAtlasBitmap()
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val texId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        return texId
    }

    fun use(
        mvpMatrix: FloatArray,
        cols: Float,
        rows: Float,
        screenWidth: Float,
        screenHeight: Float,
        timeSec: Float,
        scanlineIntensity: Float = 0.35f,
        bloomGlow: Float = 1.0f,
        vignetteStrength: Float = 0.8f,
        isThermalMode: Boolean = false
    ) {
        if (!isInitialized) initialize()
        if (programId == 0) return

        GLES30.glUseProgram(programId)

        // Bind font texture atlas to Texture Unit 0
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlasTextureId)
        GLES30.glUniform1i(uGlyphAtlasLoc, 0)

        // Set Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform2f(uGridDimLoc, cols, rows)
        GLES30.glUniform2f(uAtlasGridDimLoc, 16f, 16f)
        GLES30.glUniform1f(uTimeLoc, timeSec)
        GLES30.glUniform2f(uScreenResolutionLoc, screenWidth, screenHeight)
        GLES30.glUniform1f(uScanlineIntensityLoc, scanlineIntensity)
        GLES30.glUniform1f(uBloomGlowLoc, bloomGlow)
        GLES30.glUniform1f(uVignetteStrengthLoc, vignetteStrength)
        GLES30.glUniform1i(uThermalVisionModeLoc, if (isThermalMode) 1 else 0)
    }

    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
        if (atlasTextureId != 0) {
            val textures = intArrayOf(atlasTextureId)
            GLES30.glDeleteTextures(1, textures, 0)
            atlasTextureId = 0
        }
        isInitialized = false
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) return 0
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compilation failed ($type): " + GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
