
//Painterly Effects Shader for StarChild v1.0 by Majd Akar
//You may use this code whatever way you want, a credit would be nice
//but not necessary. A full description of the shader can be found at
//http://majdakar.blogspot.com/2011/10/techy-stuff-shader.html


//Description
//The aim of this shader is to distort a normally rendered framebuffer to
//give a painterly effect. The distortion is driven by 2 things, the brush pass
//and the depth pass. The brush pass is the whole scene rendered again with
//a brush texture applied to everything. For every pixel, we look at the depth
//of its neighbouring regions, and then we sample the actual framebuffer with
//a UV value offset with the brush pass value (like a smudge) in the opposite
//direction of the greatest depth difference. We do this because artifacts appear
//at the borders of objects, so we always smudge inwards towards the object's outline.

uniform sampler2D ColorPass;
uniform sampler2D BrushPass;
uniform sampler2D DepthPass;

varying vec2 vTexCoord;
uniform float mode;

void main (void) {
    vec3 brushValue;
    vec3 bTex =  texture2D(BrushPass, vTexCoord).rgb;

    //Depth Values for current pixel and distored pixel
    //---------------------------------------------------------------------------------------------------

    float d_mag = 0.005;     //Magnitude of distortion

    //Apply 1.0 - smoothstep to depth values to map them to the visible 0-1 range
    //this is a fast and simple way to remap the depth buffer values

    //Get the depth value of the current pixel we're shading
    float depthVal = 1.0 - smoothstep( 0.998, 1.0, texture2D(DepthPass, vTexCoord).x);

    //Get the depth values of the neighbouring pixels in an x

    float depthVal_offset1    = 1.0 - smoothstep( 0.998, 1.0, texture2D(DepthPass, vTexCoord +  vec2( -d_mag, -d_mag)).x);
    float depthVal_offset2   = 1.0 - smoothstep( 0.998, 1.0, texture2D(DepthPass, vTexCoord + vec2(  d_mag, -d_mag)).x);
    float depthVal_offset3   = 1.0 - smoothstep( 0.998, 1.0, texture2D(DepthPass, vTexCoord + vec2( -d_mag,  d_mag)).x);
    float depthVal_offset4   = 1.0 - smoothstep( 0.998, 1.0, texture2D(DepthPass, vTexCoord + vec2(  d_mag,  d_mag)).x);

    //Find the difference between the neighbours' depth and our current pixel's depth
    float d_diff1 = depthVal_offset1 - depthVal;
    float d_diff2 = depthVal_offset2 - depthVal;
    float d_diff3 = depthVal_offset3 - depthVal;
    float d_diff4 = depthVal_offset4 - depthVal;

    //Find the maximum difference
    float depthMax = max( max( max( depthVal_offset4, depthVal_offset3 ), depthVal_offset2 ), depthVal_offset1 );

    vec3 color_offset;

    //Smudge more if object is distant, like DOF but painterly

    d_mag = 0.008*(1.0-1.5*depthVal);

    //Which neighbour has the maximum depth difference,
    //get the color value from the opposite direction , and smudge
    //multiply the distort vector by the brush texture to get the paint bands.

    if ( depthVal_offset4 == depthMax )
    {
        brushValue =  texture2D(BrushPass, vTexCoord - vec2(-d_mag,-d_mag) ).rgb;
        color_offset = texture2D(ColorPass, vTexCoord + (-0.5+brushValue.r)*vec2( -d_mag, -d_mag)).rgb;
    }
    else if ( depthVal_offset3 == depthMax )
    {
        brushValue =  texture2D(BrushPass, vTexCoord - vec2( d_mag,-d_mag) ).rgb;
        color_offset = texture2D(ColorPass, vTexCoord + (-0.5+brushValue.r)*vec2(  d_mag, -d_mag)).rgb;

    }
    else if ( depthVal_offset2 == depthMax )
    {
        brushValue =  texture2D(BrushPass, vTexCoord - vec2( -d_mag, d_mag) ).rgb;
        color_offset = texture2D(ColorPass, vTexCoord + (-0.5+brushValue.r)*vec2(  -d_mag,  d_mag)).rgb;
    }
    else if ( depthVal_offset1  == depthMax )
    {
        brushValue =  texture2D(BrushPass, vTexCoord - vec2( d_mag, d_mag) ).rgb;
        color_offset = texture2D(ColorPass, vTexCoord + (-0.5+brushValue.r)*vec2(  d_mag,  d_mag)).rgb;
    }

    //Bump ----------------------------------------------------------------------

    vec3 myLight = vec3(0.35,0.659,0.47);
    float bumpValue = 1.8*dot( myLight, vec3( bTex.r, bTex.r, bTex.r ) );
    bumpValue = clamp( bumpValue, 0.5, 1.0 );

    //------------------------------------------------------------------------------


    //Vignette --------------------------------------------------------------------

    float dist = distance(vTexCoord.xy, vec2(0.5,0.5));
    float a = smoothstep(0.9, 0.1, dist);
    float b = 0.4 *a;

    //------------------------------------------------------------------------------

    // Color Correction--Levelling ----------------------------------------------

    float mr = smoothstep( 0.0, (0.95), color_offset.r );
    float mg = smoothstep( 0.0, (0.95), color_offset.g );
    float mb = smoothstep( 0.0, (0.95), color_offset.b );

    //------------------------------------------------------------------------------

    vec4 FinalResult = vec4( a*(vec3(mr,mg,mb) - (0.1*bumpValue) + 0.05*vec3(bTex.r,bTex.r,bTex.r) ), (1.0-bTex.g));

    if ( mode == 1 )      //Normal Rendering
    gl_FragColor = FinalResult;

    if ( mode == 2 )      //Dimmed for menus
    gl_FragColor = vec4(  0.4*vec3(FinalResult.r ,  FinalResult.g,  FinalResult.b), 1.0);

    if ( mode == 3 )         //Red-ish for loosing health
    gl_FragColor = vec4(  FinalResult.r ,  FinalResult.g*0.25,  FinalResult.b*0.25, 1.0);

}



