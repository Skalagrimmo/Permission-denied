#version 300 es
precision highp float;

// Vertex Attributes
layout(location = 0) in vec2 a_Position;       // Normalized quad position [-1.0, 1.0] or cell quad [0, 1]
layout(location = 1) in vec2 a_TexCoord;       // Base UV coordinates [0.0, 1.0]
layout(location = 2) in float a_GlyphIndex;    // ASCII character code (0 - 255)
layout(location = 3) in vec4 a_FgColor;        // Per-character foreground RGBA color
layout(location = 4) in vec4 a_BgColor;        // Per-character background tint RGBA color
layout(location = 5) in vec4 a_FxParams;       // x = glow/brightness, y = glitch offset, z = flicker, w = reserved

// Uniforms
uniform mat4 u_MVPMatrix;
uniform vec2 u_GridDim;         // Columns and Rows (e.g. 80.0, 45.0)
uniform vec2 u_AtlasGridDim;    // Atlas subdivisions (e.g. 16.0, 16.0 for 256 chars)
uniform float u_Time;           // Animation time in seconds

// Varying Outputs to Fragment Shader
out vec2 v_AtlasUV;
out vec4 v_FgColor;
out vec4 v_BgColor;
out vec2 v_ScreenUV;
out vec4 v_FxParams;

void main() {
    // 1. Calculate Glyph Atlas UV coordinates
    float charCode = clamp(a_GlyphIndex, 0.0, 255.0);
    float atlasCols = u_AtlasGridDim.x;
    float atlasRows = u_AtlasGridDim.y;

    float cellX = mod(charCode, atlasCols);
    float cellY = floor(charCode / atlasCols);

    vec2 cellSize = vec2(1.0 / atlasCols, 1.0 / atlasRows);
    vec2 glyphOffset = vec2(cellX * cellSize.x, cellY * cellSize.y);

    // Sub-pixel padding to avoid bleeding between atlas glyph cells
    vec2 padding = vec2(0.001, 0.001);
    vec2 clampedTexCoord = clamp(a_TexCoord, padding, vec2(1.0) - padding);

    v_AtlasUV = glyphOffset + (clampedTexCoord * cellSize);

    // 2. Pass Colors and Parameters
    v_FgColor = a_FgColor;
    v_BgColor = a_BgColor;
    v_FxParams = a_FxParams;
    v_ScreenUV = (a_Position * 0.5) + vec2(0.5, 0.5);

    // 3. Optional vertex jitter for digital EMP / cyber glitch FX
    vec2 pos = a_Position;
    if (a_FxParams.y > 0.0) {
        float jitter = sin(u_Time * 45.0 + pos.y * 30.0) * a_FxParams.y * 0.004;
        pos.x += jitter;
    }

    // 4. Output Transformed Position
    gl_Position = u_MVPMatrix * vec4(pos, 0.0, 1.0);
}
