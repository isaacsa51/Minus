import React from 'react';
import {AbsoluteFill, interpolateColors} from 'remotion';
import {C, gsf} from '../theme';
import {BOUNCY, EMPH, money, scene, sp, SPATIAL, tw, useSceneFrame} from '../anim';
import {Aura, RING_COOL} from '../components/Backgrounds';
import {Headline} from '../components/Kinetic';
import {CalendarIcon, TagIcon} from '../components/Icons';

const {m} = scene('analytics');

export const DAILY = [60, 42.5, 88, 15.75, 152.4, 35, 0, 72.3, 18.9, 95, 45.2, 0, 64, 30];
export const COUNTS = [1, 2, 3, 1, 3, 2, 0, 2, 2, 3, 2, 0, 2, 1];
export const BUDGET = 1400;
export const TOTAL = DAILY.reduce((a, b) => a + b, 0);
const ALLOWANCE = BUDGET / DAILY.length;
const MAX_COUNT = Math.max(...COUNTS);
const intensity = (i: number) => Math.min(1.4, (DAILY[i] / ALLOWANCE) * 0.6 + (COUNTS[i] / MAX_COUNT) * 0.4);
const HIGHEST = DAILY.indexOf(Math.max(...DAILY));

const CARD_BG = '#F3F3E6';

const TrendCard: React.FC<{f: number}> = ({f}) => {
  const draw = tw(f, m.line, m.line + 54, 0, 1, EMPH);
  const total = TOTAL * draw;
  const w = 864;
  const h = 300;
  let acc = 0;
  const pts = DAILY.map((d, i) => {
    acc += d;
    return [(i / (DAILY.length - 1)) * w, h - (acc / TOTAL) * (h - 10)] as const;
  });
  const line = pts.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ');
  const area = `${line} L${w},${h} L0,${h} Z`;
  const len = 1500;
  return (
    <div style={{position: 'absolute', inset: 0, padding: '40px 48px', boxSizing: 'border-box'}}>
      <div style={{...gsf(450, 100), fontSize: 34, color: C.onSurfaceVariant}}>Total gastado</div>
      <div style={{...gsf(780, 135), fontSize: 86, color: C.onSurface, lineHeight: 1.1}}>{money(Math.round(total * 100) / 100, 2)}</div>
      <svg width={w} height={h + 50} style={{position: 'absolute', left: 48, top: 200, overflow: 'visible'}}>
        <defs>
          <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={C.tertiary} stopOpacity={0.38} />
            <stop offset="100%" stopColor={C.tertiary} stopOpacity={0.02} />
          </linearGradient>
          <clipPath id="trendClip">
            <rect x={0} y={-20} width={w * draw} height={h + 40} />
          </clipPath>
        </defs>
        {[0, 0.25, 0.5, 0.75, 1].map((g) => (
          <line key={g} x1={0} x2={w} y1={h - g * (h - 10)} y2={h - g * (h - 10)} stroke={C.outlineVariant} strokeDasharray="8 8" strokeWidth={2} opacity={0.7} />
        ))}
        <path d={area} fill="url(#trendFill)" clipPath="url(#trendClip)" />
        <path d={line} fill="none" stroke={C.tertiary} strokeWidth={7} strokeLinejoin="round" strokeLinecap="round" strokeDasharray={len} strokeDashoffset={len * (1 - draw)} />
        {['1 jul', '7 jul', '14 jul'].map((l, i) => (
          <text key={l} x={[0, w * (6 / 13), w][i]} y={h + 44} textAnchor={['start', 'middle', 'end'][i] as 'start'} style={{...gsf(450, 100), fontSize: 28}} fill={C.onSurfaceVariant}>
            {l}
          </text>
        ))}
      </svg>
    </div>
  );
};

const MinMaxCard: React.FC<{f: number; isMin: boolean}> = ({f, isMin}) => {
  const grow = tw(f, m.card2 + 6, m.card2 + 40, 0, 1, EMPH);
  const bg = isMin ? C.minCard : C.maxCard;
  const ink = isMin ? C.onMinCard : C.onMaxCard;
  const w = 470;
  const h = 300;
  const curve = isMin
    ? `M0,${h - 70} C110,${h - 70} 150,${h - 150 * grow} 250,${h - 150 * grow} C350,${h - 150 * grow} 380,${h - 40} ${w},${h - 40} L${w},${h} L0,${h} Z`
    : `M0,${h - 60} C100,${h - 60} 150,${h - 100 * grow} 220,${h - 100 * grow} C290,${h - 100 * grow} 340,${h - 230 * grow} 400,${h - 230 * grow} C440,${h - 230 * grow} 450,${h - 110} ${w},${h - 110} L${w},${h} L0,${h} Z`;
  const dot = isMin ? {x: 420, y: h - 40} : {x: 400, y: h - 230 * grow};
  return (
    <div style={{position: 'absolute', inset: 0, borderRadius: 48, background: bg, color: ink, overflow: 'hidden'}}>
      <svg width={w} height={h} style={{position: 'absolute', left: 0, top: 0}}>
        <path d={curve} fill={isMin ? 'rgba(24,94,214,0.13)' : 'rgba(165,70,40,0.13)'} />
        <circle cx={dot.x} cy={dot.y} r={20 * grow} fill={isMin ? 'rgba(24,94,214,0.22)' : 'rgba(237,104,39,0.28)'} />
        <circle cx={dot.x} cy={dot.y} r={9 * grow} fill={isMin ? '#185ED6' : C.maxDot} />
      </svg>
      <div style={{position: 'absolute', left: 36, top: 34}}>
        <div style={{...gsf(780, 135), fontSize: 54, lineHeight: 1.05}}>{isMin ? '$4.50' : '$120.40'}</div>
        <div style={{...gsf(500, 85), fontSize: 34, opacity: 0.72}}>{isMin ? 'Gasto mínimo' : 'Gasto máximo'}</div>
        <div style={{display: 'flex', alignItems: 'center', gap: 10, marginTop: 14, ...gsf(450, 100), fontSize: 30}}>
          <CalendarIcon size={30} color={ink} /> {isMin ? '9 jul' : '5 jul'}
        </div>
        <div style={{display: 'flex', alignItems: 'center', gap: 10, marginTop: 4, ...gsf(450, 100), fontSize: 30}}>
          <TagIcon size={30} color={ink} /> {isMin ? 'Café' : 'Alimentos'}
        </div>
      </div>
    </div>
  );
};

