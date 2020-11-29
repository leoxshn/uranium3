#version 420 core

in vec2 uv;

layout (binding = 0) uniform sampler2D color_buffer;
layout (binding = 1) uniform sampler2D depth_buffer;

uniform vec3 sunNormal;
uniform vec3 skyLight;

uniform vec3 ambientLight;

uniform vec3 skyColor;

uniform mat4 rotation;
uniform mat4 view;
uniform mat4 projection;

out vec4 color;

#define SAMPLE_COUNT 7
const float[SAMPLE_COUNT] samples = float[] (0.07, 0.055, 0.04, 0.03, 0.02, 0.01, 0.005);

const float sampleStrength = 4.2;

void main () {
    vec2 dir = 0.5 - uv;
    float dist = sqrt(dir.x * dir.x + dir.y * dir.y);
    dir /= dist;

    vec4 c = texture2D(color_buffer, uv);
    vec4 sum = c;

    for (int i = 0; i < SAMPLE_COUNT; i++)
        sum += texture2D(color_buffer, uv + dir * samples[i]);

    sum /= SAMPLE_COUNT + 1.0;
    float t = pow(dist, 2.0) * sampleStrength;
    t = clamp(t, 0.0, 1.0);

    color = mix(c, sum, t);
}