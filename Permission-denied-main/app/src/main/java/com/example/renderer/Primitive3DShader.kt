package com.example.renderer

import android.opengl.GLES20
import android.util.Log

/**
 * OpenGL ES 2.0 / 3.0 shader for rendering 3D primitives with cyberpunk lighting,
 * neon emissive glow, procedural wireframe grids, and material properties.
 */
class Primitive3DShader {
    companion object {
        private const val TAG = "Primitive3DShader"

        const val VERTEX_SHADER = """
uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewMatrix;
uniform mat4 u_ProjectionMatrix;
uniform mat4 u_NormalMatrix;

attribute vec3 a_Position;
attribute vec3 a_Normal;
attribute vec2 a_TexCoord;

varying vec3 v_PositionWS;
varying vec3 v_NormalWS;
varying vec2 v_TexCoord;

void main() {
    vec4 posWS = u_ModelMatrix * vec4(a_Position, 1.0);
    v_PositionWS = posWS.xyz;
    v_NormalWS = normalize((u_NormalMatrix * vec4(a_Normal, 0.0)).xyz);
    v_TexCoord = a_TexCoord;
    gl_Position = u_ProjectionMatrix * u_ViewMatrix * posWS;
}
"""

        const val FRAGMENT_SHADER = """
precision mediump float;

uniform vec3 u_CameraPos;
uniform vec3 u_LightPos;
uniform vec3 u_LightColor;
uniform vec3 u_AmbientColor;
uniform vec4 u_MaterialColor;
uniform vec3 u_EmissiveColor;
uniform float u_SpecularPower;
uniform float u_WireframeGrid;
uniform float u_Time;

varying vec3 v_PositionWS;
varying vec3 v_NormalWS;
varying vec2 v_TexCoord;

void main() {
    vec3 N = normalize(v_NormalWS);
    vec3 L = normalize(u_LightPos - v_PositionWS);
    vec3 V = normalize(u_CameraPos - v_PositionWS);
    vec3 H = normalize(L + V);

    // Diffuse Lambertian
    float diff = max(dot(N, L), 0.0);
    vec3 diffuse = diff * u_LightColor;

    // Specular Blinn-Phong
    float spec = pow(max(dot(N, H), 0.0), u_SpecularPower);
    vec3 specular = spec * u_LightColor * 0.7;

    // Ambient
    vec3 ambient = u_AmbientColor;

    // Base color
    vec3 baseRgb = u_MaterialColor.rgb;

    // Cyberpunk grid / wireframe pulse overlay if enabled
    if (u_WireframeGrid > 0.5) {
        vec2 grid = abs(fract(v_TexCoord * 8.0) - 0.5);
        float line = min(grid.x, grid.y);
        float c = smoothstep(0.06, 0.0, line);
        baseRgb = mix(baseRgb, vec3(0.0, 0.95, 1.0), c * 0.85);
    }

    // Composite Lighting + Emissive Cyber Glow
    vec3 finalColor = (ambient + diffuse) * baseRgb + specular + u_EmissiveColor;

    gl_FragColor = vec4(finalColor, u_MaterialColor.a);
}
"""
    }

    var programId: Int = 0
        private set

    // Attributes
    var aPositionLoc: Int = -1
        private set
    var aNormalLoc: Int = -1
        private set
    var aTexCoordLoc: Int = -1
        private set

    // Uniforms
    var uModelMatrixLoc: Int = -1
        private set
    var uViewMatrixLoc: Int = -1
        private set
    var uProjectionMatrixLoc: Int = -1
        private set
    var uNormalMatrixLoc: Int = -1
        private set
    var uCameraPosLoc: Int = -1
        private set
    var uLightPosLoc: Int = -1
        private set
    var uLightColorLoc: Int = -1
        private set
    var uAmbientColorLoc: Int = -1
        private set
    var uMaterialColorLoc: Int = -1
        private set
    var uEmissiveColorLoc: Int = -1
        private set
    var uSpecularPowerLoc: Int = -1
        private set
    var uWireframeGridLoc: Int = -1
        private set
    var uTimeLoc: Int = -1
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
            Log.e(TAG, "Link error: " + GLES20.glGetProgramInfoLog(programId))
            GLES20.glDeleteProgram(programId)
            programId = 0
            return false
        }

        // Cache locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "a_Normal")
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord")

        uModelMatrixLoc = GLES20.glGetUniformLocation(programId, "u_ModelMatrix")
        uViewMatrixLoc = GLES20.glGetUniformLocation(programId, "u_ViewMatrix")
        uProjectionMatrixLoc = GLES20.glGetUniformLocation(programId, "u_ProjectionMatrix")
        uNormalMatrixLoc = GLES20.glGetUniformLocation(programId, "u_NormalMatrix")
        uCameraPosLoc = GLES20.glGetUniformLocation(programId, "u_CameraPos")
        uLightPosLoc = GLES20.glGetUniformLocation(programId, "u_LightPos")
        uLightColorLoc = GLES20.glGetUniformLocation(programId, "u_LightColor")
        uAmbientColorLoc = GLES20.glGetUniformLocation(programId, "u_AmbientColor")
        uMaterialColorLoc = GLES20.glGetUniformLocation(programId, "u_MaterialColor")
        uEmissiveColorLoc = GLES20.glGetUniformLocation(programId, "u_EmissiveColor")
        uSpecularPowerLoc = GLES20.glGetUniformLocation(programId, "u_SpecularPower")
        uWireframeGridLoc = GLES20.glGetUniformLocation(programId, "u_WireframeGrid")
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time")

        isInitialized = true
        return true
    }

    fun use() {
        if (!isInitialized) initialize()
        if (programId != 0) {
            GLES20.glUseProgram(programId)
        }
    }

    fun release() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
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
