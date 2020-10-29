#version 420 core

in vec2 uv;

uniform sampler2D tex;
uniform vec3 color;
uniform vec2 glyphSize;
uniform int textLength;
uniform vec2[512] text;

out vec4 outColor;

void main () {
    vec2 position = floor(uv / glyphSize);
    int i = int(position.x);
    if (i >= textLength) return;
    if (text[i].x < 0.0) return;
    vec2 localUV = uv - position * glyphSize;
    vec2 glyph = text[i] + localUV;
    outColor = vec4(color, 1.0) * texture(tex, glyph);
}