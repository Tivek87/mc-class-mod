#version 150

// A black hole laid over the finished picture: each ray from the eye is bent round the hole on its way through a ball
// about it, as light is round a true one, so the world behind is seen warped and ringed; rays that fall in show black,
// rays grazing it light a thin ring, and a disc of glowing matter whirling round it is seen above and below as well as
// in front, its near side brighter where it races towards the eye. Pull drags the whole picture in towards it, darker
// at the edges; Wave is a ring of bent light running out from it.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform mat4 Projection;
uniform mat4 InverseProjection;
uniform vec3 Center;
uniform float Horizon;
uniform float Bound;
uniform vec3 Axis;
uniform float Inner;
uniform float Outer;
uniform float Strength;
uniform float Collapse;
uniform float Clock;
uniform float Seed;
uniform vec3 Hot;
uniform vec3 Cool;
uniform float Pull;
uniform float Wave;
uniform float WaveStrength;

in vec2 texCoord;

out vec4 fragColor;

vec3 viewPoint(vec2 uv, float depth) {
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec2 toScreen(vec3 p) {
    vec4 clip = Projection * vec4(p, 1.0);
    return clip.xy / clip.w * 0.5 + 0.5;
}

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)),
            u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float weight = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += weight * noise(p);
        p = p * 2.07 + vec2(3.1, 7.7);
        weight *= 0.5;
    }
    return sum;
}

