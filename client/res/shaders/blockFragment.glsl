#version 420 core

in vec2 atlasUV;
in vec2 uv;
in vec3 normal;
in float visibility;

uniform sampler2D tex;
uniform vec3 skyColor;
uniform vec3 ambientLight;
uniform float emission;

out vec4 outColor;

const int SHEET_SIZE = 8;

void main () {
    vec2 realUV = (uv - floor(uv) + atlasUV) / SHEET_SIZE;
    vec3 lightColor = vec3(1.0, 1.0, 0.95);
    vec3 directionalLight = lightColor * (dot(normalize(vec3(0.0, 1.0, 0.3)), normal) / 2 + 0.5);
    vec3 light = max(ambientLight * (emission + 1), directionalLight);
    outColor = mix(vec4(skyColor, 1.0), vec4(light, 1.0) * texture(tex, realUV), visibility);
    //outColor = mix(vec4(normal, 1.0), outColor, 0.9);
}