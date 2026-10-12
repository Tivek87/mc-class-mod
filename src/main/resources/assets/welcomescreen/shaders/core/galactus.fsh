#version 150

// Galactus lit: his paint (alpha: how metallic), worn and blotched; a key light with a soft wrap and a shine on the
// metal; rim lights in violet and teal round his edges as the cosmos behind him lights them; crystalline cracks of
// violet energy pulsing in patches over his armour (material x: how far they show) and what glows in its own colour
// (material y).

uniform vec4 ColorModulator;
uniform vec3 KeyDir;
uniform vec3 KeyColor;
uniform vec3 Ambient;
uniform vec3 RimDirA;
uniform vec3 RimColorA;
uniform vec3 RimDirB;
uniform vec3 RimColorB;
uniform float Clock;
uniform float Energy;
uniform float Charge;

in vec3 viewPos;
in vec3 viewNormal;
in vec3 localPos;
in vec4 paint;
in vec2 material;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

// How far p lies from the nearest wall between cells, and the nearest cell's own number.
vec2 cells(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    float near = 8.0;
    float next = 8.0;
    float id = 0.0;
    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 g = vec3(float(x), float(y), float(z));
                vec3 o = vec3(hash(i + g), hash(i + g + 19.19), hash(i + g + 47.53));
                vec3 r = g + o - f;
                float d = dot(r, r);
                if (d < near) {
                    next = near;
                    near = d;
                    id = hash(i + g + 3.7);
                } else if (d < next) {
                    next = d;
                }
            }
        }
    }
    return vec2(sqrt(next) - sqrt(near), id);
}

void main() {
    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float facing = dot(n, v);
    if (facing < 0.0) {
        n = -n;
        facing = -facing;
    }
    float metal = paint.a;
    float crack = material.x;
    float glow = material.y;

    float grain = noise(localPos * 2.1);
    float blotch = noise(localPos * 0.4 + vec3(7.0, 2.0, 5.0));
    vec3 albedo = paint.rgb * (0.8 + 0.2 * grain + 0.22 * blotch);

    float ndl = dot(n, KeyDir);
    float diffuse = max(ndl, 0.0) * 0.8 + max((ndl + 0.6) / 1.6, 0.0) * 0.3;
    vec3 h = normalize(KeyDir + v);
    float shine = pow(max(dot(n, h), 0.0), mix(14.0, 48.0, metal)) * mix(0.1, 0.85, metal) * (0.7 + 0.3 * grain);
    float edge = pow(1.0 - facing, 2.2);
    vec3 rim = edge * (RimColorA * smoothstep(-0.3, 0.8, dot(n, RimDirA))
            + RimColorB * smoothstep(-0.3, 0.8, dot(n, RimDirB)));
    vec3 color = albedo * (Ambient + KeyColor * diffuse)
            + KeyColor * shine * mix(vec3(1.0), albedo * 2.4 + 0.2, metal * 0.7)
            + rim * mix(0.6, 1.1, metal);

    if (crack > 0.0) {
        float power = crack * (Energy + Charge);
        vec2 c = cells(localPos * 0.62);
        float line = 1.0 - smoothstep(0.0, 0.035, c.x);
        float patch = smoothstep(0.6, 0.82, noise(localPos * 0.21 + vec3(3.1, 1.7, 5.3)) + 0.2 * Charge);
        float pulse = 0.55 + 0.45 * sin(Clock * 0.11 + c.y * 6.2832 + blotch * 5.0);
        vec3 violet = vec3(0.62, 0.3, 1.0);
        color += violet * line * patch * pulse * power * 1.6;
        color += vec3(0.3, 0.1, 0.5) * patch * patch * (0.2 + 0.8 * c.y) * pulse * power * 0.25;
    }
    if (glow > 0.0) {
        float pulse = 0.85 + 0.15 * sin(Clock * 0.13 + localPos.y * 0.8);
        color = mix(color, paint.rgb * (1.5 + 0.8 * Charge) * pulse, glow);
    }
    fragColor = vec4(color * ColorModulator.rgb, ColorModulator.a);
}
