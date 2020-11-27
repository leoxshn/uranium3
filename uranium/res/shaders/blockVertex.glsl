#version 420 core

in vec3 inVertex;
in vec2 inUV;
in vec3 inNormal;

uniform mat4 projection;
uniform mat4 view;
uniform vec3 position;

out vec2 atlasUV;
out vec2 uv;
out vec3 normal;

void main () {
    vec4 worldPos = vec4(position + inVertex, 1.0);
    vec4 positionRelativeToEye = view * worldPos;
    gl_Position = projection * positionRelativeToEye;
    normal = inNormal;
    atlasUV = inUV;

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