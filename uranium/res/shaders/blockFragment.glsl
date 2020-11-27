#version 420 core

in vec2 atlasUV;
in vec2 uv;
in vec3 normal;

layout (binding = 0) uniform sampler2D albedo;
layout (binding = 1) uniform sampler2D emission;
layout (binding = 2) uniform sampler2D specular;

layout (location = 0) out vec4 out_Color;
layout (location = 1) out vec4 out_Normal;
layout (location = 2) out vec4 out_Specular;
layout (location = 3) out vec4 out_Glow;

const float SHEET_SIZE = 8.0;

void main () {
    vec2 realUV = (uv - floor(uv) + atlasUV) / SHEET_SIZE;

    out_Color = texture(albedo, realUV);
    out_Normal = vec4(normal, 1.0);
    out_Specular = texture(specular, realUV);
    out_Glow = texture(emission, realUV);
}