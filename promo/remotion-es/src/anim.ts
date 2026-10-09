import {Easing, interpolate, interpolateColors, spring, useCurrentFrame} from 'remotion';
import timeline from './timeline.json';
import {PILL_STOPS} from './theme';

export const FPS = timeline.fps;

export const TIME_SCALE = timeline.durationInFrames / timeline.designFrames;

export const useSceneFrame = () => useCurrentFrame() / TIME_SCALE;

export type SceneId = keyof typeof timeline.scenes;

export const scene = <S extends SceneId>(id: S) => {
  const s = timeline.scenes[id];
  return {from: s.from, dur: s.to - s.from, m: s.marks as (typeof timeline.scenes)[S]['marks']};
};

export const EMPH = Easing.bezier(0.05, 0.7, 0.1, 1);
export const EMPH_ACC = Easing.bezier(0.3, 0, 0.8, 0.15);
export const STD = Easing.bezier(0.2, 0, 0, 1);
export const IN_OUT = Easing.bezier(0.65, 0, 0.35, 1);

export const tw = (
  f: number,
  start: number,
  end: number,
  from = 0,
  to = 1,
  easing: (t: number) => number = EMPH,
) =>
  interpolate(f, [start, end], [from, to], {
    easing,
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

const springCfg = (stiffness: number, dampingRatio: number) => ({
  stiffness,
  damping: 2 * dampingRatio * Math.sqrt(stiffness),
  mass: 1,
});

export const SPATIAL = springCfg(300, 0.8);
export const SPATIAL_FAST = springCfg(600, 0.6);
export const SPATIAL_SLOW = springCfg(170, 0.8);
export const BOUNCY = springCfg(330, 0.55);

export const sp = (f: number, delay = 0, config = SPATIAL) =>
  spring({frame: f - delay, fps: FPS, config});

export const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

export const clamp01 = (v: number) => Math.min(1, Math.max(0, v));

export const pillColors = (progress: number) => {
  const p = clamp01(progress);
  const xs = PILL_STOPS.map((s) => s[0]);
  return {
    fill: interpolateColors(p, xs, PILL_STOPS.map((s) => s[1])),
    track: interpolateColors(p, xs, PILL_STOPS.map((s) => s[2])),
    content: interpolateColors(p, [0, 0.3, 0.5, 1], ['#0F2100', '#1C1E00', '#261A00', '#2D1300']),
  };
};

export const money = (v: number, decimals?: number) => {
  const fixed = decimals ?? (Math.abs(v - Math.round(v)) < 0.005 ? 0 : 2);
  const s = Math.abs(v).toLocaleString('en-US', {
    minimumFractionDigits: fixed,
    maximumFractionDigits: fixed,
  });
  return `${v < 0 ? '−' : ''}$${s}`;
};

export const countTo = (f: number, start: number, end: number, from: number, to: number) =>
  tw(f, start, end, from, to, STD);