const int radius = 5;

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 src_size = vec2 (1.0 / iResolution.x, 1.0 / iResolution.y);
    vec2 uv = fragCoord.xy/iResolution.xy;
    float n = float((radius + 1) * (radius + 1));
    int i;
    int j;
    vec3 m0 = vec3(0.0); vec3 m1 = vec3(0.0); vec3 m2 = vec3(0.0); vec3 m3 = vec3(0.0);
    vec3 s0 = vec3(0.0); vec3 s1 = vec3(0.0); vec3 s2 = vec3(0.0); vec3 s3 = vec3(0.0);
    vec3 c;

    for (int j = -radius; j <= 0; ++j)  {
        for (int i = -radius; i <= 0; ++i)  {
            c = texture(iChannel0, uv + vec2(i,j) * src_size).rgb;
            m0 += c;
            s0 += c * c;
        }
    }

    for (int j = -radius; j <= 0; ++j)  {
        for (int i = 0; i <= radius; ++i)  {
            c = texture(iChannel0, uv + vec2(i,j) * src_size).rgb;
            m1 += c;
            s1 += c * c;
        }
    }

    for (int j = 0; j <= radius; ++j)  {
        for (int i = 0; i <= radius; ++i)  {
            c = texture(iChannel0, uv + vec2(i,j) * src_size).rgb;
            m2 += c;
            s2 += c * c;
        }
    }

    for (int j = 0; j <= radius; ++j)  {
        for (int i = -radius; i <= 0; ++i)  {
            c = texture(iChannel0, uv + vec2(i,j) * src_size).rgb;
            m3 += c;
            s3 += c * c;
        }
    }


    float min_sigma2 = 1e+2;
    m0 /= n;
    s0 = abs(s0 / n - m0 * m0);

    float sigma2 = s0.r + s0.g + s0.b;
    if (sigma2 < min_sigma2) {
        min_sigma2 = sigma2;
        fragColor = vec4(m0, 1.0);
    }

    m1 /= n;
    s1 = abs(s1 / n - m1 * m1);

    sigma2 = s1.r + s1.g + s1.b;
    if (sigma2 < min_sigma2) {
        min_sigma2 = sigma2;
        fragColor = vec4(m1, 1.0);
    }

    m2 /= n;
    s2 = abs(s2 / n - m2 * m2);

    sigma2 = s2.r + s2.g + s2.b;
    if (sigma2 < min_sigma2) {
        min_sigma2 = sigma2;
        fragColor = vec4(m2, 1.0);
    }

    m3 /= n;
    s3 = abs(s3 / n - m3 * m3);

    sigma2 = s3.r + s3.g + s3.b;
    if (sigma2 < min_sigma2) {
        min_sigma2 = sigma2;
        fragColor = vec4(m3, 1.0);
    }
}