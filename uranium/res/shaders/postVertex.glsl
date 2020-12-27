
in vec2 inVertex;
out vec2 uv;

void main () {
    gl_Position = vec4(inVertex, 0.0, 1.0);
    uv = vec2(0.5 + inVertex.x / 2.0, 0.5 + inVertex.y / 2.0);
}