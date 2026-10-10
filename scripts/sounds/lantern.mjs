// Builds the Power Battery's charge in src/main/resources/assets/welcomescreen/sounds/lantern/: charge, from the ring's
// touch on, a whirr spinning up (a rich hum rising in pitch, fluttering faster as it goes, a thin whine over it) with
// the crackle of light in its glass thickening, until the ring is full at FULL seconds; then a bright shimmer as the
// whirr winds down. Change the numbers here and run it again rather than editing the sound by hand:
//   node scripts/sounds/lantern.mjs
// It needs ffmpeg with libvorbis on the PATH, or its path in the FFMPEG environment variable.
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RATE = 44100;
const OUT = resolve(dirname(fileURLToPath(import.meta.url)), '../../src/main/resources/assets/welcomescreen/sounds/lantern');
// From the touch to the ring full: PowerRing.RECHARGE_BACK - RECHARGE_HIT ticks.
const FULL = 1.4;
const LENGTH = 2.4;

// The same noise every run, so the sound only changes when the numbers do.
function random(seed) {
    return () => {
        seed = (seed + 0x6d2b79f5) | 0;
        let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

// A band pass (RBJ biquad) that keeps its state from sample to sample.
function bandPass(freq, q) {
    const w = 2 * Math.PI * freq / RATE;
    const alpha = Math.sin(w) / (2 * q);
    const a0 = 1 + alpha;
    const b0 = alpha / a0;
    const b2 = -alpha / a0;
    const a1 = -2 * Math.cos(w) / a0;
    const a2 = (1 - alpha) / a0;
    let x1 = 0, x2 = 0, y1 = 0, y2 = 0;
    return (x) => {
        const y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = x;
        y2 = y1;
        y1 = y;
        return y;
    };
}

function onePoleLow(freq) {
    const k = 1 - Math.exp(-2 * Math.PI * freq / RATE);
    let y = 0;
    return (x) => (y += k * (x - y));
}

function scale(samples, peak) {
    let most = 0;
    for (const s of samples) {
        most = Math.max(most, Math.abs(s));
    }
    for (let i = 0; i < samples.length; i++) {
        samples[i] *= peak / Math.max(most, 1e-9);
    }
    return samples;
}

function wav(samples) {
    const data = Buffer.alloc(samples.length * 2);
    samples.forEach((s, i) => data.writeInt16LE(Math.round(Math.max(-1, Math.min(1, s)) * 32767), i * 2));
    const head = Buffer.alloc(44);
    head.write('RIFF', 0);
    head.writeUInt32LE(36 + data.length, 4);
    head.write('WAVEfmt ', 8);
    head.writeUInt32LE(16, 16);
    head.writeUInt16LE(1, 20);
    head.writeUInt16LE(1, 22);
    head.writeUInt32LE(RATE, 24);
    head.writeUInt32LE(RATE * 2, 28);
    head.writeUInt16LE(2, 32);
    head.writeUInt16LE(16, 34);
    head.write('data', 36);
    head.writeUInt32LE(data.length, 40);
    return Buffer.concat([head, data]);
}

const ease = (u) => (u <= 0 ? 0 : u >= 1 ? 1 : u * u * (3 - 2 * u));

// How far the spin-up has come at t: rising to full at FULL, winding down after.
function spin(t) {
    return t < FULL ? ease(t / FULL) * 0.85 + 0.15 * (t / FULL) : 1 - 0.75 * ease((t - FULL) / (LENGTH - FULL));
}

function charge() {
    const n = Math.round(LENGTH * RATE);
    const out = new Float64Array(n);
    const rnd = random(7);
    const hum = bandPass(420, 0.9);
    const body = onePoleLow(2600);
    const whine = bandPass(2400, 6);
    const crackle = bandPass(3600, 1.4);
    let phase = 0;
    let whinePhase = 0;
    let flutter = 0;
    let spark = 0;
    for (let i = 0; i < n; i++) {
        const t = i / RATE;
        const s = spin(t);
        const freq = 70 + 230 * s;
        phase += freq / RATE;
        whinePhase += freq * 7.0 / RATE;
        flutter += (9 + 26 * s) / RATE;
        // A saw's rich hum, its harmonics opened as it spins up, pulsing like a rotor.
        const saw = 2 * (phase % 1) - 1;
        const pulse = 0.72 + 0.28 * Math.sin(2 * Math.PI * flutter);
        let x = body(hum(saw) * 2.2 + saw * 0.25 * s) * pulse;
        x += 0.12 * s * whine(Math.sin(2 * Math.PI * whinePhase) + 0.3 * (rnd() - 0.5));
        x += 0.18 * Math.sin(2 * Math.PI * 55 * t) * (0.6 + 0.4 * s);
        // Sparks of crackle, thicker as the charge builds: each a short snap of bright noise.
        const rate = t < FULL ? 8 + 70 * ease(t / FULL) : 78 * (1 - ease((t - FULL) / 0.5));
        if (rnd() < rate / RATE) {
            spark = 0.5 + 0.5 * rnd();
        }
        spark *= Math.exp(-1 / (RATE * 0.004));
        x += crackle((rnd() * 2 - 1) * spark) * 0.6;
        // The shimmer as it is full: bright partials ringing out.
        if (t >= FULL) {
            const u = t - FULL;
            const ring = Math.exp(-u * 3.2) * ease(u / 0.01);
            for (const [f, a] of [[880, 0.16], [1318, 0.12], [1760, 0.09], [2637, 0.05]]) {
                x += a * ring * Math.sin(2 * Math.PI * f * u + f);
            }
        }
        const swell = t < FULL ? 0.35 + 0.65 * ease(t / FULL) : 1;
        out[i] = x * swell * ease(t / 0.03) * (1 - ease((t - (LENGTH - 0.5)) / 0.5));
    }
    return scale(out, 0.85);
}

const sounds = { charge: charge() };

mkdirSync(OUT, { recursive: true });
const dir = mkdtempSync(join(tmpdir(), 'lantern-'));
const ffmpeg = process.env.FFMPEG || 'ffmpeg';
let failed = false;
for (const [name, samples] of Object.entries(sounds)) {
    const source = join(dir, name + '.wav');
    writeFileSync(source, wav(samples));
    const run = spawnSync(ffmpeg, ['-y', '-hide_banner', '-loglevel', 'error', '-i', source, '-c:a', 'libvorbis', '-q:a',
        '6', '-map_metadata', '-1', join(OUT, name + '.ogg')], { stdio: 'inherit' });
    if (run.status !== 0) {
        console.error('ffmpeg failed on ' + name + (run.error ? ': ' + run.error.message : ''));
        failed = true;
    } else {
        console.log('Wrote ' + join(OUT, name + '.ogg'));
    }
}
rmSync(dir, { recursive: true, force: true });
process.exit(failed ? 1 : 0);
