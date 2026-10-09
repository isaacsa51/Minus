import React from 'react';
import {C, gsf} from '../theme';
import {clamp01, EMPH, tw} from '../anim';

export const Cursor: React.FC<{
  x: number;
  y: number;
  f: number;
  presses?: number[];
  tag?: string;
  tagIn?: number;
  opacity?: number;
  size?: number;
}> = ({x, y, f, presses = [], tag, tagIn = 0, opacity = 1, size = 78}) => {
  let press = 0;
  let ripple: {t: number} | null = null;
  for (const p of presses) {
    const d = f - p;
    if (d >= -4 && d < 10) press = Math.max(press, d < 0 ? (d + 4) / 4 : 1 - d / 10);
    if (d >= 0 && d < 22) ripple = {t: d / 22};
  }
  const scale = 1 - 0.16 * clamp01(press);
  const tagScale = tag ? tw(f, tagIn, tagIn + 14, 0, 1, EMPH) : 0;
  return (
    <div style={{position: 'absolute', left: x, top: y, opacity, pointerEvents: 'none'}}>
      {ripple ? (
        <div
          style={{
            position: 'absolute',
            left: -60 * (0.3 + ripple.t),
            top: -60 * (0.3 + ripple.t),
            width: 120 * (0.3 + ripple.t),
            height: 120 * (0.3 + ripple.t),
            borderRadius: '50%',
            border: `5px solid ${C.onSurface}`,
            opacity: 0.45 * (1 - ripple.t),
          }}
        />
      ) : null}
      <svg
        width={size}
        height={size * 1.25}
        viewBox="-3 -3 46 58"
        style={{transform: `scale(${scale}) rotate(-8deg)`, transformOrigin: '0 0', filter: 'drop-shadow(0 6px 10px rgba(26,28,21,0.28))'}}
      >
        <path d="M0,0 L0,40 L10,31 L17,48 L25,44.5 L18,28 L31,28 Z" fill="#FFFFFF" stroke={C.onSurface} strokeWidth={3.2} strokeLinejoin="round" />
      </svg>
      {tag ? (
        <div
          style={{
            position: 'absolute',
            left: size * 0.52,
            top: size * 0.92,
            transform: `rotate(-7deg) scale(${tagScale})`,
            transformOrigin: '0 0',
            background: C.onSurface,
            color: '#C3FF95',
            borderRadius: 14,
            padding: '8px 20px 10px',
            whiteSpace: 'nowrap',
            ...gsf(700, 112),
            fontSize: 36,
            boxShadow: '0 8px 18px rgba(26,28,21,0.22)',
          }}
        >
          {tag}
        </div>
      ) : null}
    </div>
  );
};
