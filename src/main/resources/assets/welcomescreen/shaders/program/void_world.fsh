#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

// Time: 0..1 each second. Intensity: 0 = normal view, 1 = full effect.
uniform float Time;
uniform float Intensity;

out vec4 fragColor;

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float lumaAt(vec2 offset) {
    return luma(texture(DiffuseSampler, texCoord + offset * oneTexel).rgb);
}

void main() {
    vec3 color = texture(DiffuseSampler, texCoord).rgb;
    float light = luma(color);

    float tl = lumaAt(vec2(-1.0, -1.0));
    float t  = lumaAt(vec2( 0.0, -1.0));
    float tr = lumaAt(vec2( 1.0, -1.0));
    float l  = lumaAt(vec2(-1.0,  0.0));
    float r  = lumaAt(vec2( 1.0,  0.0));
    float bl = lumaAt(vec2(-1.0,  1.0));
    float b  = lumaAt(vec2( 0.0,  1.0));
    float br = lumaAt(vec2( 1.0,  1.0));
    float gx = (tr + 2.0 * r + br) - (tl + 2.0 * l + bl);
    float gy = (bl + 2.0 * b + br) - (tl + 2.0 * t + tr);
    float edge = clamp(length(vec2(gx, gy)) * 2.5, 0.0, 1.0);

    vec3 shadow = vec3(0.04, 0.015, 0.08) + vec3(0.16, 0.10, 0.24) * pow(light, 1.6);
    vec3 voidColor = shadow + vec3(0.62, 0.38, 1.0) * edge;

    // Pure white = a marked enemy's glow outline: keep it bright.
    float mark = smoothstep(0.97, 0.995, min(min(color.r, color.g), color.b));
    voidColor = mix(voidColor, vec3(1.0, 0.3, 0.35), mark);

    vec2 fromCenter = texCoord - vec2(0.5);
    float vignette = 1.0 - smoothstep(0.25, 0.95, length(fromCenter) * 1.3);
    voidColor *= mix(0.3, 1.0, vignette);
    voidColor *= 0.9 + 0.1 * sin(Time * 6.2831853);

    fragColor = vec4(mix(color, voidColor, clamp(Intensity, 0.0, 1.0)), 1.0);
}
