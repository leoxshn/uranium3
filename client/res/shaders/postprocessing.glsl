in vec2 uv;

layout (binding = 0) uniform sampler2D color_buffer;
layout (binding = 1) uniform sampler2D normal_buffer;
layout (binding = 2) uniform sampler2D specular_buffer;
layout (binding = 3) uniform sampler2D glow_buffer;
layout (binding = 4) uniform sampler2D light_buffer;
layout (binding = 5) uniform sampler2D depth_buffer;

uniform float time;

uniform mat4 rotation;
uniform mat4 view;
uniform mat4 projection;

uniform ivec3 selection;

out vec4 color;

const float FOG_DENSITY = 0.01;
const float FOG_GRADIENT = 2.4;
const float FOG_START = 12;

const vec3 sky_color = vec3(0.18, 0.0, 0.36);
const vec3 sky_light = vec3(0.3, 0.3, 0.5);
const vec3 ambient_light = vec3(0.08, 0.09, 0.1);

#define LevCount (42.0)
#define HueLevCount (36.0)
#define skyFogMinHeight (96.0)

vec3 getView (vec2 uv, float depth, mat4 invRotation, mat4 invProjection) {
    vec4 v = invRotation * invProjection * vec4(uv * 2.0 - 1.0, depth, 1.0);
    return v.xyz / -v.w;
}

vec3 positiveMix (vec3 a, vec3 b) { return vec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z)); }

