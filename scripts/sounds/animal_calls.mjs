// Builds the wild animals' calls that the game has no sound for, under
// src/main/resources/assets/welcomescreen/sounds/: duck/quack.ogg (four quacks, the first the loudest, each a little
// lower: they fall where Duck's QUACK act opens its bill, in ticks, 20 a second), duck/call.ogg (one soft quack),
// monkey/hoot.ogg (five rising hoots, where Monkey's HOOT act bounces), monkey/call.ogg (two short chirps),
// flamingo/honk.ogg (three nasal honks, where Flamingo's HONK act opens its bill), flamingo/display.ogg (four honks
// with low grunts between, where its DISPLAY act opens its bill), flamingo/call.ogg (a low two-note gabble) and
// elephant/trumpet.ogg (one long brassy blast that rises and falls). Each is a voice: a buzz of harmonics at the
// note's pitch, shaped by the mouth's resonances, made rough by a flutter, with a little breath under it. Change the
// numbers here and run it again rather than editing a sound by hand:
//   node scripts/sounds/animal_calls.mjs
// It needs ffmpeg with libvorbis on the PATH, or its path in the FFMPEG environment variable.
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RATE = 44100;
const PEAK = 0.8;
const SOUNDS = resolve(dirname(fileURLToPath(import.meta.url)), '../../src/main/resources/assets/welcomescreen/sounds');

// Each voice: its length (seconds); its notes (from, to, how loud, pitch at its start and at its end); `arch`, how
// far the pitch swells in the middle of a note; `roll`, how fast the harmonics die away (low: buzzy, high: pure);
// its resonances (pitch, sharpness, how loud); `dry`, how much of the plain buzz comes through; the flutter (how
// often a second, how deep); how unsteady its pitch is; its breath; and how fast a note comes in and dies.
const VOICES = {
    'duck/quack': {
        length: 1.35,
        notes: [[0.10, 0.32, 1.0, 330, 235], [0.40, 0.57, 0.8, 310, 225], [0.70, 0.86, 0.7, 300, 220],
            [1.00, 1.15, 0.6, 290, 210]],
        arch: 0.0, roll: 0.55,
        formants: [[1050, 3.0, 1.0], [2300, 4.0, 0.8], [3400, 5.0, 0.3]],
        dry: 0.1, flutter: [78, 0.55], jitter: 0.03, breath: 0.12, attack: 0.012, release: 0.06,
    },
    'duck/call': {
        length: 0.4,
        notes: [[0.05, 0.25, 1.0, 300, 220]],
        arch: 0.0, roll: 0.6,
        formants: [[1050, 3.0, 1.0], [2300, 4.0, 0.7], [3400, 5.0, 0.25]],
        dry: 0.1, flutter: [74, 0.5], jitter: 0.03, breath: 0.12, attack: 0.015, release: 0.08,
    },
    'monkey/hoot': {
        length: 1.75,
        notes: [[0.10, 0.27, 0.6, 520, 640], [0.40, 0.57, 0.7, 560, 700], [0.70, 0.87, 0.85, 600, 760],
            [1.00, 1.18, 1.0, 640, 820], [1.35, 1.58, 1.0, 680, 800]],
        arch: 0.06, roll: 1.3,
        formants: [[850, 2.5, 1.0], [1700, 4.0, 0.5]],
        dry: 0.25, flutter: [36, 0.12], jitter: 0.012, breath: 0.08, attack: 0.02, release: 0.07,
    },
    'monkey/call': {
        length: 0.45,
        notes: [[0.04, 0.13, 1.0, 1750, 1250], [0.2, 0.3, 0.8, 1650, 1150]],
        arch: 0.0, roll: 1.8,
        formants: [[1500, 2.0, 1.0], [2800, 4.0, 0.4]],
        dry: 0.3, flutter: [0, 0.0], jitter: 0.008, breath: 0.05, attack: 0.008, release: 0.04,
    },
    'elephant/trumpet': {
        length: 1.9,
        notes: [[0.04, 1.75, 1.0, 400, 370]],
        arch: 0.42, roll: 0.5,
        formants: [[720, 2.0, 1.0], [1650, 3.0, 0.9], [3100, 4.0, 0.5]],
        dry: 0.12, flutter: [52, 0.5], jitter: 0.035, breath: 0.22, attack: 0.03, release: 0.5,
    },
    'flamingo/honk': {
        length: 3.0,
        notes: [[0.22, 0.42, 1.0, 470, 400], [1.43, 1.63, 0.9, 460, 390], [2.64, 2.86, 0.85, 450, 375]],
        arch: 0.12, roll: 0.5,
        formants: [[900, 3.0, 1.0], [2200, 4.0, 0.7], [3300, 5.0, 0.3]],
        dry: 0.12, flutter: [60, 0.4], jitter: 0.03, breath: 0.1, attack: 0.02, release: 0.06,
    },
    'flamingo/display': {
        length: 4.1,
        notes: [[0.22, 0.40, 1.0, 480, 410], [0.80, 0.88, 0.35, 330, 300], [1.43, 1.61, 0.95, 470, 400],
            [2.01, 2.09, 0.35, 320, 295], [2.64, 2.82, 1.0, 490, 415], [3.22, 3.30, 0.35, 330, 300],
            [3.85, 4.03, 0.9, 470, 400]],
        arch: 0.1, roll: 0.5,
        formants: [[900, 3.0, 1.0], [2200, 4.0, 0.7], [3300, 5.0, 0.3]],
        dry: 0.12, flutter: [60, 0.4], jitter: 0.035, breath: 0.1, attack: 0.02, release: 0.06,
    },
    'flamingo/call': {
        length: 0.45,
        notes: [[0.04, 0.14, 1.0, 320, 280], [0.22, 0.34, 0.8, 300, 265]],
        arch: 0.05, roll: 0.6,
        formants: [[800, 3.0, 1.0], [2000, 4.0, 0.6]],
        dry: 0.15, flutter: [48, 0.45], jitter: 0.03, breath: 0.12, attack: 0.015, release: 0.05,
    },
};

