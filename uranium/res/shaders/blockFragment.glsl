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

vec3 positiveMix (vec3 a, vec3 b) {
    return vec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z));
}

void main () {
    vec2 realUV = (uv - floor(uv) + atlasUV) / SHEET_SIZE;

    /// LIGHTING
    float specularValue = (dot(reflect(-sunNormal, normal), normalize(toEyeVector)) / 2 + 0.5);
    vec4 specularTexture = texture(specular, realUV);
    vec3 specularLight = skyLight * specularTexture.r * 2 * pow(specularValue, specularTexture.b * 4);

    vec3 directionalLight = skyLight * (dot(sunNormal, normal) / 2 + 0.5);
    vec3 light = max(ambientLight * (emission + 1), directionalLight) + specularLight;

    /// FOG
    float visibility = min(exp(-pow(max(length(toEyeVector) - FOG_START, 0) * FOG_DENSITY, FOG_GRADIENT)), 1.0);

    vec3 vecToEye = -normalize(toEyeVector);
    vec3 sunMixVector = normalize(mix(sunNormal, vec3(0.0, -1.0, 0.0), 0.9));

    float sunsetity = 1 - dot(vec3(0.0, 1.0, 0.0), abs(sunNormal));
    float sunity = max(dot(vecToEye, sunNormal) / 2 + 0.5, 0.0);
    float mixedSunity = min(dot(vecToEye, sunMixVector), 0.0) + 1.0;

    vec3 halo = vec3(0.57, 0.24, 0.1);
    vec3 sky = mix(skyColor, positiveMix(skyColor, halo), pow(mixedSunity, 6 - sunsetity * 4) * pow(sunsetity, 3) / 2.8);
    vec3 sun = pow(sunity, 5 - sunsetity * 4) * sqrt(sunsetity * 4) / 2 * halo;

    outColor = mix(vec4(positiveMix(sky, sun), 1.0), vec4(light, 1.0) * texture(albedo, realUV), visibility);
}