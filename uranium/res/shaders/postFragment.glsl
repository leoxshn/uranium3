#version 420 core

in vec2 uv;

layout (binding = 0) uniform sampler2D color_buffer;
layout (binding = 1) uniform sampler2D normal_buffer;
layout (binding = 2) uniform sampler2D specular_buffer;
layout (binding = 3) uniform sampler2D glow_buffer;
layout (binding = 4) uniform sampler2D depth_buffer;

uniform vec3 sunNormal;
uniform vec3 skyLight;

uniform vec3 ambientLight;

uniform vec3 skyColor;

uniform mat4 rotation;
uniform mat4 view;
uniform mat4 projection;

out vec4 color;

const float FOG_DENSITY = 0.01;
const float FOG_GRADIENT = 2;
const float FOG_START = 48;

const float edge_thres = 0.012;
const float edge_thres2 = 5.5;

#define HueLevCount 15
#define SatLevCount 14
#define ValLevCount 23
float[HueLevCount] HueLevels = float[] (0,20,30,45,60,90,120,140,180,210,240,270,300,330,360);
float[SatLevCount] SatLevels = float[] (0,.1,.15,.2,.24,.3,.4,.5,.6,.8,.9,.95,.98,1);
float[ValLevCount] ValLevels = float[] (0,.01,.02,.03,.07,.12,.18,.22,.26,.3,.4,.45,.5,.55,.6,.65,.7,.75,.8,.85,.9,.95,1);

vec3 RGBtoHSV (float r, float g, float b) {
    float minv, maxv, delta;
    vec3 res;

    minv = min(min(r, g), b);
    maxv = max(max(r, g), b);
    res.z = maxv;            // v

    delta = maxv - minv;

    if (maxv != 0.0)
        res.y = delta / maxv;      // s
    else {
        // r = g = b = 0      // s = 0, v is undefined
        res.y = 0.0;
        res.x = -1.0;
        return res;
    }

    if (r == maxv)
        res.x = (g - b) / delta;      // between yellow & magenta
    else if (g == maxv)
        res.x = 2.0 + (b - r) / delta;   // between cyan & yellow
    else
        res.x = 4.0 + (r - g) / delta;   // between magenta & cyan

    res.x = res.x * 60.0;            // degrees
    if (res.x < 0.0)
        res.x = res.x + 360.0;

    return res;
}

vec3 HSVtoRGB (float h, float s, float v) {
    int i;
    float f, p, q, t;
    vec3 res;

    if (s == 0.0) {
        // achromatic (grey)
        res.x = v;
        res.y = v;
        res.z = v;
        return res;
    }

    h /= 60.0;         // sector 0 to 5
    i = int(floor(h));
    f = h - float(i);         // factorial part of h
    p = v * (1.0 - s);
    q = v * (1.0 - s * f);
    t = v * (1.0 - s * (1.0 - f));

    switch (i) {
        case 0:
            res.x = v;
            res.y = t;
            res.z = p;
            break;
        case 1:
            res.x = q;
            res.y = v;
            res.z = p;
            break;
        case 2:
            res.x = p;
            res.y = v;
            res.z = t;
            break;
        case 3:
            res.x = p;
            res.y = q;
            res.z = v;
            break;
        case 4:
            res.x = t;
            res.y = p;
            res.z = v;
            break;
        default:      // case 5:
            res.x = v;
            res.y = p;
            res.z = q;
            break;
    }
    return res;
}

