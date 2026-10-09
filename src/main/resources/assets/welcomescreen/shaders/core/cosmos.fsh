#version 150

#moj_import <fog.glsl>

// A window into a deep sky: layers of stars and slow clouds lying ever deeper behind the surface it is cut in, each
// seen where the line from the eye through this point meets it, so they slide against each other as the eye moves.
// They whirl slowly round the middle; the middle glows, and the window darkens towards its edge, then rims in light.

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec3 Center;
uniform vec3 AxisA;
uniform vec3 AxisB;
uniform vec3 Normal;
uniform float Depth;
uniform float Clock;
uniform vec3 Deep;
uniform vec3 Light;
uniform float Glow;
uniform float Seed;

in vec3 worldPos;
in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

vec2 hash2(vec2 p) {
    return vec2(hash(p), hash(p + 17.31));
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)),
            u.x), u.y);
}

float clouds(vec2 p) {
    float sum = 0.0;
    float weight = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += weight * noise(p);
        p = p * 2.03 + vec2(1.7, 9.2);
        weight *= 0.5;
    }
    return sum;
}

vec2 turn(vec2 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// At most one star in each cell of a grid, cells a block across; each its own brightness, tint and twinkle.
vec3 stars(vec2 p, float cells, float size, float layer) {
    vec2 grid = p * cells;
    vec2 cell = floor(grid);
    vec2 inside = fract(grid);
    vec3 sum = vec3(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 near = cell + vec2(float(x), float(y));
            float bright = hash(near + Seed + layer * 7.1);
            if (bright < 0.45) {
                continue;
            }
            vec2 at = vec2(float(x), float(y)) + hash2(near + layer * 3.7 + Seed) - inside;
            float far = length(at) / cells;
            float twinkle = 0.65 + 0.35 * sin(Clock * (0.08 + 0.12 * hash(near + 4.2)) + bright * 40.0);
            float core = 1.0 - smoothstep(0.0, size, far);
            float halo = size * size * 0.08 / (far * far + size * size * 0.08);
            vec3 tint = mix(Light, vec3(1.0), hash(near + 9.9));
            sum += tint * (bright - 0.45) / 0.55 * twinkle * (core * 1.6 + halo * 0.6);
        }
    }
    return sum;
}

void main() {
    vec3 rel = worldPos - Center;
    vec2 surface = vec2(dot(rel, AxisA), dot(rel, AxisB));
    vec3 look = normalize(worldPos);
    float into = max(0.12, -dot(look, Normal));
    vec2 drift = vec2(dot(look, AxisA), dot(look, AxisB)) / into;
    float edge = 1.0 - vertexColor.a;

    vec3 color = Deep;
    for (int i = 0; i < 2; i++) {
        float layer = float(i);
        vec2 p = surface + drift * Depth * (1.5 + 2.5 * layer);
        float whirl = Clock * 0.004 * (1.0 - 1.7 * layer) + 0.6 / (0.6 + length(p) * 0.25);
        vec2 q = turn(p, whirl) * (0.22 - 0.06 * layer) + vec2(Seed * 0.37 + layer * 5.3, Clock * 0.001);
        float wisps = smoothstep(0.35, 0.85, clouds(q));
        vec3 tint = mix(Light * 0.55, mix(Light, vec3(0.3, 0.9, 1.0), 0.35) * 0.45, layer);
        color += tint * wisps * (0.55 - 0.15 * layer);
    }
    for (int i = 0; i < 4; i++) {
        float layer = float(i);
        vec2 p = surface + drift * Depth * (0.4 + 0.6 * layer + 0.45 * layer * layer);
        p = turn(p, Clock * 0.002 * (1.0 + layer));
        color += stars(p, 1.3 + 0.7 * layer, 0.09 - 0.015 * layer, layer);
    }
    float middle = 1.0 - smoothstep(0.0, 1.0, edge);
    color += Light * 0.25 * middle * middle;
    color *= mix(1.0, 0.35, smoothstep(0.55, 0.95, edge));
    color += Light * smoothstep(0.88, 1.0, edge) * 1.2;
    color *= Glow;
    fragColor = linear_fog(vec4(color, 1.0) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
