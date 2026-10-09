import React from 'react';
import {C, gsf} from '../theme';
import {EMPH, EMPH_ACC, sp, SPATIAL, tw} from '../anim';

export const Headline: React.FC<{
  f: number;
  lines: string[];
  start?: number;
  stagger?: number;
  size?: number;
  color?: string;
  wght?: number;
  wdth?: number;
  exit?: number;
  align?: 'left' | 'center';
  lineHeight?: number;
  style?: React.CSSProperties;
}> = ({f, lines, start = 0, stagger = 4, size = 104, color = C.onSurface, wght = 760, wdth = 112, exit, align = 'left', lineHeight = 1.02, style}) => {
  let i = 0;
  const out = exit !== undefined ? tw(f, exit, exit + 14, 0, 1, EMPH_ACC) : 0;
  return (
    <div style={{...gsf(wght, wdth), fontSize: size, lineHeight, color, textAlign: align, letterSpacing: -size * 0.012, ...style}}>
      {lines.map((line, li) => (
        <div key={li} style={{whiteSpace: 'nowrap'}}>
          {line.split(' ').map((word, wi, arr) => {
            const d = start + i++ * stagger;
            const p = sp(f, d, SPATIAL);
            const o = tw(f, d, d + 10, 0, 1, EMPH);
            const blur = (1 - tw(f, d, d + 16, 0, 1, EMPH)) * 18 + out * 16;
            return (
              <span
                key={wi}
                style={{
                  display: 'inline-block',
                  marginRight: wi < arr.length - 1 ? size * 0.24 : 0,
                  opacity: o * (1 - out),
                  filter: blur > 0.1 ? `blur(${blur}px)` : undefined,
                  transform: `translateY(${(1 - p) * size * 0.55 - out * size * 0.3}px)`,
                }}
              >
                {word}
              </span>
            );
          })}
        </div>
      ))}
    </div>
  );
};

export const Tag: React.FC<{text: string; bg?: string; color?: string; size?: number; style?: React.CSSProperties}> = ({
  text,
  bg = C.onPrimaryContainer,
  color = '#C3FF95',
  size = 36,
  style,
}) => (
  <div
    style={{
      display: 'inline-block',
      background: bg,
      color,
      ...gsf(650, 115),
      fontSize: size,
      padding: `${size * 0.22}px ${size * 0.55}px ${size * 0.3}px`,
      borderRadius: size * 0.3,
      whiteSpace: 'nowrap',
      ...style,
    }}
  >
    {text}
  </div>
);