float isEdge (vec2 coords) {
    vec2 ts = textureSize(normal_buffer, 0);
    float dxtex = 1.0 / float(ts.x);
    float dytex = 1.0 / float(ts.y);
    vec4 pix[9];
    int k = -1;

    // read neighboring pixel intensities
    for (int i = -1; i < 2; i++) {
        for(int j = -1; j < 2; j++) {
            k++;
            vec2 uv = coords + vec2(float(i)*dxtex, float(j)*dytex);
            pix[k] = vec4(texture(normal_buffer, uv).rgb, texture(depth_buffer, uv).r);
        }
    }

    // average color differences around neighboring pixels
    vec4 multi_delta = (
        abs(pix[1] - pix[7]) +
        abs(pix[5] - pix[3]) +
        abs(pix[0] - pix[8]) +
        abs(pix[2] - pix[6])
    ) / 4.0;

    float delta = multi_delta.w + max(max(multi_delta.x, multi_delta.y), multi_delta.z) * 0.1;

    return clamp(5.0 * delta, 0.0, 1.0);
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

float getVisibility (float distance) {
    return min(exp(-pow(max(distance - FOG_START, 0.0) * FOG_DENSITY, FOG_GRADIENT)), 1.0);
}

float texh(in vec3 p, in float str) {
    p *= .7; //Scale p
    float rz = 1.0;
    for (int i = 0; i < 10; i++) {
        float g = pow(open_simplex_bcc_base(vec3(.025, .5, .5) * p).a, 1.0);
        g = smoothstep(0.0 - str * 0.1, 2.3 - str * 0.1, g);
        rz = min(1.0 - g, rz);
        p.xyz = p.yzx;
        p += .07;
        p *= 1.2;
        if (float(i) > str) break;
    }
    return rz*1.05;
}

float starsBase (vec3 vecToEye) {
    vec2 resolution = textureSize(color_buffer, 0);

    float stars = max(0, random(ivec2(vecToEye.xz * (resolution.y * 0.8))) * 8.0 - 7.0);
    if (stars <= 0.99) return 0.0;
    stars *= max(0, min(1, vecToEye.y * 1.2 - 0.2));
    stars *= 0.5 + max(-0.5, open_simplex_bcc_plane_first(vec3(vecToEye.xz * 40, time / 2000)).a);
    return stars;
}

float glitter (vec3 vecToEye) {
    vec2 resolution = textureSize(color_buffer, 0);

    float stars = max(0, random(ivec2(vecToEye.xz * resolution.y)) * 8.0 - 7.0);
    if (stars <= 0.9) return 0.0;
    stars *= max(0, min(1, vecToEye.y * 1.2 - 0.2));
    stars *= 0.5 + max(-0.5, open_simplex_bcc_plane_first(vec3(vecToEye.xz * 12, time / 4800)).a);
    stars *= abs(open_simplex_bcc_plane_first(vec3(vecToEye.xz * 96, time / 960)).a);
    return stars;
}

float stars (vec3 vecToEye, vec2 uv) {
    vec2 resolution = textureSize(color_buffer, 0);

    const float r = resolution.y / 100.0;
    const float a = 1.0 / resolution.y;
    const float w = 0.9;
    const float e = 0.5;

    float s = starsBase(vecToEye);

    const float rr = r * a;
    const float xa = resolution.y / resolution.x * a;
    const float rrx = r * xa;

    mat4 ir = inverse(rotation);
    mat4 ip = inverse(projection);
    float ww = w;

    for (float x = xa; x < rrx; x += xa) {
        s += starsBase(-normalize(
            getView(vec2(uv.x + x, uv.y), 1.0, ir, ip)
        )) * ww;
        s += starsBase(-normalize(
            getView(vec2(uv.x - x, uv.y), 1.0, ir, ip)
        )) * ww;
        ww *= e;
    }
    ww = w;
    for (float y = a; y < rr; y += a) {
        s += starsBase(-normalize(
            getView(vec2(uv.x, uv.y + y), 1.0, ir, ip)
        )) * ww;
        s += starsBase(-normalize(
            getView(vec2(uv.x, uv.y - y), 1.0, ir, ip)
        )) * ww;
        ww *= e;
    }
    return s;
}

vec3 renderPixel (vec2 uv) {
    vec3 color = texture(color_buffer, uv).rgb;
    vec3 normal = texture(normal_buffer, uv).rgb * 2.0 - 1.0;
    vec3 specular = texture(specular_buffer, uv).rgb;
    vec3 light = texture(light_buffer, uv).rgb;
    float depth = texture(depth_buffer, uv).x;

    vec2 resolution = textureSize(color_buffer, 0);
    float aspectRatio = resolution.y / resolution.x;

    vec3 v = getView(uv, depth, inverse(rotation), inverse(projection));
    vec3 nv = normalize(v);

    float edge = isEdge(uv);

    vec4 world = inverse(view) * inverse(projection) * vec4(vec3(uv, depth) * 2.0 - 1.0, 1.0);
    world.xyz /= world.w;
    vec3 block = ivec3(world.xyz);

    if (depth != 1.0) {
        if (block == selection) {
            color += edge * 6.0;
        } else if (edge >= 0.008) {
            color *= 1.0 - edge;
        }
        if (length(normal) != 0.0) {
            /// LIGHTING
            float fresnel = pow(max(0, min(0.8, (1.0 - dot(nv, normal)) * 0.5)), 3.14);

            // Diffuse
            vec3 directional_light = sky_light * (dot(vec3(0.0, 1.0, 0.0), normal) / 2 + 0.5);
            vec3 diffuse_light = positiveMix(ambient_light + directional_light, light) * (1 - fresnel);

            // Specular (texture: r = intensity, b = smoothness)
            float s = (dot(reflect(vec3(0.0, -1.0, 0.0), normal), nv) / 2 + 0.5);
            vec3 specular_color = positiveMix(light, sky_light);
            vec3 specular_light = specular_color * specular.r * 2 * pow(s, specular.b * 4) * (1 + fresnel);

            color = color * diffuse_light + specular_light;
        }
        color = mix(color, color * ambientOcclusion(uv), 1.0);
    }

    if (depth != 0.0) {
        /// FOG
        const float distance = length(v);
        const float visibility = getVisibility(distance);
        const vec3 vecToEye = -nv;

        if (block.y >= skyFogMinHeight) {
            vec3 fog_start = vec3(0.36, 0.0, 0.75);
            vec3 fog_end = mix(vec3(0.32, 0.28, 0.75), sky_color, max(vecToEye.y, 0));
            float fog_far = clamp((distance - FOG_START - 24) / 192, 0.0, 1.0);
            vec3 fog_color = mix(fog_start, fog_end, fog_far);
            vec3 fog = fog_color * min(block.y - skyFogMinHeight, 5.0) / 5.0;
            float invisibility = (1 - visibility);
            color = mix(fog, color, visibility);

            if (depth == 1.0) {
                color += stars(vecToEye, uv);
                color += glitter(vecToEye);
            }

            color = mix(color, color * fog, pow(edge, 2.0) * invisibility * 0.8);
        } else {
            color = mix(vec3(0.0), color, visibility);
        }
    }

    return color;
}

const highp float NOISE_GRANULARITY = 0.5/255.0;

void main () {
    color = vec4(renderPixel(uv) + mix(-NOISE_GRANULARITY, NOISE_GRANULARITY, random(uv)), 1.0);
}