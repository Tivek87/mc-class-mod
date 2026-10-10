// Builds the Heavy Chainsaw's sounds in src/main/resources/assets/welcomescreen/sounds/saw/: a two-stroke engine
// (one firing each turn, its pop ringing through the muffler, driven hard into a buzzing rasp) with the chain whining
// round its bar. start: the cord pulled twice, the engine catching, revving and settling; idle, run and cut: loops
// that join end to start (idling unevenly, screaming at full throttle, bogging down and grinding through something);
// rev: one blip of the throttle. Change the numbers here and run it again rather than editing the sounds by hand:
//   node scripts/sounds/chainsaw.mjs
// It needs ffmpeg with libvorbis on the PATH, or its path in the FFMPEG environment variable.
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RATE = 44100;
const OUT = resolve(dirname(fileURLToPath(import.meta.url)), '../../src/main/resources/assets/welcomescreen/sounds/saw');

// The same noise every run, so the sounds only change when the numbers do.
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

function onePoleHigh(freq) {
    const low = onePoleLow(freq);
    return (x) => x - low(x);
}

// The engine at `rpm(t)` firings a second (one a turn), `load(t)` 0 free to 1 cutting hard, `chain(t)` how fast the
// chain runs (0 to 1), `skip` the chance an idling turn misfires.
function engine(length, rpm, load, chain, seed, skip = 0) {
    const out = new Float64Array(Math.round(length * RATE));
    const noise = random(seed);
    const body = [bandPass(160, 1.6), bandPass(390, 3.0), bandPass(820, 2.5), bandPass(2100, 2.0)];
    const gains = [1.0, 0.75, 0.5, 0.22];
    const whine = bandPass(3600, 4.0);
    const whineHigh = bandPass(6200, 5.0);
    const grind = bandPass(1300, 1.2);
    const rasp = onePoleHigh(900);
    const low = onePoleLow(9000);
    let phase = 0;
    let burst = 0;
    let burstAmp = 0;
    let chainPhase = 0;
    for (let i = 0; i < out.length; i++) {
        const t = i / RATE;
        const f = rpm(t);
        const l = load(t);
        phase += f / RATE * (1 + (noise() - 0.5) * 0.06 * (1 - l * 0.5));
        if (phase >= 1) {
            phase -= 1;
            const misfire = noise() < skip;
            burst = misfire ? 0 : 1;
            burstAmp = (0.8 + noise() * 0.4) * (1 + l * 0.3);
        }
        // The pop: a short hot burst of noise, falling off within the turn.
        const pop = burst * burstAmp * (noise() * 2 - 1);
        burst *= Math.exp(-1 / (RATE * 0.0035));
        let x = 0;
        for (let k = 0; k < body.length; k++) {
            x += body[k](pop) * gains[k];
        }
        // The exhaust pulse itself, a low thump each turn.
        x += 0.5 * Math.exp(-phase * 7) * Math.sin(2 * Math.PI * phase) * burstAmp;
        // Driven hard: the two-stroke's buzz.
        x = Math.tanh(x * (2.6 + 1.6 * l));
        x += rasp(x) * 0.35;
        // The chain round the bar: a whine riding on how fast it runs, roughened as teeth pass.
        const c = chain(t);
        chainPhase += c * 900 / RATE;
        const teeth = 0.6 + 0.4 * Math.sin(2 * Math.PI * chainPhase);
        const hiss = noise() * 2 - 1;
        x += c * c * teeth * (whine(hiss) * 1.4 + whineHigh(hiss) * 0.8);
        // Cutting: wood or flesh grinding against the teeth, chattering.
        const chatter = 0.55 + 0.45 * Math.sin(2 * Math.PI * t * (31 + 9 * Math.sin(t * 5.3)));
        x += l * chatter * grind(hiss) * 2.2;
        out[i] = low(x);
    }
    return out;
}

