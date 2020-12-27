in vec2 uv;

layout (binding = 0) uniform sampler2D color_buffer;
layout (binding = 1) uniform sampler2D normal_buffer;
layout (binding = 2) uniform sampler2D specular_buffer;
layout (binding = 3) uniform sampler2D glow_buffer;
layout (binding = 4) uniform sampler2D depth_buffer;

uniform vec3 sunNormal;
uniform vec3 sunLight;

uniform vec3 ambientLight;

uniform vec3 skyColor;

uniform mat4 rotation;
uniform mat4 view;
uniform mat4 projection;

out vec4 color;

const float FOG_DENSITY = 0.005;
const float FOG_GRADIENT = 3.6;
const float FOG_START = 96;

const float edge_thres = 0.012;
const float edge_thres2 = 5.5;

#define HueLevCount 15
#define SatLevCount 23
#define ValLevCount 25
const float[HueLevCount] HueLevels = float[] (0,20,30,45,60,90,120,140,180,210,240,270,300,330,360);
const float[SatLevCount] SatLevels = float[] (0,.01,.02,.03,.07,.1,.15,.2,.24,.38,.45,.5,.55,.6,.65,.7,.75,.8,.85,.9,.95,.98,1);
const float[ValLevCount] ValLevels = float[] (0,.01,.02,.03,.07,.09,.12,.18,.22,.26,.3,.35,.4,.45,.5,.55,.6,.65,.7,.75,.8,.85,.9,.95,1);

vec3 positiveMix (vec3 a, vec3 b) { return vec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z)); }

float nearestLevel (float col, int mode) {
    int levCount;
    if (mode == 0) levCount = HueLevCount;
    if (mode == 1) levCount = SatLevCount;
    if (mode == 2) levCount = ValLevCount;

    for (int i = 0; i < levCount - 1; i++) {
        if (mode == 0) {
            if (col >= HueLevels[i] && col <= HueLevels[i+1]) {
                return HueLevels[i+1];
            }
        }
        if (mode == 1) {
            if (col >= SatLevels[i] && col <= SatLevels[i+1]) {
                return SatLevels[i+1];
            }
        }
        if (mode == 2) {
            if (col >= ValLevels[i] && col <= ValLevels[i+1]) {
                return ValLevels[i+1];
            }
        }
    }
}

float isEdge (vec2 coords) {
    float dxtex = 1.0 / float(textureSize(depth_buffer, 0).x);
    float dytex = 1.0 / float(textureSize(depth_buffer, 0).y);
    float pix[9];
    int k = -1;
    float delta;

    // read neighboring pixel intensities
    for (int i = -1; i < 2; i++) {
        for(int j = -1; j < 2; j++) {
            k++;
            pix[k] = texture(depth_buffer, coords + vec2(float(i)*dxtex, float(j)*dytex)).x;
        }
    }

    // average color differences around neighboring pixels
    delta = (
        abs(pix[1] - pix[7]) +
        abs(pix[5] - pix[3]) +
        abs(pix[0] - pix[8]) +
        abs(pix[2] - pix[6])
    ) / 4.0;

    return clamp(edge_thres2 * delta, 0.0, 1.0);
}

float doAmbientOcclusion (vec2 tcoord, vec2 uv, vec3 p, vec3 cnorm) {
    float scale = 0.5, bias = 0.1, intensity = 2.0;
    vec3 diff = -getView(tcoord + uv, texture(depth_buffer, tcoord + uv).x, inverse(rotation), inverse(projection)) - p;
    vec3 v = normalize(diff);
    float d = length(diff) * scale;
    return max(0.0, dot(cnorm, v) - bias) * (1.0 / (1.0 + d)) * intensity;
}

float ambientOcclusion(vec2 uv) {
    float depth = texture(depth_buffer, uv).x;
    if (depth == 1.0) return 1.0;
    vec3 p = -getView(uv, depth, inverse(rotation), inverse(projection));
    vec3 n = texture(normal_buffer, uv).xyz;
    vec2 rnd = normalize(vec2(random(p.xy), random(n.xy)));

    float ao = 0.0f;
    float rad = 1.0 / p.z;
    vec2 vec[4];
    vec[0] = vec2(1.0, 0.0);
    vec[1] = vec2(-1.0, 0.0);
    vec[2] = vec2(0.0, 1.0);
    vec[3] = vec2(0.0, -1.0);

    int iterations = 4;
    for (int j = 0; j < iterations; ++j) {
        vec2 coord1 = reflect(vec[j], rnd) * rad;
        vec2 coord2 = vec2(coord1.x * 0.707 - coord1.y * 0.707, coord1.x * 0.707 + coord1.y * 0.707);
        ao += doAmbientOcclusion(uv, coord1 * 0.25, p, n);
        ao += doAmbientOcclusion(uv, coord2 * 0.5, p, n);
        ao += doAmbientOcclusion(uv, coord1 * 0.75, p, n);
        ao += doAmbientOcclusion(uv, coord2, p, n);
    }
    ao /= float(iterations) * 4.0;
    return 1.0 - ao;
}

