#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 viewPos;
out vec3 viewNormal;
out vec3 localPos;
out vec4 paint;
out vec2 material;

// Galactus's boxes, lit in view space; the part's own height sets the pulse of what glows.
void main() {
    vec4 at = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * at;
    viewPos = at.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    localPos = Position;
    paint = Color;
    material = UV0;
}
