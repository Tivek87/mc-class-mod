#version 150

// A bubble of bent light: what lies behind it is seen as through a ball of glass, swollen in the middle, squeezed and
// smeared out towards the rim, with coloured fringes and a bright rim line.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform mat4 Projection;
uniform mat4 InverseProjection;
uniform vec3 Center;
uniform float Radius;
uniform float Strength;
uniform float Clock;
uniform vec3 Tint;

in vec2 texCoord;

out vec4 fragColor;

vec3 viewPoint(vec2 uv, float depth) {
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

void main() {
    vec2 uv = texCoord;
    vec3 ray = normalize(viewPoint(uv, 1.0));
    float b = dot(ray, Center);
    float h = b * b - dot(Center, Center) + Radius * Radius;
    if (h <= 0.0) {
        discard;
    }
    h = sqrt(h);
    float tIn = b - h;
    float tOut = b + h;
    if (tOut <= 0.0) {
        discard;
    }
    bool inside = tIn < 0.0;
    float t = inside ? tOut : tIn;
    // Whatever stands in front of the bubble's wall is seen as it is.
    float scene = length(viewPoint(uv, texture(DepthSampler, uv).r));
    if (scene < t) {
        discard;
    }
    vec3 normal = (ray * t - Center) / Radius;
    float off = clamp(length(Center - ray * b) / Radius, 0.0, 1.0);
    float apparent = Radius / max(length(Center), Radius * 1.05);
    float power = 0.75 * Strength * (inside ? 0.5 : 1.0);
    vec2 bend = normal.xy * vec2(Projection[0][0], Projection[1][1]) * 0.5 * apparent * power;
    // Ripples run out through the glass.
    float wave = sin(off * 20.0 - Clock * 0.6);
    bend *= 1.0 + 0.12 * wave * Strength;
    float smear = 0.4 * Strength * off;
    vec3 seen = vec3(0.0);
    for (int i = 0; i < 8; i++) {
        vec2 at = uv - bend * (1.0 + smear * float(i) / 7.0);
        vec2 fringe = bend * 0.1 * off;
        seen.r += texture(SceneSampler, at - fringe).r;
        seen.g += texture(SceneSampler, at).g;
        seen.b += texture(SceneSampler, at + fringe).b;
    }
    seen /= 8.0;
    float grey = dot(seen, vec3(0.299, 0.587, 0.114));
    seen = mix(seen, grey * Tint * 1.1, 0.18 * Strength);
    seen *= mix(vec3(1.0), Tint, 0.2 * Strength);
    float fresnel = pow(off, 5.0);
    float line = smoothstep(0.9, 0.975, off) * (1.0 - smoothstep(0.975, 1.0, off));
    seen += Tint * (0.25 * fresnel + 0.9 * line + 0.03 * (1.0 - off)) * Strength;
    vec3 base = texture(SceneSampler, uv).rgb;
    float edge = 1.0 - smoothstep(0.985, 1.0, off);
    fragColor = vec4(mix(base, seen, edge * clamp(Strength * 1.5, 0.0, 1.0)), 1.0);
}