// The cord: a ratchet of clicks under a rising whoosh, `at` seconds in.
function cord(samples, at, length, seed) {
    const noise = random(seed);
    const swoosh = bandPass(1500, 0.8);
    const click = bandPass(4200, 6);
    const from = Math.round(at * RATE);
    const to = Math.min(samples.length, from + Math.round(length * RATE));
    let next = 0;
    for (let i = from; i < to; i++) {
        const u = (i - from) / (to - from);
        const env = Math.sin(Math.PI * u);
        let x = swoosh(noise() * 2 - 1) * 0.5 * env;
        if (i >= next) {
            next = i + Math.round(RATE * (0.018 - 0.01 * u));
            x += 3 * env;
        }
        samples[i] += x * 0.4 + click(x) * 0.6;
    }
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

function fade(samples, inSec, outSec) {
    const a = Math.round(inSec * RATE);
    const b = Math.round(outSec * RATE);
    for (let i = 0; i < a && i < samples.length; i++) {
        samples[i] *= i / a;
    }
    for (let i = 0; i < b && i < samples.length; i++) {
        samples[samples.length - 1 - i] *= i / b;
    }
    return samples;
}

// Made `extra` seconds long beyond `length`: its tail is laid over its start, so the end runs on into the start.
function loop(make, length, extra) {
    const raw = make(length + extra);
    const n = Math.round(length * RATE);
    const x = Math.round(extra * RATE);
    const out = raw.slice(0, n);
    for (let i = 0; i < x; i++) {
        const w = i / x;
        out[i] = raw[i] * w + raw[n + i] * (1 - w);
    }
    return out;
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
const IDLE = 46;
const FULL = 205;

const sounds = {
    start: (() => {
        const length = 2.1;
        const s = new Float64Array(Math.round(length * RATE));
        cord(s, 0.0, 0.28, 11);
        cord(s, 0.42, 0.3, 12);
        // Catching at 0.68 s, flaring up to full and settling to its idle.
        const caught = 0.68;
        const run = engine(length - caught, (t) => t < 0.05 ? 20 + 1600 * t : t < 0.35 ? FULL * ease((t - 0.05) / 0.12)
            + 10 : IDLE + (FULL - IDLE) * (1 - ease((t - 0.35) / 0.6)),
        (t) => 0, (t) => t < 0.35 ? ease(t / 0.2) : 1 - 0.85 * ease((t - 0.35) / 0.6), 21, 0.08);
        const offset = Math.round(caught * RATE);
        for (let i = 0; i < run.length; i++) {
            s[offset + i] += run[i] * ease(i / (RATE * 0.02));
        }
        return fade(scale(s, 0.85), 0.005, 0.25);
    })(),
    idle: scale(loop((len) => engine(len, (t) => IDLE + 3 * Math.sin(t * 2.1), () => 0, () => 0.12, 31, 0.12), 1.6,
        0.2), 0.62),
    run: scale(loop((len) => engine(len, (t) => FULL + 6 * Math.sin(t * 3.7), () => 0, () => 1, 41), 1.2, 0.15), 0.86),
    cut: scale(loop((len) => engine(len, (t) => FULL * 0.78 + 14 * Math.sin(t * 4.3) + 8 * Math.sin(t * 11.0),
        (t) => 0.75 + 0.25 * Math.sin(t * 6.1), () => 0.85, 51), 1.4, 0.15), 0.9),
    rev: fade(scale(engine(0.85, (t) => t < 0.12 ? IDLE + (FULL - IDLE) * ease(t / 0.12) : t < 0.38 ? FULL
        : FULL - (FULL - IDLE) * ease((t - 0.38) / 0.4), () => 0, (t) => t < 0.12 ? ease(t / 0.12) : t < 0.38 ? 1
        : 1 - 0.85 * ease((t - 0.38) / 0.4), 61), 0.88), 0.01, 0.12),
};

mkdirSync(OUT, { recursive: true });
const dir = mkdtempSync(join(tmpdir(), 'chainsaw-'));
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
