import {Easing} from 'remotion';
import {TileKey, TileXf} from './Logo';

const overshoot = (t: number, tension = 2) => {
  const x = t - 1;
  return x * x * ((tension + 1) * x + tension) + 1;
};
const fastOutSlowIn = Easing.bezier(0.4, 0, 0.2, 1);

const AVD: Record<TileKey, {offset: number; duration: number}> = {
  plus: {offset: 0, duration: 280},
  minus: {offset: 70, duration: 280},
  times: {offset: 140, duration: 280},
  cookie: {offset: 220, duration: 380},
};

export const SPLASH_MS = 600;

export const splashTiles = (ms: number): Partial<Record<TileKey, TileXf>> =>
  Object.fromEntries(
    (Object.keys(AVD) as TileKey[]).map((k) => {
      const {offset, duration} = AVD[k];
      const p = Math.min(1, Math.max(0, (ms - offset) / duration));
      const s = p <= 0 ? 0 : overshoot(p);
      const r = k === 'cookie' ? -60 + 60 * fastOutSlowIn(p) : 0;
      return [k, {s, r, o: p > 0 ? 1 : 0}];
    }),
  );
