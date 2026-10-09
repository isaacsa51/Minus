import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C} from '../theme';
import {lerp} from '../anim';

export const RING_SPRING = ['#C3FF95', '#FFF086', '#FFDBCD', '#D2E4FF', '#D4EC9E', '#EDE68C'];
export const RING_WARM = ['#FFF086', '#FFDBCD', '#FFECC6', '#C3FF95', '#EDE68C'];
export const RING_COOL = ['#D2E4FF', '#C3FF95', '#DEE6C5', '#FFF086', '#D4EC9E'];

export const Aura: React.FC<{
  f: number;
  ring?: string[];
  center?: string;
  open?: number;
  flat?: number;
  flatColor?: string;
  cx?: number;
  cy?: number;
  radius?: number;
  speed?: number;
}> = ({f, ring = RING_SPRING, center = C.surface, open = 1, flat = 0, flatColor = C.primaryContainer, cx = 540, cy = 960, radius = 470, speed = 0.7}) => {
  const rot = f * speed;
  const breathe = Math.sin(f / 26) * 26;
  const r1 = Math.max(0, lerp(0, radius, open) + breathe * open);
  const size = 2900;
  return (
    <AbsoluteFill style={{background: ring[0], overflow: 'hidden'}}>
      <div
        style={{
          position: 'absolute',
          left: cx - size / 2,
          top: cy - size / 2,
          width: size,
          height: size,
          borderRadius: '50%',
          background: `conic-gradient(from ${rot}deg, ${ring.join(', ')}, ${ring[0]})`,
          filter: 'blur(110px)',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: cx - size / 2,
          top: cy - size / 2,
          width: size,
          height: size,
          background: `radial-gradient(circle at center, ${center} 0px, ${center} ${r1 * 0.5}px, transparent ${r1 * 1.08}px)`,
          filter: 'blur(36px)',
        }}
      />
      {flat > 0 ? <AbsoluteFill style={{background: flatColor, opacity: flat}} /> : null}
    </AbsoluteFill>
  );
};

export const DotGrid: React.FC<{bg: string; dot: string; gap?: number; offsetX?: number; offsetY?: number}> = ({
  bg,
  dot,
  gap = 46,
  offsetX = 0,
  offsetY = 0,
}) => (
  <AbsoluteFill
    style={{
      backgroundColor: bg,
      backgroundImage: `radial-gradient(circle, ${dot} 2.6px, transparent 3.4px)`,
      backgroundSize: `${gap}px ${gap}px`,
      backgroundPosition: `${offsetX}px ${offsetY}px`,
    }}
  />
);