float nearestLevel (float col, int mode) {
    int levCount;
    if (mode == 0) levCount = HueLevCount;
    if (mode == 1) levCount = SatLevCount;
    if (mode == 2) levCount = ValLevCount;

    for (int i = 0; i < levCount-1; i++ ) {
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

float isEdge (in vec2 coords) {
    float dxtex = 1.0 / float(textureSize(depth_buffer,0));
    float dytex = 1.0 / float(textureSize(depth_buffer,0));
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
        abs(pix[1]-pix[7])+
        abs(pix[5]-pix[3]) +
        abs(pix[0]-pix[8])+
        abs(pix[2]-pix[6])
    ) / 4.0;

    //return clamp(5.5*delta,0.0,1.0);
    return clamp(edge_thres2*delta,0.0,1.0);
}

vec3 toonify (vec3 color) {
    vec3 tmp = vec3(min(color.x, 1.0), min(color.y, 1.0), min(color.z, 1.0));
    vec3 vHSV = RGBtoHSV(tmp.r, tmp.g, tmp.b);
    vHSV.x = nearestLevel(vHSV.x, 0);
    vHSV.y = nearestLevel(vHSV.y, 1);
    vHSV.z = nearestLevel(vHSV.z, 2);
    float edg = isEdge(uv);
    vec3 c = HSVtoRGB(vHSV.x,vHSV.y,vHSV.z);
    if (edg >= edge_thres) {
        c.x = pow(c.x, 2.0);
        c.y = pow(c.y, 2.0);
        c.z = pow(c.z, 2.0);
    }
    return c;
}

vec3 positiveMix (vec3 a, vec3 b) {
    return vec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z));
}

vec3 bloom (float radius) {
    vec4 c = texture(glow_buffer, uv);
    if (radius <= 0) return c.rgb;
    int i = 1;
    for (int x = 1; x < radius; x++) {
        for (int y = 1; y < radius; y++) {
            c += texture(glow_buffer, uv + vec2(x, y) / 1000);
            c += texture(glow_buffer, uv + vec2(-x, y) / 1000);
            c += texture(glow_buffer, uv + vec2(x, -y) / 1000);
            c += texture(glow_buffer, uv + vec2(-x, -y) / 1000);
            i += 4;
        }
    }
    return c.rgb / i;
}

vec3 renderPixel (vec2 uv) {
    vec3 color = vec3(0.0, 0.0, 0.0);
    vec3 albedo = texture(color_buffer, uv).rgb;
    vec3 normal = texture(normal_buffer, uv).rgb;
    vec3 specular = texture(specular_buffer, uv).rgb;
    float depth = texture(depth_buffer, uv).x;

    color = texture(color_buffer, uv).rgb;

    vec3 ndc = vec3(uv * 2.0 - 1.0, depth);
    vec4 v = inverse(rotation) * (inverse(projection) * vec4(ndc, 1.0));
    v.xyz /= v.w;
    vec4 world = view * inverse(projection) * vec4(ndc, 1.0);
    vec3 world_position = world.xyz / world.w;

    if (length(normal) != 0) {
        /// LIGHTING
        float s = (dot(reflect(-sunNormal, normal), normalize(v.xyz)) / 2 + 0.5);
        vec3 specularLight = skyLight * specular.r * 2 * pow(s, specular.b * 4);
        vec3 directionalLight = skyLight * (dot(sunNormal, normal) / 2 + 0.5);
        color = color * positiveMix(ambientLight, directionalLight) + specularLight;
    }

    color = toonify(color);

    color += bloom(9.0);

    if (depth != 0.0) {
        /// FOG
        float visibility = min(exp(-pow(max(length(v.xyz) - FOG_START, 0.0) * FOG_DENSITY, FOG_GRADIENT)), 1.0);

        vec3 vecToEye = -normalize(v.xyz);
        vec3 sunMixVector = normalize(mix(sunNormal, vec3(0.0, -1.0, 0.0), 0.9));

        float sunsetity = 1 - dot(vec3(0.0, 1.0, 0.0), abs(sunNormal));
        float sunity = max(dot(vecToEye, sunNormal) / 2 + 0.5, 0.0);
        float mixedSunity = min(dot(vecToEye, sunMixVector), 0.0) + 1.0;

        vec3 halo = vec3(0.57, 0.24, 0.1);
        vec3 sky = mix(skyColor, positiveMix(skyColor, halo), pow(mixedSunity, 6 - sunsetity * 4) * pow(sunsetity, 3) / 2.8);
        vec3 sun = pow(sunity, 5 - sunsetity * 4) * sqrt(sunsetity * 4) / 2 * halo;

        color = mix(positiveMix(sky, sun), color, visibility);
    }
    return color;
}

void main () {
    color = vec4(renderPixel(uv), 1.0);
}