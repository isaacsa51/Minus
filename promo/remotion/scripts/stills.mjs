import path from 'node:path';
import {bundle} from '@remotion/bundler';
import {renderStill, selectComposition} from '@remotion/renderer';

const root = path.resolve(import.meta.dirname, '..');
const outDir = path.resolve(root, process.argv[2] ?? '../research/review');
const frames = (process.argv[3] ?? '0,36,100,150,200,240,270,320,380,420,470,540,600,650,700,740,800,900,960,1000,1060,1100,1160,1220,1280,1340,1400,1450,1499')
  .split(',')
  .map(Number);
const scale = Number(process.argv[4] ?? 0.5);

const serveUrl = await bundle({entryPoint: path.join(root, 'src/index.ts'), publicDir: path.resolve(root, '../assets')});
const composition = await selectComposition({serveUrl, id: 'MinusPromo'});
for (const frame of frames) {
  const output = path.join(outDir, `f${String(frame).padStart(4, '0')}.png`);
  await renderStill({composition, serveUrl, output, frame, scale, imageFormat: 'png'});
  console.log('rendered', output);
}
