#version 420 core

in vec2 uv;

uniform sampler2D tex;

layout (location = 0) out vec4 out_Color;
layout (location = 1) out vec4 out_Normal;
layout (location = 2) out vec4 out_Specular;
layout (location = 3) out vec4 out_Glow;

void main () {
    out_Color = texture(tex, uv);
    out_Glow = texture(tex, uv);
    out_Normal = vec4(0.0, 0.0, 0.0, 0.0);
    out_Specular = vec4(0.0, 0.0, 0.0, 0.0);
}