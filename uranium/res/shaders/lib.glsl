

float random (vec2 co) { return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453); }

vec3 getView (vec2 uv, float depth, mat4 invRotation, mat4 invProjection) {
    vec4 v = invRotation * invProjection * vec4(uv * 2.0 - 1.0, depth, 1.0);
    return v.xyz / -v.w;
}

vec3 RGBtoHSV (float r, float g, float b) {

    float minv = min(min(r, g), b);
    float maxv = max(max(r, g), b);

    vec3 res;

    res.z = maxv;

    float delta = maxv - minv;

    if (maxv != 0.0) {
        res.y = delta / maxv;
    } else {
        res.y = 0.0;
        res.x = -1.0;
        return res;
    }

    if (r == maxv) {
        res.x = (g - b) / delta;
    } else if (g == maxv) {
        res.x = 2.0 + (b - r) / delta;
    } else {
        res.x = 4.0 + (r - g) / delta;
    }

    res.x = res.x * 60.0;
    if (res.x < 0.0) {
        res.x = res.x + 360.0;
    }

    return res;
}

vec3 HSVtoRGB (float h, float s, float v) {

    if (s == 0.0) {
        return vec3(v, v, v);
    }

    h /= 60.0;         // sector 0 to 5
    int i = int(floor(h));
    float f = h - float(i);         // factorial part of h
    float p = v * (1.0 - s);
    float q = v * (1.0 - s * f);
    float t = v * (1.0 - s * (1.0 - f));

    vec3 res;
    switch (i) {
        case 0: {
            res.x = v;
            res.y = t;
            res.z = p;
        } break;
        case 1: {
            res.x = q;
            res.y = v;
            res.z = p;
        } break;
        case 2: {
            res.x = p;
            res.y = v;
            res.z = t;
        } break;
        case 3: {
            res.x = p;
            res.y = q;
            res.z = v;
        } break;
        case 4: {
            res.x = t;
            res.y = p;
            res.z = v;
        } break;
        default: { // case 5:
            res.x = v;
            res.y = p;
            res.z = q;
        } break;
    }
    return res;
}