vec3 godrays(
    float density,
    float weight,
    float decay,
    vec2 screenSpaceLightPos,
    vec2 uv,
    vec3 sun
) {
    const int numSamples = 28;
    vec3 fragColor = vec3(0.0);

    vec2 deltaTextCoord = vec2(uv - screenSpaceLightPos.xy);

    vec2 textCoo = uv.xy;
    deltaTextCoord *= (1.0 / float(numSamples)) * density;
    float illuminationDecay = 1.0;

    for (int i = 0; i < numSamples; i++) {
        textCoo -= deltaTextCoord;
        vec3 samp = texture2D(depth_buffer, textCoo).x == 1.0 ? sun : vec3(0.0);
        samp *= illuminationDecay * weight;
        fragColor += samp;
        illuminationDecay *= decay;
    }

    return texture2D(depth_buffer, uv).x == 1.0 ? positiveMix(sun, fragColor) : fragColor;
}

vec3 renderPixel (vec2 uv) {
    vec3 color = vec3(0.0);
    vec3 albedo = texture(color_buffer, uv).rgb;
    vec3 normal = texture(normal_buffer, uv).rgb * 2.0 - 1.0;
    vec3 specular = texture(specular_buffer, uv).rgb;
    float depth = texture(depth_buffer, uv).x;

    color = texture(color_buffer, uv).rgb;

    vec3 v = getView(uv, depth, inverse(rotation), inverse(projection));

    if (length(normal) != 0) {
        /// LIGHTING
        float s = (dot(reflect(-sunNormal, normal), normalize(v)) / 2 + 0.5);
        vec3 specularLight = sunLight * specular.r * 2 * pow(s, specular.b * 4);
        vec3 directionalLight = sunLight * (dot(sunNormal, normal) / 2 + 0.5);
        color = color * positiveMix(ambientLight, directionalLight) + specularLight;
    }

    float edge = isEdge(uv);

    { // Toonify
        vec3 tmp = vec3(min(color.x, 1.0), min(color.y, 1.0), min(color.z, 1.0));
        vec3 vHSV = RGBtoHSV(tmp.r, tmp.g, tmp.b);
        vHSV.x = nearestLevel(vHSV.x, 0);
        vHSV.y = nearestLevel(vHSV.y, 1);
        vHSV.z = nearestLevel(vHSV.z, 2);
        if (edge >= edge_thres) {
            vHSV.y *= 2.0;
            vHSV.z = pow(vHSV.z, 1.8);
        }
        color = HSVtoRGB(vHSV.x, vHSV.y, vHSV.z);
    }

    { // AO (blurred)
        vec2 res = textureSize(glow_buffer, 0);
        float aspectRatio = res.y / res.x;
        float c = ambientOcclusion(uv);
        c += ambientOcclusion(uv + vec2(aspectRatio, 1.0) / res.y);
        c += ambientOcclusion(uv + vec2(-aspectRatio, 1.0) / res.y);
        c += ambientOcclusion(uv + vec2(aspectRatio, -1.0) / res.y);
        c += ambientOcclusion(uv + vec2(-aspectRatio, -1.0) / res.y);
        color = mix(color, color * c / 5, 0.5);
    }

    /*{ // Bloom
        int radius = 4;
        vec4 c = texture(glow_buffer, uv);
        vec2 res = textureSize(glow_buffer, 0);
        float aspectRatio = res.y / res.x;
        int i = 1;
        for (int x = 1; x < radius; x++) {
            for (int y = 1; y < radius; y++) {
                c += texture(glow_buffer, uv + vec2(x * aspectRatio, y) / 480);
                c += texture(glow_buffer, uv + vec2(-x * aspectRatio, y) / 480);
                c += texture(glow_buffer, uv + vec2(x * aspectRatio, -y) / 480);
                c += texture(glow_buffer, uv + vec2(-x * aspectRatio, -y) / 480);
                i += 4;
            }
        }
        color += c.rgb / i * .8;
    }*/

    if (depth != 0.0) {
        /// FOG
        float visibility = min(exp(-pow(max(length(v) - FOG_START, 0.0) * FOG_DENSITY, FOG_GRADIENT)), 1.0);

        vec3 vecToEye = -normalize(v);
        vec3 sunMixVector = normalize(mix(sunNormal, vec3(0.0, -1.0, 0.0), 0.9));

        float sunsetity = 1 - dot(vec3(0.0, 1.0, 0.0), abs(sunNormal));
        float sunity = max(dot(vecToEye, sunNormal) / 2 + 0.5, 0.0);
        float mixedSunity = min(dot(vecToEye, sunMixVector), 0.0) + 1.0;

        vec3 haloColor = vec3(0.57, 0.24, 0.1);
        vec3 sky = mix(skyColor, positiveMix(skyColor, haloColor), pow(mixedSunity, 6 - sunsetity * 4) * pow(sunsetity, 3) / 2.8);
        vec3 halo = pow(sunity, 5 - sunsetity * 4) * sqrt(sunsetity * 4) / 2 * haloColor;

        color = mix(positiveMix(sky, halo), color, visibility);

        color = mix(color, color * sky, clamp(edge * 72, 0.0, 1.0) * (1 - visibility));

        vec4 screenSpaceSunPos = (projection * (rotation * vec4(sunNormal, 1.0)));
        screenSpaceSunPos.xyz = screenSpaceSunPos.xyz / screenSpaceSunPos.w / 2.0 + 0.5;

        vec3 sun = vec3(pow(sunity, 35.0) * 2.4, pow(sunity, 47.0) * 2.1, pow(sunity, 57.0) * 2);
        color += godrays(0.85, 0.06, 0.98, screenSpaceSunPos.xy, uv, sun) * max(sqrt(screenSpaceSunPos.z), 0.0) / 2.0;
    }

    return color;
}

void main () {
    color = vec4(renderPixel(uv), 1.0);
}