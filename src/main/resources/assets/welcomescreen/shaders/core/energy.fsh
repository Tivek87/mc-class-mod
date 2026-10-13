#version 150

uniform float GameTime;

in vec2 uv;
in vec4 tint;
flat in ivec2 kind;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
            f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(17.1, 9.2);
        a *= 0.5;
    }
    return v;
}

// Each pattern gives how bright it is here and how far toward white-hot. On a round one the quad runs from -1 to 1
// both ways; on a band x counts along it and y runs from -1 to 1 across.

// Tongues of fire licking upward round a hot heart.
vec2 flame(vec2 p, float t, float s) {
    float n = fbm(vec2(p.x * 2.2 + s, p.y * 2.2 - t * 2.4));
    float n2 = fbm(vec2(p.x * 4.5 - s, p.y * 3.5 - t * 3.6));
    float r = length(vec2(p.x * 1.15, p.y * 0.9 + 0.12)) + (n - 0.5) * 0.55 + (n2 - 0.5) * 0.25 * (p.y + 1.0);
    float body = smoothstep(0.95, 0.25, r);
    return vec2(body * (0.5 + 0.6 * n2), smoothstep(0.5, 0.0, r));
}

// A six-pointed star of ice, slowly turning, glittering.
vec2 frost(vec2 p, float t, float s) {
    float r = length(p);
    float a = atan(p.y, p.x) + t * 0.35 + s;
    float spike = pow(abs(cos(a * 3.0)), 14.0) * smoothstep(1.0, 0.0, r);
    float fine = pow(abs(cos(a * 3.0 + 1.5708)), 30.0) * smoothstep(0.6, 0.0, r);
    float glitter = smoothstep(0.82, 1.0, noise(p * 14.0 + vec2(s, floor(t * 6.0)))) * smoothstep(1.0, 0.2, r);
    float core = exp(-r * r * 9.0);
    float haze = smoothstep(1.0, 0.0, r) * 0.22 * (0.6 + 0.4 * fbm(p * 3.0 + t * 0.3));
    return vec2(spike * 0.8 + fine * 0.5 + glitter * 0.7 + core * 0.9 + haze, core + glitter * 0.5);
}

// A ball of thin crackling threads, new every instant.
vec2 spark(vec2 p, float t, float s) {
    float r = length(p);
    float tt = floor(t * 14.0);
    float n = fbm(p * 2.6 + vec2(s + tt * 3.7, tt * 1.3));
    float n2 = fbm(p * 4.1 - vec2(tt * 2.1, s));
    float thread = 0.035 / (abs(n - 0.5) + 0.02) + 0.012 / (abs(n2 - 0.5) + 0.02);
    float mask = smoothstep(1.0, 0.15, r);
    float core = exp(-r * r * 14.0);
    return vec2(min(thread, 2.5) * mask * 0.45 + core * 0.7, clamp(thread * mask * 0.2 + core, 0.0, 1.0));
}

// Three arms of wind whirling round an empty eye.
vec2 gust(vec2 p, float t, float s) {
    float r = length(p);
    float a = atan(p.y, p.x);
    float arms = sin(a * 3.0 + r * 9.0 - t * 9.0 + s);
    float streak = smoothstep(0.5, 1.0, arms);
    float c = cos(t * 1.3);
    float d = sin(t * 1.3);
    float n = fbm(vec2(p.x * c - p.y * d, p.x * d + p.y * c) * 3.2 + s);
    float mask = smoothstep(1.0, 0.55, r) * smoothstep(0.05, 0.35, r);
    return vec2(streak * mask * (0.45 + 0.7 * n), streak * mask * 0.3);
}

// Grit and clods thrown outward through a haze of dust.
vec2 dust(vec2 p, float t, float s) {
    float r = length(p);
    vec2 out2 = p / max(r, 0.001);
    float clods = smoothstep(0.6, 0.78, noise(p * 5.0 - out2 * t * 1.3 + s));
    float grit = smoothstep(0.78, 0.95, noise(p * 16.0 - out2 * t * 2.2 + s));
    float haze = fbm(p * 2.5 + vec2(s, -t * 0.6)) * 0.4;
    float mask = smoothstep(1.0, 0.3, r) * smoothstep(0.0, 0.25, r + 0.15);
    return vec2((clods * 0.55 + grit * 0.5 + haze) * mask, 0.0);
}

// A stream rushing along its band: a bright core in a torn sheath, shards racing down it.
vec2 stream(vec2 p, float t, float s) {
    float v = abs(p.y);
    float n = fbm(vec2(p.x * 1.4 - t * 9.0, p.y * 2.5 + s));
    float n2 = fbm(vec2(p.x * 3.0 - t * 14.0, p.y * 6.0 - s));
    float core = exp(-v * v * 22.0);
    float body = smoothstep(1.0, 0.1, v + (n - 0.5) * 0.7);
    float shards = smoothstep(0.68, 0.9, n2) * smoothstep(1.0, 0.3, v);
    return vec2(body * (0.3 + 0.5 * n) + shards * 0.55 + core * 0.85, core + shards * 0.4);
}

// A jagged bolt leaping along its band, another shape every instant.
vec2 bolt(vec2 p, float t, float s) {
    float tt = floor(t * 18.0);
    float off = (fbm(vec2(p.x * 1.7 + tt * 5.3, s + tt)) - 0.5) * 1.3 + (noise(vec2(p.x * 7.0 - tt * 2.0, s)) - 0.5) * 0.25;
    float line = 0.05 / (abs(p.y - off * 0.75) + 0.03);
    float edge = smoothstep(1.0, 0.75, abs(p.y));
    float flicker = 0.7 + 0.3 * hash(vec2(tt, s));
    return vec2(min(line, 3.0) * edge * flicker * 0.55, clamp(line * 0.25, 0.0, 1.0) * edge);
}

// A rippling ring of force with a thin wake inside it.
vec2 ring(vec2 p, float t, float s) {
    float r = length(p);
    float n = fbm(p / max(r, 0.001) * 2.2 + vec2(s, t * 2.0));
    float d = r - 0.78 + (n - 0.5) * 0.12;
    float rim = exp(-d * d * 260.0);
    float wake = smoothstep(0.78, 0.2, r) * smoothstep(0.0, 0.6, r) * 0.16 * (0.5 + n);
    return vec2(rim + wake, rim * 0.5);
}

void main() {
    float t = GameTime * 1200.0;
    float s = float(kind.y) * 1.618;
    vec2 e;
    bool round = true;
    if (kind.x == 0) {
        e = flame(uv, t, s);
    } else if (kind.x == 1) {
        e = frost(uv, t, s);
    } else if (kind.x == 2) {
        e = spark(uv, t, s);
    } else if (kind.x == 3) {
        e = gust(uv, t, s);
    } else if (kind.x == 4) {
        e = dust(uv, t, s);
    } else if (kind.x == 5) {
        e = stream(uv, t, s);
        round = false;
    } else if (kind.x == 6) {
        e = bolt(uv, t, s);
        round = false;
    } else {
        e = ring(uv, t, s);
    }
    float edge = round ? smoothstep(1.0, 0.86, length(uv)) : 1.0;
    vec3 colour = mix(tint.rgb, vec3(1.0), clamp(e.y, 0.0, 1.0) * 0.32);
    fragColor = vec4(colour * e.x * edge * tint.a * 0.85, 1.0);
}
