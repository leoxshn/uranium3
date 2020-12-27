
in vec2 pos;

uniform vec2 position;
uniform vec2 size;

out vec2 uv;

void main () {
    mat4 model = mat4(1.0);
    model[3].x = position.x;
    model[3].y = position.y;
    model[0].x = size.x;
    model[1].y = size.y;
    gl_Position = model * vec4(pos, 0, 1);
    uv = vec2(0.5 + pos.x / 2.0, 0.5 - pos.y / 2.0);
}