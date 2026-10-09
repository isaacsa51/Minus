import {execSync} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const timeline = JSON.parse(fs.readFileSync(path.join(root, 'src/timeline.json'), 'utf8'));
const seconds = String(timeline.durationInFrames / timeline.fps);
const [videoIn, out] = process.argv.slice(2).map((p) => path.resolve(root, p));
const tmp = `${out}.tmp.mp4`;
const wav = path.resolve(root, '../assets/audio/minus_promo_mix.wav');
const q = (v) => `"${v}"`;
const args = ['-y', '-v', 'error', '-i', q(videoIn), '-i', q(wav), '-map', '0:v', '-map', '1:a', '-c:v', 'copy', '-c:a', 'aac', '-b:a', '320k', '-af', `atrim=end=${seconds}`, '-t', seconds, '-movflags', '+faststart', q(tmp)];
execSync(`npx remotion ffmpeg ${args.join(' ')}`, {cwd: root, stdio: 'inherit'});
fs.renameSync(tmp, out);
if (videoIn.endsWith('.render.mp4')) fs.rmSync(videoIn);
console.log(`muxed ${path.relative(root, out)} at ${seconds}s`);
