
layout(location = 0) in vec3 inVertex;
layout(location = 1) in vec2 inUV;
layout(location = 2) in vec3 inNormal;
layout(location = 3) in vec3 inLight;

uniform mat4 projection;
uniform mat4 view;
uniform ivec3 position;

layout(location = 0) out flat vec2 atlasUV;
layout(location = 1) out vec2 uv;
layout(location = 2) out vec3 normal;
layout(location = 3) out vec3 light;

void main () {
    vec4 worldPos = vec4(position + inVertex, 1.0);
    vec4 positionRelativeToEye = view * worldPos;
    gl_Position = projection * positionRelativeToEye;
    normal = inNormal;
    atlasUV = inUV;
    light = inLight;

    if (abs(normal.x) == 1.0) {
        uv = -inVertex.zy;
    } else if (abs(normal.y) == 1.0) {
        uv = inVertex.xz;
    } else if (abs(normal.z) == 1.0) {
        uv = -inVertex.xy;
    } else {
        uv = vec2(0.0, 0.0);
    }
}