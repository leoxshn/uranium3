
float PHI = 1.61803398874989484820459;  // Φ = Golden Ratio

vec3 tonemap_filmic(vec3 x) {
    vec3 X = max(vec3(0.0), x - 0.004);
    vec3 result = (X * (6.2 * X + 0.5)) / (X * (6.2 * X + 1.7) + 0.06);
    return pow(result, vec3(2.2));
}

float tonemap_filmic(float x) {
    float X = max(0.0, x - 0.004);
    float result = (X * (6.2 * X + 0.5)) / (X * (6.2 * X + 1.7) + 0.06);
    return pow(result, 2.2);
}

float random (vec2 co) {
    highp float a = 12.9898;
    highp float b = 78.233;
    highp float c = 43758.5453;
    highp float dt = dot(co.xy, vec2(a,b));
    highp float sn = mod(dt, 3.14);
    return fract(sin(sn) * c);
}
float random (float a, float b) { return random(vec2(a, b)); }
float hash (float n) { return fract(cos(n) * 114514.1919); }

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float smooth_quantize (float col, float levCount) {
    float c = col * levCount;
    float cc = round(c);
    float f = c - cc;
    return (smoothstep(-0.5, 0.5, f) + cc) / levCount;
}

/// K.jpg's Simplex-like Re-oriented 4-Point BCC Noise
/// Output: vec4(dF/dx, dF/dy, dF/dz, value)

// Inspired by Stefan Gustavson's noise
vec4 open_simplex_permute(vec4 t) { return t * (t * 34.0 + 133.0); }

// Gradient set is a normalized expanded rhombic dodecahedron
vec3 open_simplex_grad(float hash) {
    // Random vertex of a cube, +/- 1 each
    vec3 cube = mod(floor(hash / vec3(1.0, 2.0, 4.0)), 2.0) * 2.0 - 1.0;
    // Random edge of the three edges connected to that vertex
    // Also a cuboctahedral vertex
    // And corresponds to the face of its dual, the rhombic dodecahedron
    vec3 cuboct = cube;
    cuboct[int(hash / 16.0)] = 0.0;
    // In a funky way, pick one of the four points on the rhombic face
    float type = mod(floor(hash / 8.0), 2.0);
    vec3 rhomb = (1.0 - type) * cube + type * (cuboct + cross(cube, cuboct));
    // Expand it so that the new edges are the same length
    // as the existing ones
    vec3 grad = cuboct * 1.22474487139 + rhomb;
    // To make all gradients the same length, we only need to shorten the
    // second type of vector. We also put in the whole noise scale constant.
    // The compiler should reduce it into the existing floats. I think.
    grad *= (1.0 - 0.042942436724648037 * type) * 32.80201376986577;
    return grad;
}

// BCC lattice split up into 2 cube lattices
vec4 open_simplex_bcc_base(vec3 X) {
    // First half-lattice, closest edge
    vec3 v1 = round(X);
    vec3 d1 = X - v1;
    vec3 score1 = abs(d1);
    vec3 dir1 = step(max(score1.yzx, score1.zxy), score1);
    vec3 v2 = v1 + dir1 * sign(d1);
    vec3 d2 = X - v2;
    // Second half-lattice, closest edge
    vec3 X2 = X + 144.5;
    vec3 v3 = round(X2);
    vec3 d3 = X2 - v3;
    vec3 score2 = abs(d3);
    vec3 dir2 = step(max(score2.yzx, score2.zxy), score2);
    vec3 v4 = v3 + dir2 * sign(d3);
    vec3 d4 = X2 - v4;
    // Gradient hashes for the four points, two from each half-lattice
    vec4 hashes = open_simplex_permute(mod(vec4(v1.x, v2.x, v3.x, v4.x), 289.0));
    hashes = open_simplex_permute(mod(hashes + vec4(v1.y, v2.y, v3.y, v4.y), 289.0));
    hashes = mod(open_simplex_permute(mod(hashes + vec4(v1.z, v2.z, v3.z, v4.z), 289.0)), 48.0);
    // Gradient extrapolations & kernel function
    vec4 a = max(0.5 - vec4(dot(d1, d1), dot(d2, d2), dot(d3, d3), dot(d4, d4)), 0.0);
    vec4 aa = a * a; vec4 aaaa = aa * aa;
    vec3 g1 = open_simplex_grad(hashes.x); vec3 g2 = open_simplex_grad(hashes.y);
    vec3 g3 = open_simplex_grad(hashes.z); vec3 g4 = open_simplex_grad(hashes.w);
    vec4 extrapolations = vec4(dot(d1, g1), dot(d2, g2), dot(d3, g3), dot(d4, g4));
    // Derivatives of the noise
    vec3 derivative = -8.0 * mat4x3(d1, d2, d3, d4) * (aa * a * extrapolations)
    + mat4x3(g1, g2, g3, g4) * aaaa;
    // Return it all as a vec4
    return vec4(derivative, dot(aaaa, extrapolations));
}

