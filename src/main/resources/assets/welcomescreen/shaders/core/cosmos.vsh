#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out vec3 worldPos;
out float vertexDistance;
out vec4 vertexColor;

// The window lies in the world as seen from the eye: the fragment shader looks through it from there.
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    worldPos = Position;
    vertexDistance = fog_distance(Position, FogShape);
    vertexColor = Color;
}
