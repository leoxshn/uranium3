#version 420 core

in vec3 position;
out vec3 coords;

uniform mat4 projection;
uniform mat4 rotation;

void main () {
    gl_Position = projection * rotation * vec4(position, 1.0);
    coords = position;
}