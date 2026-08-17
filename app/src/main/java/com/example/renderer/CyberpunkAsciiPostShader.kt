package com.example.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log

/**
 * Cyberpunk ASCII-Art Post-Processing Shader.
 * Takes the rendered 3D primitive scene texture from the FBO and executes:
 * - Monospace character cell quantization
 * - Edge detection for crisp mechanical ASCII outlines (+, |, -, /, #)
 * - Glyph texture atlas sampling
 * - Cyberpunk chromatic aberration (glitch FX)
 * - CRT scanline phosphor simulation
 * - Neon bloom glow & vignette
 * - Thermal vision and ANSI palette quantizer
 */
class CyberpunkAsciiPostShader {
    companion object {
        private const val TAG = "CyberpunkAsciiPostShader"

        const val VERTEX_SHADER = """
attribute vec3 a_Position;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;

void main() {
    v_TexCoord = a_TexCoord;
    gl_Position = vec4(a_Position, 1.0);
}
"""

        const val FRAGMENT_SHADER = """
precision mediump float;

uniform sampler2D u_SceneTexture;
uniform sampler2D u_GlyphAtlas;
uniform vec2 u_ScreenResolution;
uniform vec2 u_GridDim;           // e.g. (96.0, 54.0) character cells
uniform float u_Time;
uniform float u_ScanlineStrength;
uniform float u_BloomGlow;
uniform float u_ChromaticAberration;
uniform int u_ThermalVision;
uniform int u_AnsiMode;           // 0: Full RGB, 1: ANSI-16, 2: Cyber Game Palette

varying vec2 v_TexCoord;

// Character ramp glyph indices (indices 0..15 in 16x16 font atlas)
// ' ', '.', ':', '-', '=', '+', '*', '%', '#', '@'
float mapLuminanceToGlyph(float luma, float isEdge) {
    if (isEdge > 0.35) {
        return 35.0; // '#' for high-contrast 3D mesh edges
    }
    if (luma < 0.08) return 32.0;  // ' '
    if (luma < 0.18) return 46.0;  // '.'
    if (luma < 0.28) return 58.0;  // ':'
    if (luma < 0.38) return 45.0;  // '-'
    if (luma < 0.48) return 61.0;  // '='
    if (luma < 0.60) return 43.0;  // '+'
    if (luma < 0.72) return 42.0;  // '*'
    if (luma < 0.84) return 37.0;  // '%'
    if (luma < 0.94) return 35.0;  // '#'
    return 64.0;                   // '@'
}

vec3 quantizeColor(vec3 color, int mode) {
    if (mode == 1) {
        // ANSI 16 approx
        return floor(color * 3.0 + 0.5) / 3.0;
    } else if (mode == 2) {
        // Cyber Neon Palette snapping
        float luma = dot(color, vec3(0.299, 0.587, 0.114));
        vec3 cyan = vec3(0.0, 0.94, 1.0);
        vec3 green = vec3(0.0, 1.0, 0.4);
        vec3 magenta = vec3(1.0, 0.0, 0.47);
        vec3 amber = vec3(1.0, 0.72, 0.0);
        if (luma < 0.2) return vec3(0.02, 0.04, 0.08);
        if (color.r > color.g && color.r > color.b) return mix(magenta, amber, 0.5);
        if (color.g > color.r && color.g > color.b) return green;
        return cyan;
    }
    return color;
}

void main() {
    vec2 uv = v_TexCoord;

    // 1. Grid cell quantization
    vec2 cellCount = u_GridDim;
    vec2 cellIndex = floor(uv * cellCount);
    vec2 cellCenterUV = (cellIndex + vec2(0.5, 0.5)) / cellCount;
    vec2 localUV = fract(uv * cellCount);

    // 2. Subtle Radial & Directional Chromatic Aberration
    vec2 centeredUV = uv - vec2(0.5, 0.5);
    float radialDist = length(centeredUV);
    vec2 radialOffset = centeredUV * (radialDist * radialDist) * (u_ChromaticAberration * 0.012);
    vec2 dirOffset = vec2(u_ChromaticAberration * 0.0035, 0.0);
    vec2 totalShift = radialOffset + dirOffset;

    float r = texture2D(u_SceneTexture, cellCenterUV + totalShift).r;
    float g = texture2D(u_SceneTexture, cellCenterUV).g;
    float b = texture2D(u_SceneTexture, cellCenterUV - totalShift).b;
    vec3 sceneColor = vec3(r, g, b);

    // 3. Sobel Edge Detection for 3D Geometry Silhouette Outlining
    vec2 texel = 1.0 / u_GridDim;
    float cN = dot(texture2D(u_SceneTexture, cellCenterUV + vec2(0.0, texel.y)).rgb, vec3(0.299, 0.587, 0.114));
    float cS = dot(texture2D(u_SceneTexture, cellCenterUV - vec2(0.0, texel.y)).rgb, vec3(0.299, 0.587, 0.114));
    float cE = dot(texture2D(u_SceneTexture, cellCenterUV + vec2(texel.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
    float cW = dot(texture2D(u_SceneTexture, cellCenterUV - vec2(texel.x, 0.0)).rgb, vec3(0.299, 0.587, 0.114));
    float edge = length(vec2(cE - cW, cN - cS));

    // 4. Luminance calculation
    float luma = dot(sceneColor, vec3(0.299, 0.587, 0.114));

    // 5. Select character code from glyph atlas (16x16 atlas)
    float charCode = mapLuminanceToGlyph(luma, edge);
    float atlasCols = 16.0;
    float atlasRows = 16.0;
    float col = mod(charCode, atlasCols);
    float row = floor(charCode / atlasCols);

    vec2 glyphCellSize = vec2(1.0 / atlasCols, 1.0 / atlasRows);
    vec2 glyphUV = vec2(col * glyphCellSize.x, row * glyphCellSize.y) + (localUV * glyphCellSize);
    
    // Subpixel glyph sampling for subtle text edge fringing
    float glyphShift = u_ChromaticAberration * 0.0008;
    float glyphAlphaR = texture2D(u_GlyphAtlas, glyphUV + vec2(glyphShift, 0.0)).r;
    float glyphAlphaG = texture2D(u_GlyphAtlas, glyphUV).r;
    float glyphAlphaB = texture2D(u_GlyphAtlas, glyphUV - vec2(glyphShift, 0.0)).r;
    vec3 glyphAlphaRgb = vec3(glyphAlphaR, glyphAlphaG, glyphAlphaB);
    float glyphAlpha = (glyphAlphaR + glyphAlphaG + glyphAlphaB) / 3.0;

    // 6. Color Processing & Quantization
    vec3 finalColor = quantizeColor(sceneColor, u_AnsiMode);

    // Thermal Vision Mode
    if (u_ThermalVision == 1) {
        vec3 thermalCyan = vec3(0.0, 0.9, 1.0);
        vec3 thermalHot = vec3(1.0, 0.15, 0.3);
        finalColor = mix(thermalCyan * luma, thermalHot * (luma * 1.5), smoothstep(0.4, 0.9, luma));
    }

    // 7. Foreground Glyph composite with Ambient Dark Background
    vec3 bgColor = finalColor * 0.12; // Dim phosphor backing
    vec3 fgColor = finalColor * (1.0 + u_BloomGlow * 0.5); // Neon glow boost
    vec3 compColor = mix(bgColor, fgColor, glyphAlphaRgb);

    // 8. Subtle CRT Scanlines & Cathode Refresh Beam
    if (u_ScanlineStrength > 0.0) {
        // Pixel-frequency scanline raster
        float linePattern = sin(uv.y * u_ScreenResolution.y * 3.14159265) * 0.5 + 0.5;
        // Subtle rolling vertical cathode beam
        float rollBeam = sin(uv.y * 6.0 - u_Time * 2.5) * 0.5 + 0.5;
        // Phosphor decay micro-flicker
        float microFlicker = sin(u_Time * 30.0) * 0.012 + 0.988;

        float scanFactor = mix(1.0, (0.82 + 0.18 * linePattern) * (0.96 + 0.04 * rollBeam), u_ScanlineStrength) * microFlicker;
        compColor *= scanFactor;
    }

    // 9. Cyber Vignette
    vec2 vigUV = uv * (vec2(1.0) - uv.yx);
    float vig = vigUV.x * vigUV.y * 15.0;
    vig = clamp(pow(vig, 0.25), 0.0, 1.0);
    compColor *= vig;

    gl_FragColor = vec4(compColor, 1.0);
}
"""
    }