const Heatmap: React.FC<{f: number}> = ({f}) => {
  const cw = 118;
  const ch = 88;
  const gx = 6;
  const left = 30;
  const top = 150;
  const days = Array.from({length: 18}).map((_, i) => i + 1);
  const peak = f >= m.peak ? Math.sin(Math.min(1, (f - m.peak) / 16) * Math.PI) * 0.12 : 0;
  return (
    <div style={{position: 'absolute', inset: 0, padding: '34px 40px', boxSizing: 'border-box'}}>
      <div style={{...gsf(700, 135), fontSize: 42, color: C.onSurface, marginLeft: 12}}>Julio</div>
      {['D', 'L', 'M', 'M', 'J', 'V', 'S'].map((d, i) => (
        <div key={i} style={{position: 'absolute', left: left + i * (cw + gx), top: 104, width: cw, textAlign: 'center', ...gsf(700, 125), fontSize: 26, color: C.onSurfaceVariant}}>
          {d}
        </div>
      ))}
      {days.map((day) => {
        const i = day - 1;
        const col = (3 + i) % 7;
        const row = Math.floor((3 + i) / 7);
        const inPeriod = day <= 14;
        const has = inPeriod && DAILY[i] > 0;
        const pop = has ? sp(f, m.cells + i * 2, BOUNCY) : 1;
        const heat = has ? intensity(i) : 0;
        const isHigh = i === HIGHEST;
        const bg = heat > 1 ? C.errorContainer : interpolateColors(Math.min(1, heat), [0, 1], [C.surfaceHighest, C.primaryContainer]);
        const ink = !inPeriod ? 'rgba(26,28,21,0.3)' : heat > 1 ? C.onErrorContainer : interpolateColors(Math.min(1, heat), [0, 1], [C.onSurface, C.onPrimaryContainer]);
        const scale = isHigh ? (1 + 0.15 * pop + peak) : 1;
        return (
          <div key={day} style={{position: 'absolute', left: left + col * (cw + gx), top: top + row * (ch + gx), width: cw, height: ch, zIndex: isHigh ? 2 : 1}}>
            {has ? <div style={{position: 'absolute', inset: 3, borderRadius: 8, background: bg, transform: `scale(${pop * scale})`}} /> : null}
            <div style={{position: 'absolute', inset: 3, borderRadius: 8, border: `2px solid ${C.outlineVariant}`, opacity: 0.65}} />
            <div style={{position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', color: ink, ...(isHigh ? gsf(900, 135) : gsf(420, 100, {slnt: inPeriod ? 0 : -10})), fontSize: isHigh ? 40 : 32, transform: `scale(${isHigh ? scale : 1})`}}>
              {day}
            </div>
          </div>
        );
      })}
      <div style={{position: 'absolute', right: 44, bottom: 30, display: 'flex', alignItems: 'center', gap: 8, ...gsf(450, 85), fontSize: 26, color: 'rgba(69,72,60,0.6)'}}>
        Menos
        {[0, 0.25, 0.5, 0.75, 1].map((r) => (
          <div key={r} style={{width: 22, height: 22, borderRadius: 5, border: '1px solid rgba(198,200,185,0.5)', background: r === 0 ? 'transparent' : interpolateColors(r, [0, 1], [C.surfaceHighest, C.primaryContainer])}} />
        ))}
        Más
      </div>
    </div>
  );
};

export const S10Analytics: React.FC = () => {
  const f = useSceneFrame();
  const cards = [
    {at: m.card1, x: 60, y: 420, w: 960, h: 560, rot: -4},
    {at: m.card2, x: 60, y: 1004, w: 470, h: 300, rot: 5},
    {at: m.card2 + 4, x: 550, y: 1004, w: 470, h: 300, rot: -5},
    {at: m.card3, x: 60, y: 1328, w: 960, h: 520, rot: 3},
  ];
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <Aura f={f + 1224} ring={RING_COOL} radius={700} cy={1150} />
      <div style={{position: 'absolute', inset: 0}}>
        <div style={{position: 'absolute', left: 96, top: 120}}>
          <Headline f={f} lines={['Mira a dónde', 'se fue todo.']} start={4} stagger={4} size={100} />
        </div>
        {cards.map((c, i) => {
          const k = sp(f, c.at, SPATIAL);
          return (
            <div
              key={i}
              style={{
                position: 'absolute',
                left: c.x,
                top: c.y,
                width: c.w,
                height: c.h,
                borderRadius: 48,
                background: i === 0 || i === 3 ? CARD_BG : 'transparent',
                boxShadow: '0 24px 60px rgba(58,77,16,0.12)',
                transform: `translateY(${(1 - k) * 900}px) rotate(${(1 - k) * c.rot}deg)`,
              }}
            >
              {i === 0 ? <TrendCard f={f} /> : i === 1 ? <MinMaxCard f={f} isMin /> : i === 2 ? <MinMaxCard f={f} isMin={false} /> : <Heatmap f={f} />}
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};
