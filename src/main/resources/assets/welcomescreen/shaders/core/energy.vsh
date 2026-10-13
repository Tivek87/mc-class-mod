#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 uv;
out vec4 tint;
flat out ivec2 kind;

// UV0 is the place on the quad, UV2 the pattern to paint there and its seed.
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    uv = UV0;
    tint = Color;
    kind = UV2;
}
