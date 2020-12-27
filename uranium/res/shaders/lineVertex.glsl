
in vec3 inVertex;

uniform mat4 projection;
uniform mat4 view;
uniform vec3 position;

out vec3 color;

void main () {
    vec4 worldPos = vec4(position + inVertex, 1.0);
    vec4 positionRelativeToEye = view * worldPos;
    gl_Position = vec4(inVertex, 1.0);//projection * positionRelativeToEye;
    color = vec3(1.0, 1.0, 1.0);
}