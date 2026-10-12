// Builds the owl's hoot, src/main/resources/assets/welcomescreen/sounds/owl/hoot.ogg: a tawny owl's call, one long
// soft note, a pause, two short ones and a long quavering one, each a nearly pure tone that slides up as it starts
// and sinks as it dies, with a little breath under it. The notes fall where Owl's HOOT act swells its throat (in
// ticks, 20 a second). Change the numbers here and run it again rather than editing the sound by hand:
//   node scripts/sounds/owl_hoot.mjs
// It needs ffmpeg with libvorbis on the PATH, or its path in the FFMPEG environment variable.
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RATE = 44100;
const LENGTH = 2.4;
const PEAK = 0.8;
const PITCH = 590;
// Each note: from, to (seconds), how loud, and whether it quavers.
const NOTES = [
    [0.10, 0.55, 1.0, false],
    [0.95, 1.15, 0.7, false],
    [1.25, 1.45, 0.75, false],
    [1.55, 2.25, 1.0, true],
];
const OUT = resolve(dirname(fileURLToPath(import.meta.url)), '../../src/main/resources/assets/welcomescreen/sounds/owl/hoot.ogg');

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

function smooth(x) {
    const t = Math.max(0, Math.min(1, x));
    return t * t * (3 - 2 * t);
}

function hoot(samples) {
    const noise = random(11);
    const breath = bandPass(PITCH * 1.6, 1.4);
    let phase = 0;
    for (let i = 0; i < samples.length; i++) {
        const t = i / RATE;
        let loud = 0;
        let pitch = PITCH;
        for (const [from, to, level, quaver] of NOTES) {
            if (t < from || t > to) {
                continue;
            }
            const along = (t - from) / (to - from);
            loud = level * smooth((t - from) / 0.07) * smooth((to - t) / 0.14);
            pitch = PITCH * (1 - 0.07 * Math.exp(-(t - from) / 0.05)) * (1 - 0.06 * along * along);
            if (quaver) {
                const shake = smooth((t - from) / 0.12);
                loud *= 1 - 0.42 * shake * (0.5 - 0.5 * Math.cos(2 * Math.PI * 11 * (t - from)));
                pitch *= 1 + 0.012 * shake * Math.sin(2 * Math.PI * 11 * (t - from));
            }
        }
        phase += 2 * Math.PI * pitch / RATE;
        const tone = Math.sin(phase) + 0.09 * Math.sin(2 * phase) + 0.03 * Math.sin(3 * phase);
        samples[i] = loud * (tone + 0.05 * breath(noise() * 2 - 1));
    }
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

const samples = new Float64Array(Math.round(LENGTH * RATE));
hoot(samples);
master(samples);
mkdirSync(dirname(OUT), { recursive: true });
const dir = mkdtempSync(join(tmpdir(), 'owl-hoot-'));
const source = join(dir, 'hoot.wav');
writeFileSync(source, wav(samples));
const ffmpeg = process.env.FFMPEG || 'ffmpeg';
const run = spawnSync(ffmpeg, ['-y', '-hide_banner', '-loglevel', 'error', '-i', source, '-c:a', 'libvorbis', '-q:a', '6',
    '-map_metadata', '-1', OUT], { stdio: 'inherit' });
rmSync(dir, { recursive: true, force: true });
if (run.status !== 0) {
    console.error('ffmpeg failed' + (run.error ? ': ' + run.error.message : ''));
    process.exit(1);
}
console.log('Wrote ' + OUT);
