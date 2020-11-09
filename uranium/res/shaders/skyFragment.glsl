#version 150

in vec3 coords;
out vec4 color;

uniform vec3 skyColor;

uniform vec3 sunNormal;

vec3 positiveMix (vec3 a, vec3 b) {
    return vec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z));
}

void main () {
    vec3 vecToEye = normalize(coords);
    vec3 sunMixVector = normalize(mix(sunNormal, vec3(0.0, -1.0, 0.0), 0.9));

    float L = dot(vec3(0.0, 1.0, 0.0), vecToEye);
    float sunsetity = 1 - dot(vec3(0.0, 1.0, 0.0), abs(sunNormal));
    float sunity = max(dot(vecToEye, sunNormal) / 2 + 0.5, 0.0);
    float mixedSunity = min(dot(vecToEye, sunMixVector), 0.0) + 1.0;

    vec3 halo = vec3(0.56, 0.2, 0.1);
    vec3 sky = mix(sqrt(L / 2.0 + 0.5) * skyColor, positiveMix(skyColor, halo), pow(mixedSunity, 6 - sunsetity * 4) * pow(sunsetity, 3) / 2.4);
    vec3 sun = pow(sunity, 5 - sunsetity * 4) * sqrt(sunsetity * 4) / 2 * halo;

    color = vec4(positiveMix(sky, sun), 1.0);
}