// Use this if you don't want Z to look different from X and Y
vec4 open_simplex_bcc_classic(vec3 X) {
    // Rotate around the main diagonal. Not a skew transform.
    vec4 result = open_simplex_bcc_base(dot(X, vec3(2.0/3.0)) - X);
    return vec4(dot(result.xyz, vec3(2.0/3.0)) - result.xyz, result.w);
}

// Use this if you want to show X and Y in a plane, and use Z for time, etc.
vec4 open_simplex_bcc_plane_first(vec3 X) {
    // Rotate so Z points down the main diagonal. Not a skew transform.
    mat3 orthonormalMap = mat3(
    0.788675134594813, -0.211324865405187, -0.577350269189626,
    -0.211324865405187, 0.788675134594813, -0.577350269189626,
    0.577350269189626, 0.577350269189626, 0.577350269189626);
    vec4 result = open_simplex_bcc_base(orthonormalMap * X);
    return vec4(result.xyz * orthonormalMap, result.w);
}
/// End noise code


/*
// Adapted from Brian Sharpe's Simplex Cellular Noise from https://github.com/BrianSharpe/GPU-Noise-Lib/blob/master/gpu_noise_lib.glsl
//generates 2 random numbers for each of the 4 cell corners
void _SimplexCellular2D_FAST32_hash_2D(vec2 gridcell, float period, out vec4 hash_0, out vec4 hash_1) {
    //gridcell = mod(gridcell, period);
    const vec2 OFFSET = vec2( 26.0, 161.0 );
    const float DOMAIN = 71.0;
    const vec2 SOMELARGEFLOATS = vec2( 951.135664, 642.949883 );
    vec4 P = vec4( gridcell.xy, gridcell.xy + 1.0 );

    // Wrap by period
    P = mod(P, period);

    P = P - floor(P * ( 1.0 / DOMAIN )) * DOMAIN;
    P += OFFSET.xyxy;
    P *= P;
    P = P.xzxz * P.yyww;
    hash_0 = fract( P * ( 1.0 / SOMELARGEFLOATS.x ) );
    hash_1 = fract( P * ( 1.0 / SOMELARGEFLOATS.y ) );
}

vec4 _SimplexCellular2D_Cellular_weight_samples(vec4 samples) {
    samples = samples * 2.0 - 1.0;
    //return (1.0 - samples * samples) * sign(samples);	// square
    return (samples * samples * samples) - sign(samples);	// cubic (even more variance)
}

float SimplexCellular2D(vec2 P, float period) {
    //	simplex math based off Stefan Gustavson's and Ian McEwan's work at...
    //	http://github.com/ashima/webgl-noise

    //	simplex math constants
    const float SKEWFACTOR = 0.36602540378443864676372317075294;			// 0.5*(sqrt(3.0)-1.0)
    const float UNSKEWFACTOR = 0.21132486540518711774542560974902;			// (3.0-sqrt(3.0))/6.0
    const float SIMPLEX_TRI_HEIGHT = 0.70710678118654752440084436210485;	// sqrt( 0.5 )	height of simplex triangle.
    const float INV_SIMPLEX_TRI_HEIGHT = 1.4142135623730950488016887242097;	//	1.0 / sqrt( 0.5 )
    const vec3 SIMPLEX_POINTS = vec3(1.0-UNSKEWFACTOR, -UNSKEWFACTOR, 1.0-2.0*UNSKEWFACTOR) * INV_SIMPLEX_TRI_HEIGHT;		//	vertex info for simplex triangle

    //	establish our grid cell.
    P *= SIMPLEX_TRI_HEIGHT;		// scale space so we can have an approx feature size of 1.0  ( optional )
    vec2 Pi = floor(P + dot(P, vec2(SKEWFACTOR)));

    vec4 hash_x, hash_y;
    _SimplexCellular2D_FAST32_hash_2D(Pi, period, hash_x, hash_y);
    const float JITTER_WINDOW = (0.10566243270259355887271280487451 * INV_SIMPLEX_TRI_HEIGHT);		// this will guarentee no artifacts.
    hash_x = _SimplexCellular2D_Cellular_weight_samples(hash_x) * JITTER_WINDOW;
    hash_y = _SimplexCellular2D_Cellular_weight_samples(hash_y) * JITTER_WINDOW;
    vec2 p0 = ((Pi - dot( Pi, vec2(UNSKEWFACTOR))) - P) * INV_SIMPLEX_TRI_HEIGHT;
    hash_x += p0.xxxx;
    hash_y += p0.yyyy;
    hash_x.yzw += SIMPLEX_POINTS.xyz;
    hash_y.yzw += SIMPLEX_POINTS.yxz;
    vec4 distsq = hash_x*hash_x + hash_y*hash_y;
    vec2 tmp = min(distsq.xy, distsq.zw);
    return min(tmp.x, tmp.y);
}*/