// The same noise every run, so a sound only changes when the numbers do.
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

function smooth(x) {
    const t = Math.max(0, Math.min(1, x));
    return t * t * (3 - 2 * t);
}

function sing(voice, seed) {
    const samples = new Float64Array(Math.round(voice.length * RATE));
    const noise = random(seed);
    const mouth = voice.formants.map(([freq, q, gain]) => [bandPass(freq, q), gain]);
    const breath = bandPass(voice.formants[0][0] * 1.8, 1.2);
    const [rate, depth] = voice.flutter;
    let phase = 0;
    let drift = 0;
    for (let i = 0; i < samples.length; i++) {
        const t = i / RATE;
        let loud = 0;
        let pitch = voice.notes[0][3];
        for (const [from, to, level, high, low] of voice.notes) {
            if (t < from || t > to) {
                continue;
            }
            const along = (t - from) / (to - from);
            loud = level * smooth((t - from) / voice.attack) * smooth((to - t) / voice.release);
            pitch = (high + (low - high) * along) * (1 + voice.arch * Math.sin(Math.PI * Math.sqrt(along)));
        }
        // The pitch wanders a little, slowly, as a throat's does.
        drift += (noise() * 2 - 1 - drift) * 0.002;
        pitch *= 1 + voice.jitter * drift * 25;
        phase += 2 * Math.PI * pitch / RATE;
        let buzz = 0;
        for (let n = 1; n * pitch < RATE * 0.45 && n <= 40; n++) {
            buzz += Math.sin(n * phase) / Math.pow(n, voice.roll);
        }
        const hiss = breath(noise() * 2 - 1) * voice.breath * 6;
        const source = buzz * (1 - depth * (0.5 - 0.5 * Math.cos(2 * Math.PI * rate * t))) + hiss;
        let out = voice.dry * source;
        for (const [filter, gain] of mouth) {
            out += gain * filter(source);
        }
        samples[i] = loud * out;
    }
    return samples;
}

function master(samples) {
    let top = 0;
    for (let i = 0; i < samples.length; i++) {
        top = Math.max(top, Math.abs(samples[i]));
    }
    for (let i = 0; i < samples.length; i++) {
        samples[i] *= PEAK / top;
    }
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

const dir = mkdtempSync(join(tmpdir(), 'animal-calls-'));
const ffmpeg = process.env.FFMPEG || 'ffmpeg';
let seed = 21;
for (const [name, voice] of Object.entries(VOICES)) {
    const samples = sing(voice, seed++);
    master(samples);
    const out = join(SOUNDS, name + '.ogg');
    mkdirSync(dirname(out), { recursive: true });
    const source = join(dir, 'voice.wav');
    writeFileSync(source, wav(samples));
    const run = spawnSync(ffmpeg, ['-y', '-hide_banner', '-loglevel', 'error', '-i', source, '-c:a', 'libvorbis', '-q:a', '6',
        '-map_metadata', '-1', out], { stdio: 'inherit' });
    if (run.status !== 0) {
        rmSync(dir, { recursive: true, force: true });
        console.error('ffmpeg failed' + (run.error ? ': ' + run.error.message : ''));
        process.exit(1);
    }
    console.log('Wrote ' + out);
}
rmSync(dir, { recursive: true, force: true });
