#version 150

// Galactus lit as the game lights its creatures: each face of his boxes flat, by how it faces the key light (the moon's,
// or the sun's by day) over a little ambient, the cosmos behind him tinting the faces turned its way violet and teal.
// Crack pixels (material x) glow and pulse each on its own (paint alpha: the pixel's number); what glows keeps its own
// colour (material y).

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

void main() {
    vec3 n = normalize(viewNormal);
    if (dot(n, -viewPos) < 0.0) {
        n = -n;
    }
    float crack = material.x;
    float glow = material.y;
    vec3 albedo = paint.rgb;
    float ndl = dot(n, KeyDir);
    float diffuse = max(ndl, 0.0) * 0.75 + 0.25;
    vec3 rim = RimColorA * max(dot(n, RimDirA), 0.0) + RimColorB * max(dot(n, RimDirB), 0.0);
    vec3 color = albedo * (Ambient + KeyColor * diffuse) + (albedo + 0.06) * rim * 0.35;
    if (crack > 0.0) {
        float pulse = 0.55 + 0.45 * sin(Clock * 0.11 + paint.a * 6.2832);
        float power = clamp(Energy + Charge, 0.0, 1.5);
        color = mix(color, vec3(0.62, 0.3, 1.0) * pulse * 1.4, crack * min(power, 1.0));
    }
    if (glow > 0.0) {
        float pulse = 0.85 + 0.15 * sin(Clock * 0.13 + localPos.y * 0.8);
        color = mix(color, paint.rgb * (1.5 + 0.8 * Charge) * pulse, glow);
    }
    fragColor = vec4(color * ColorModulator.rgb, ColorModulator.a);
}
