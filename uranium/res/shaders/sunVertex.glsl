#version 420 core

in vec2 inVertex;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 rotation;
uniform float distance;

out vec2 uv;

void main () {
    mat4 r = mat4(0.0);
    r[0].x = 1.0;
    r[1].z = -1.0;
    r[2].y = 1.0;
    r[3].w = 1.0;
    vec4 worldPos = vec4(inVertex, -distance, 1.0) * r * rotation;
    vec4 positionRelativeToEye = view * worldPos;
    gl_Position = projection * positionRelativeToEye;
    uv = vec2(0.5 + inVertex.x / 2.0, 0.5 - inVertex.y / 2.0);
}