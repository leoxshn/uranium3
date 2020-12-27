
in vec2 uv;

uniform sampler2D tex;
uniform vec3 ambientLight;

out vec4 outColor;

void main () {
    outColor = vec4(
        pow(ambientLight.r, 0.8),
        pow(ambientLight.g, 0.8),
        pow(ambientLight.b, 0.8),
    1.0) * texture(tex, uv);
}