    var programId: Int = 0
        private set
    var glyphAtlasTextureId: Int = 0
        private set

    // Attributes
    var aPositionLoc: Int = -1
        private set
    var aTexCoordLoc: Int = -1
        private set

    // Uniforms
    var uSceneTextureLoc: Int = -1
        private set
    var uGlyphAtlasLoc: Int = -1
        private set
    var uScreenResolutionLoc: Int = -1
        private set
    var uGridDimLoc: Int = -1
        private set
    var uTimeLoc: Int = -1
        private set
    var uScanlineStrengthLoc: Int = -1
        private set
    var uBloomGlowLoc: Int = -1
        private set
    var uChromaticAberrationLoc: Int = -1
        private set
    var uThermalVisionLoc: Int = -1
        private set
    var uAnsiModeLoc: Int = -1
        private set

    var isInitialized = false
        private set

    fun initialize(): Boolean {
        if (isInitialized) return true

        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (vShader == 0 || fShader == 0) return false

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vShader)
        GLES20.glAttachShader(programId, fShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Post-process link error: " + GLES20.glGetProgramInfoLog(programId))
            GLES20.glDeleteProgram(programId)
            programId = 0
            return false
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position")
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord")

        uSceneTextureLoc = GLES20.glGetUniformLocation(programId, "u_SceneTexture")
        uGlyphAtlasLoc = GLES20.glGetUniformLocation(programId, "u_GlyphAtlas")
        uScreenResolutionLoc = GLES20.glGetUniformLocation(programId, "u_ScreenResolution")
        uGridDimLoc = GLES20.glGetUniformLocation(programId, "u_GridDim")
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time")
        uScanlineStrengthLoc = GLES20.glGetUniformLocation(programId, "u_ScanlineStrength")
        uBloomGlowLoc = GLES20.glGetUniformLocation(programId, "u_BloomGlow")
        uChromaticAberrationLoc = GLES20.glGetUniformLocation(programId, "u_ChromaticAberration")
        uThermalVisionLoc = GLES20.glGetUniformLocation(programId, "u_ThermalVision")
        uAnsiModeLoc = GLES20.glGetUniformLocation(programId, "u_AnsiMode")

        glyphAtlasTextureId = createGlyphAtlasTexture()

        isInitialized = true
        return true
    }

