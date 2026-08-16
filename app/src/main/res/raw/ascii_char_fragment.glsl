#version 300 es
precision highp float;

// Interpolated inputs from Vertex Shader
in vec2 v_AtlasUV;
in vec4 v_FgColor;
in vec4 v_BgColor;
in vec2 v_ScreenUV;
in vec4 v_FxParams;

// Uniforms
uniform sampler2D u_GlyphAtlas;          // Monospace 16x16 glyph font atlas (Alpha/Red channel)
uniform vec2 u_ScreenResolution;         // Viewport width and height
uniform float u_ScanlineIntensity;       // Intensity of CRT scanlines (0.0 to 1.0)
uniform float u_BloomGlow;               // Phosphor glow multiplier
uniform float u_VignetteStrength;        // Edge darkening
uniform float u_Time;                    // Time in seconds
uniform int u_ThermalVisionMode;         // 1 if thermal optics active, 0 otherwise

// Fragment Output
out vec4 out_FragColor;

void main() {
    // 1. Sample character glyph mask from font texture atlas
    float glyphAlpha = texture(u_GlyphAtlas, v_AtlasUV).r;

    // Optional subtle chromatic edge sampling for cyber neon effect
    if (v_FxParams.x > 0.5) {
        float glyphAlphaR = texture(u_GlyphAtlas, v_AtlasUV + vec2(0.0008, 0.0)).r;
        float glyphAlphaB = texture(u_GlyphAtlas, v_AtlasUV - vec2(0.0008, 0.0)).r;
        glyphAlpha = (glyphAlpha * 0.7) + (glyphAlphaR * 0.15) + (glyphAlphaB * 0.15);
    }

    // 2. Per-Character Foreground & Background Tint Blending
    vec4 fg = v_FgColor;
    vec4 bg = v_BgColor;

    // Dynamic brightness boost from per-character FX params
    float brightnessBoost = 1.0 + (v_FxParams.x * u_BloomGlow * 0.6);
    vec3 boostedFgRgb = fg.rgb * brightnessBoost;

    // Alpha composite foreground glyph over background tint
    vec3 compositeRgb = mix(bg.rgb, boostedFgRgb, glyphAlpha * fg.a);
    float compositeAlpha = max(bg.a, glyphAlpha * fg.a);

    // 3. Cyber Thermal Vision Mode
    if (u_ThermalVisionMode == 1) {
        float luma = dot(compositeRgb, vec3(0.299, 0.587, 0.114));
        vec3 thermalCyan = vec3(0.0, 0.9, 1.0);
        vec3 thermalHot = vec3(1.0, 0.15, 0.3);
        compositeRgb = mix(thermalCyan * luma, thermalHot * (luma * 1.5), smoothstep(0.4, 0.9, luma));
    }

    // 4. CRT Horizontal Scanlines & Phosphor Grid
    if (u_ScanlineIntensity > 0.0) {
        float scanline = sin((v_ScreenUV.y * u_ScreenResolution.y * 1.5) + u_Time * 6.0) * 0.5 + 0.5;
        float scanlineFactor = mix(1.0, 0.85 + 0.15 * scanline, u_ScanlineIntensity);
        compositeRgb *= scanlineFactor;
    }

    // 5. Subtle Terminal Edge Vignette
    if (u_VignetteStrength > 0.0) {
        vec2 uv = v_ScreenUV * (vec2(1.0) - v_ScreenUV.yx);
        float vig = uv.x * uv.y * 15.0;
        vig = clamp(pow(vig, u_VignetteStrength * 0.25), 0.0, 1.0);
        compositeRgb *= vig;
    }

    out_FragColor = vec4(compositeRgb, compositeAlpha);
}
