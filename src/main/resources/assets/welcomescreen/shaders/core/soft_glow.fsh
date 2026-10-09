#version 150

#moj_import <fog.glsl>

// The lightning shader's glow, fading out over its last stretch (Softness blocks at most) before the line where it runs
// into a block or a creature, so it never ends in a hard line there; glow lying along a surface never meets it and keeps
// its strength. Softness 0 draws it as the lightning shader does.

uniform sampler2D DepthSampler;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform mat4 InverseProjection;
uniform vec2 SceneSize;
uniform float Softness;

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

vec3 seen(vec2 uv, float depth) {
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec3 scene(vec2 uv) {
    return seen(uv, textureLod(DepthSampler, uv, 0.0).r);
}

// Toward the neighbour on the side nearest in depth, so an edge in the scene beside the pixel never tilts its surface.
vec3 toward(vec3 here, vec3 before, vec3 after) {
    return abs(after.z - here.z) < abs(here.z - before.z) ? after - here : here - before;
}

// A glow fades over at most this share of the stretch it has left before its own dim edge, so a thin line keeps its
// light and only a wide glow fades over the whole Softness.
const float SHARE = 0.25;

void main() {
    vec4 color = vertexColor * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
    if (Softness > 0.0) {
        vec2 uv = gl_FragCoord.xy / SceneSize;
        vec3 glow = seen(uv, gl_FragCoord.z);
        vec3 right = dFdx(glow);
        vec3 down = dFdy(glow);
        vec3 facing = cross(right, down);
        vec2 dimming = vec2(dFdx(vertexColor.a) / max(length(right), 1e-9),
                dFdy(vertexColor.a) / max(length(down), 1e-9));
        float softness = min(Softness, SHARE * vertexColor.a / max(length(dimming), 1e-6));
        float depth = textureLod(DepthSampler, uv, 0.0).r;
        if (depth < 1.0 && softness > 1e-4) {
            vec3 behind = seen(uv, depth);
            vec2 texel = 1.0 / SceneSize;
            vec3 across = toward(behind, scene(uv - vec2(texel.x, 0.0)), scene(uv + vec2(texel.x, 0.0)));
            vec3 up = toward(behind, scene(uv - vec2(0.0, texel.y)), scene(uv + vec2(0.0, texel.y)));
            vec3 surface = cross(across, up);
            surface /= max(length(surface), 1e-20);
            float meeting = length(cross(facing / max(length(facing), 1e-20), surface));
            float reach = abs(dot(glow - behind, surface)) / max(meeting, 1e-3);
            color.a *= smoothstep(0.0, softness, reach);
        }
        if (color.a <= 0.0) {
            discard;
        }
    }
    fragColor = color;
}
