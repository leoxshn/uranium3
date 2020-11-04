#version 420 core

in vec2 atlasUV;
in vec2 uv;
in vec3 normal;
in vec3 toEyeVector;

layout (binding = 0) uniform sampler2D albedo;
layout (binding = 1) uniform sampler2D specular;
uniform vec3 skyColor;
uniform vec3 sunNormal;
uniform vec3 skyLight;
uniform vec3 ambientLight;
uniform float emission;

out vec4 outColor;

const float SHEET_SIZE = 8.0;

const float FOG_DENSITY = 0.01;
const float FOG_GRADIENT = 2;
const float FOG_START = 48;

void main () {
    vec2 realUV = (uv - floor(uv) + atlasUV) / SHEET_SIZE;
    vec3 directionalLight = skyLight * (dot(sunNormal, normal) / 2 + 0.5);

    float specularValue = (dot(reflect(-sunNormal, normal), normalize(toEyeVector)) / 2 + 0.5);
    vec4 specularTexture = texture(specular, realUV);
    vec3 specularLight = skyLight * specularTexture.r * 2 * pow(specularValue, specularTexture.b * 4);

    vec3 light = max(ambientLight * (emission + 1), directionalLight) + specularLight;

    float visibility = min(exp(-pow(max(length(toEyeVector) - FOG_START, 0) * FOG_DENSITY, FOG_GRADIENT)), 1.0);

    outColor = mix(vec4(skyColor, 1.0), vec4(light, 1.0) * texture(albedo, realUV), visibility);
}