    private fun createGlyphAtlasTexture(): Int {
        val size = 512
        val cellSize = size / 16
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = cellSize * 0.8f
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

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val texId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        return texId
    }

    fun use(
        sceneTexId: Int,
        screenWidth: Float,
        screenHeight: Float,
        gridCols: Float,
        gridRows: Float,
        timeSec: Float,
        scanline: Float = 0.35f,
        bloom: Float = 1.0f,
        chromatic: Float = 1.0f,
        isThermal: Boolean = false,
        ansiModeInt: Int = 2
    ) {
        if (!isInitialized) initialize()
        if (programId == 0) return

        GLES20.glUseProgram(programId)

        // Bind Scene Color Texture to Texture 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sceneTexId)
        GLES20.glUniform1i(uSceneTextureLoc, 0)

        // Bind Glyph Atlas Texture to Texture 1
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glyphAtlasTextureId)
        GLES20.glUniform1i(uGlyphAtlasLoc, 1)

        // Uniforms
        GLES20.glUniform2f(uScreenResolutionLoc, screenWidth, screenHeight)
        GLES20.glUniform2f(uGridDimLoc, gridCols, gridRows)
        GLES20.glUniform1f(uTimeLoc, timeSec)
        GLES20.glUniform1f(uScanlineStrengthLoc, scanline)
        GLES20.glUniform1f(uBloomGlowLoc, bloom)
        GLES20.glUniform1f(uChromaticAberrationLoc, chromatic)
        GLES20.glUniform1i(uThermalVisionLoc, if (isThermal) 1 else 0)
        GLES20.glUniform1i(uAnsiModeLoc, ansiModeInt)
    }

    fun release() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
        if (glyphAtlasTextureId != 0) {
            val textures = intArrayOf(glyphAtlasTextureId)
            GLES20.glDeleteTextures(1, textures, 0)
            glyphAtlasTextureId = 0
        }
        isInitialized = false
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compilation failed ($type): " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
