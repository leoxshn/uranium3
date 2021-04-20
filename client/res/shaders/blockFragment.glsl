
layout(location = 0) in flat vec2 atlasUV;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec3 light;

layout (binding = 0) uniform sampler2D albedo;
layout (binding = 1) uniform sampler2D emission;
layout (binding = 2) uniform sampler2D specular;

layout (location = 0) out vec4 out_Color;
layout (location = 1) out vec4 out_Normal;
layout (location = 2) out vec4 out_Specular;
layout (location = 3) out vec4 out_Glow;
layout (location = 4) out vec4 out_Light;

const float SHEET_SIZE = 8.0;

void main () {
    float p = 1.0 / 128.0;
    vec2 realUV = (clamp(uv - floor(uv), p, 1.0 - p) + atlasUV) / SHEET_SIZE;

    out_Color = texture(albedo, realUV);
    out_Normal = vec4(normal / 2.0 + 0.5, 1.0);
    out_Specular = texture(specular, realUV);
    out_Glow = texture(emission, realUV);
    out_Light = vec4(light, 1.0);
}