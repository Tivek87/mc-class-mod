// Builds the kill confirm sound, src/main/resources/assets/welcomescreen/sounds/ui/kill_confirm.ogg: a sharp click
// that cuts through the fight, a punchy low thump with a little drive (so laptop speakers still carry it) and a short
// bright ring over them. Change the numbers here and run it again rather than editing the sound by hand:
//   node scripts/sounds/kill_confirm.mjs
// It needs ffmpeg with libvorbis on the PATH, or its path in the FFMPEG environment variable.
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RATE = 44100;
const LENGTH = 0.38;
const PEAK = 0.89;
const OUT = resolve(dirname(fileURLToPath(import.meta.url)), '../../src/main/resources/assets/welcomescreen/sounds/ui/kill_confirm.ogg');

// The same noise every run, so the sound only changes when the numbers do.
function random(seed) {
    return () => {
        seed = (seed + 0x6d2b79f5) | 0;
        let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

function click(samples) {
    const noise = random(7);
    const cut = Math.exp(-2 * Math.PI * 2200 / RATE);
    let last = 0;
    let lastOut = 0;
    for (let i = 0; i < samples.length; i++) {
        const t = i / RATE;
        if (t > 0.02) {
            break;
        }
        const raw = (noise() * 2 - 1) * Math.exp(-t / 0.0028);
        // One pole high pass: only the crack, none of the hiss's body.
        lastOut = cut * (lastOut + raw - last);
        last = raw;
        samples[i] += 0.75 * lastOut;
    }
}

function thump(samples) {
    let phase = 0;
    for (let i = 0; i < samples.length; i++) {
        const t = i / RATE;
        const pitch = 58 + (190 - 58) * Math.exp(-t / 0.022);
        phase += 2 * Math.PI * pitch / RATE;
        const attack = Math.min(1, t / 0.0015);
        const body = attack * Math.exp(-t / 0.075);
        samples[i] += 0.95 * Math.tanh(2.2 * Math.sin(phase)) / Math.tanh(2.2) * body;
    }
}

function ring(samples) {
    const start = 0.004;
    const partials = [
        [1870, 0.34, 0.13],
        [2990, 0.2, 0.085],
        [4430, 0.1, 0.045],
    ];
    for (let i = 0; i < samples.length; i++) {
        const t = i / RATE - start;
        if (t < 0) {
            continue;
        }
        const attack = Math.min(1, t / 0.001);
        let sum = 0;
        for (const [hz, amp, decay] of partials) {
            sum += amp * Math.sin(2 * Math.PI * hz * t) * Math.exp(-t / decay);
        }
        samples[i] += attack * sum;
    }
}

function master(samples) {
    const drive = 1.5;
    let top = 0;
    for (let i = 0; i < samples.length; i++) {
        samples[i] = Math.tanh(drive * samples[i]) / Math.tanh(drive);
        top = Math.max(top, Math.abs(samples[i]));
    }
    const fade = Math.round(0.02 * RATE);
    for (let i = 0; i < samples.length; i++) {
        const tail = Math.min(1, (samples.length - 1 - i) / fade);
        samples[i] *= PEAK / top * tail;
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
click(samples);
thump(samples);
ring(samples);
master(samples);
const dir = mkdtempSync(join(tmpdir(), 'kill-confirm-'));
const source = join(dir, 'kill_confirm.wav');
writeFileSync(source, wav(samples));
const ffmpeg = process.env.FFMPEG || 'ffmpeg';
const run = spawnSync(ffmpeg, ['-y', '-hide_banner', '-loglevel', 'error', '-i', source, '-c:a', 'libvorbis', '-q:a', '7',
    '-map_metadata', '-1', OUT], { stdio: 'inherit' });
rmSync(dir, { recursive: true, force: true });
if (run.status !== 0) {
    console.error('ffmpeg failed' + (run.error ? ': ' + run.error.message : ''));
    process.exit(1);
}
console.log('Wrote ' + OUT);