vec2 turn(vec2 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// The disc where a ray crosses it at q (in horizons from the middle), seen along v: its light and how much it hides.
// Inner rings turn faster than outer ones; two copies half a turn of the clock apart fade into each other so the swirl
// never winds up tighter and tighter.
vec4 disc(vec3 q, vec3 v, vec3 e1, vec3 e2) {
    float r = length(q);
    float inner = Inner * (1.0 - 0.55 * Collapse);
    float outer = Outer * (1.0 - 0.7 * Collapse);
    if (r < inner * 0.8 || r > outer) {
        return vec4(0.0);
    }
    vec2 plane = vec2(dot(q, e1), dot(q, e2));
    float omega = 1.6 / pow(max(r, 0.5), 1.5) * (1.0 + 2.5 * Collapse);
    float period = 60.0;
    float t1 = mod(Clock, period);
    float t2 = mod(Clock + period * 0.5, period);
    float first = 1.0 - abs(2.0 * t1 / period - 1.0);
    float n1 = fbm(turn(plane, -omega * t1 * 0.12) * vec2(1.3, 1.3) + Seed);
    float n2 = fbm(turn(plane, -omega * t2 * 0.12) * vec2(1.3, 1.3) + Seed + 5.3);
    float n = mix(n2, n1, first);
    float lanes = 0.5 + 0.5 * sin(r * 6.0 + n * 7.0 - Clock * 0.05);
    float heat = clamp(pow(inner / r, 0.9), 0.0, 1.0);
    vec3 tint = mix(Cool, Hot, heat * heat * heat);
    vec3 orbit = normalize(cross(Axis, q));
    float beta = 0.55 / sqrt(max(r, 1.0));
    float doppler = pow(1.0 / max(0.3, 1.0 - beta * dot(orbit, -v)), 3.0);
    float edge = smoothstep(inner * 0.8, inner * 1.15, r) * (1.0 - smoothstep(outer * 0.55, outer, r));
    float bright = (0.5 + 1.8 * n * n + 0.5 * lanes) * (0.6 + 2.4 * heat) * doppler * (1.0 + 3.0 * Collapse);
    float hides = clamp(edge * (0.45 + 0.8 * n) * (0.7 + 0.5 * heat), 0.0, 1.0);
    return vec4(tint * bright * edge, hides);
}

// The picture at `at`, smeared in towards the hole as hard as it pulls.
vec3 dragged(vec2 at) {
    if (Pull <= 0.001 || Center.z >= 0.0) {
        return texture(SceneSampler, at).rgb;
    }
    vec2 towards = toScreen(Center) - at;
    vec3 sum = vec3(0.0);
    for (int i = 0; i < 10; i++) {
        sum += texture(SceneSampler, at + towards * (float(i) / 9.0) * 0.035 * Pull).rgb;
    }
    return sum / 10.0;
}

void main() {
    vec2 uv = texCoord;
    vec3 ray = normalize(viewPoint(uv, 1.0));
    float scene = length(viewPoint(uv, texture(DepthSampler, uv).r));
    vec3 color = texture(SceneSampler, uv).rgb;
    bool touched = false;
    bool ahead = Center.z < 0.0;
    vec2 middle = ahead ? toScreen(Center) : vec2(0.5);

    if (Pull > 0.001 && ahead) {
        color = dragged(uv);
        touched = true;
    }

    float b = dot(ray, Center);

    // The ring of bent light running out, seen round the hole wherever its shell crosses the line of sight.
    if (WaveStrength > 0.001 && Wave > 0.0) {
        float across = length(Center - ray * b);
        float width = max(0.8, Wave * 0.14);
        float x = (across - Wave) / width;
        if (abs(x) < 1.0 && scene > b - Wave) {
            float push = sin(x * 3.14159) * (1.0 - x * x);
            vec2 away = normalize(uv - middle + vec2(1.0E-5));
            vec2 at = uv + away * push * 0.035 * WaveStrength;
            vec2 fringe = away * 0.005 * WaveStrength;
            vec3 seen = vec3(texture(SceneSampler, at - fringe).r, texture(SceneSampler, at).g,
                    texture(SceneSampler, at + fringe).b);
            color = seen + Hot * (1.0 - x * x) * (1.0 - abs(x)) * 0.35 * WaveStrength;
            touched = true;
        }
    }

    float reach = Bound * Horizon;
    float h = b * b - dot(Center, Center) + reach * reach;
    if (Strength > 0.001 && h > 0.0) {
        h = sqrt(h);
        float tIn = max(b - h, 0.0);
        float tOut = b + h;
        // Whatever stands in front of the ball is seen as it is.
        if (tOut > 0.0 && scene > tIn) {
            vec3 p = (ray * tIn - Center) / Horizon;
            vec3 v = ray;
            vec3 e1 = normalize(cross(Axis, abs(Axis.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
            vec3 e2 = cross(Axis, e1);
            vec3 spin = cross(p, v);
            float h2 = dot(spin, spin);
            vec4 seenDisc = vec4(0.0);
            vec3 haze = vec3(0.0);
            float ring = 0.0;
            bool swallowed = false;
            bool blocked = false;
            float side = dot(p, Axis);
            float travelled = 0.0;
            float limit = (scene - tIn) / Horizon;
            for (int i = 0; i < 90; i++) {
                float r = length(p);
                if (r < 1.0) {
                    swallowed = true;
                    break;
                }
                // Bending fades out towards the ball's edge, so the world outside it joins on without a seam.
                float fall = 1.0 - smoothstep(Bound * 0.5, Bound, r);
                vec3 accel = -1.5 * h2 * p / pow(r, 5.0) * fall;
                float dt = clamp(0.12 * r, 0.03, 0.8);
                v = normalize(v + accel * dt);
                vec3 next = p + v * dt;
                travelled += dt;
                float s2 = dot(next, Axis);
                if (side * s2 < 0.0) {
                    vec3 q = mix(p, next, side / (side - s2));
                    vec4 d = disc(q, v, e1, e2);
                    seenDisc.rgb += (1.0 - seenDisc.a) * d.rgb;
                    seenDisc.a += (1.0 - seenDisc.a) * d.a;
                }
                side = s2;
                // The disc is not quite flat: a haze of its light hangs just above and below it.
                float height = abs(s2);
                if (height < 0.6) {
                    vec4 d = disc(next - Axis * s2, v, e1, e2);
                    haze += (1.0 - seenDisc.a) * d.rgb * exp(-height / 0.18) * dt * 0.35;
                }
                ring += exp(-pow((r - 1.5) / 0.15, 2.0)) * dt;
                p = next;
                if (travelled > limit) {
                    blocked = true;
                    break;
                }
                if (length(p) > Bound && dot(p, v) > 0.0) {
                    break;
                }
            }
            vec3 back = vec3(0.0);
            if (!swallowed) {
                vec3 from = Center + p * Horizon;
                vec3 target = blocked ? from : from + v * max(scene - length(Center), Horizon * 2.0);
                if (target.z < -0.05) {
                    vec2 at = toScreen(target);
                    float off = max(max(-at.x, at.x - 1.0), max(-at.y, at.y - 1.0));
                    back = dragged(clamp(at, 0.001, 0.999)) * (1.0 - clamp(off * 4.0, 0.0, 0.7));
                }
            }
            // Only light that grazed the hole and got away again forms the ring; what fell in shows black.
            float glow = swallowed ? 0.0 : clamp(ring, 0.0, 2.0) * (1.0 + 2.0 * Collapse);
            // Its own light is eased towards white rather than cut off there, so the disc keeps its colour.
            vec3 shine = 1.0 - exp(-(seenDisc.rgb + haze + Hot * glow * 2.2) * 1.3);
            vec3 hole = back * (1.0 - seenDisc.a) + shine;
            color = mix(color, hole, Strength);
            touched = true;
        }
    }

    if (!touched) {
        discard;
    }
    // Dragged in, the whole picture greys and darkens round its edges, hole and all.
    if (Pull > 0.001 && ahead) {
        float grey = dot(color, vec3(0.299, 0.587, 0.114));
        color = mix(color, grey * mix(vec3(1.0), Cool, 0.4), 0.35 * Pull);
        float vignette = smoothstep(0.25, 0.85, length((uv - 0.5) * vec2(1.0, 0.8)));
        color *= 1.0 - 0.6 * Pull * vignette;
    }
    fragColor = vec4(color, 1.0);
}
