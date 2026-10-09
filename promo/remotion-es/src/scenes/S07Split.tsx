import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C, gsf} from '../theme';
import {BOUNCY, EMPH, EMPH_ACC, IN_OUT, lerp, money, scene, sp, SPATIAL, STD, tw, useSceneFrame} from '../anim';
import {Aura, RING_SPRING} from '../components/Backgrounds';
import {Headline, Tag} from '../components/Kinetic';
import {Cursor} from '../components/Cursor';

const {m, dur} = scene('split');

const CARD = {x: 60, y: 600, w: 960, h: 1150};
const PAD = 56;
const BAR_W = CARD.w - PAD * 2;
const MAX = 150;
const ROW_H = 262;

type Row = {name: string; caption: string; value: number; color: string; ink: string; at: number; pending?: number};

const ROWS: Row[] = [
  {name: 'Igual cada día', caption: 'Total ÷ días del período', value: 100, color: '#A9B948', ink: C.onSurface, at: m.row1},
  {name: 'Se recalcula cada día', caption: '(Total − Gastado) ÷ días restantes', value: 103.08, color: '#82C153', ink: C.onSurface, at: m.row2},
  {name: 'Se guarda para después', caption: 'Lo que no gastas pasa a mañana', value: 140, color: C.primary, ink: C.onPrimary, at: m.row3},
  {name: 'Tú decides cada día', caption: 'Repártelo o súmalo todo a hoy', value: 100, color: C.logoGreen, ink: C.onSurface, at: m.row4, pending: 40},
];

export const PENDING_CHIP = {
  x: CARD.x + PAD + BAR_W * (120 / MAX),
  y: CARD.y + 50 + 3 * ROW_H + 161 - 72,
};

const Chip: React.FC<{text: string; bg: string; ink: string; scale: number; style?: React.CSSProperties; dashed?: boolean}> = ({text, bg, ink, scale, style, dashed}) => (
  <div
    style={{
      position: 'absolute',
      transform: `translate(-50%, -50%) scale(${scale})`,
      background: bg,
      color: ink,
      ...gsf(700, 100),
      fontSize: 38,
      padding: '8px 22px 12px',
      borderRadius: 40,
      whiteSpace: 'nowrap',
      border: dashed ? `3px dashed ${C.onSurplus}` : undefined,
      boxShadow: '0 8px 18px rgba(26,28,21,0.14)',
      ...style,
    }}
  >
    {text}
  </div>
);

export const S07Split: React.FC = () => {
  const f = useSceneFrame();
  const cardIn = sp(f, 2, SPATIAL);
  const zoom = tw(f, m.zoom, dur, 0, 1, EMPH_ACC);
  const z = lerp(1, 5, zoom);
  const ox = PENDING_CHIP.x;
  const oy = PENDING_CHIP.y;
  const hover = tw(f, m.hover - 26, m.hover, 0, 1, IN_OUT);
  const toPending = tw(f, m.hover + 6, m.click - 4, 0, 1, IN_OUT);
  const cx = lerp(lerp(1200, CARD.x + PAD + BAR_W * (140 / MAX), hover), PENDING_CHIP.x + 20, toPending);
  const cy = lerp(lerp(1900, CARD.y + 50 + 2 * ROW_H + 160, hover), PENDING_CHIP.y + 12, toPending);
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <div style={{position: 'absolute', inset: 0, transform: `scale(${z})`, transformOrigin: `${ox}px ${oy}px`}}>
        <Aura f={f + 760} ring={RING_SPRING} radius={640} cy={1200} />
        <div style={{position: 'absolute', left: 96, top: 120, transform: `scale(${sp(f, 0, SPATIAL)})`, transformOrigin: '0 50%'}}>
          <Tag text="Día 2" size={40} />
        </div>
        <div style={{position: 'absolute', left: 96, top: 214}}>
          <Headline f={f} lines={['Cuatro maneras', 'de repartirlo.']} start={0} stagger={4} size={100} />
        </div>
        <div style={{position: 'absolute', left: 98, top: 458, ...gsf(460, 100), fontSize: 44, color: C.onSurfaceVariant, opacity: tw(f, 12, 26, 0, 1, EMPH)}}>
          Día 1: $60 de $100 gastados.
        </div>
        <div
          style={{
            position: 'absolute',
            left: CARD.x,
            top: CARD.y,
            width: CARD.w,
            height: CARD.h,
            borderRadius: 56,
            background: 'rgba(250,250,238,0.88)',
            boxShadow: '0 30px 80px rgba(58,77,16,0.12)',
            transform: `translateY(${(1 - cardIn) * 300}px)`,
            opacity: Math.min(1, cardIn * 1.4),
          }}
        >
          {[0.25, 0.5, 0.75, 1].map((g) => (
            <div key={g} style={{position: 'absolute', left: PAD + BAR_W * g * (MAX / MAX) - 1, top: 40, bottom: 40, borderLeft: `2px dashed ${C.outlineVariant}`, opacity: 0.6}} />
          ))}
          {ROWS.map((r, i) => {
            const y = 50 + i * ROW_H;
            const k = tw(f, r.at, r.at + 14, 0, 1, EMPH);
            const grow = sp(f, r.at + 6, {stiffness: 120, damping: 17, mass: 1});
            const val = r.value * Math.min(1, grow);
            const shown = grow < 0.995 ? money(Math.round(val)) : money(r.value);
            const end = PAD + BAR_W * (val / MAX);
            const chipScale = sp(f, r.at + 8, BOUNCY);
            const pendK = r.pending ? sp(f, m.pending, {stiffness: 160, damping: 20, mass: 1}) : 0;
            const pendEnd = PAD + BAR_W * ((val + (r.pending ?? 0) * pendK) / MAX);
            return (
              <div key={r.name} style={{position: 'absolute', left: 0, top: y, width: CARD.w, height: ROW_H}}>
                <div style={{position: 'absolute', left: PAD, top: 0, opacity: k, transform: `translateX(${(1 - k) * -40}px)`, filter: k < 1 ? `blur(${(1 - k) * 8}px)` : undefined}}>
                  <div style={{...gsf(680, 110), fontSize: 50, color: C.onSurface}}>{r.name}</div>
                  <div style={{...gsf(450, 100), fontSize: 34, color: C.onSurfaceVariant, marginTop: 6}}>{r.caption}</div>
                </div>
                <div style={{position: 'absolute', left: PAD, top: 150, width: BAR_W, height: 22, borderRadius: 11, background: C.surfaceHighest, opacity: k}} />
                {r.pending ? (
                  <div
                    style={{
                      position: 'absolute',
                      left: end - 11,
                      top: 150,
                      width: Math.max(0, pendEnd - end + 11),
                      height: 22,
                      borderRadius: '0 11px 11px 0',
                      opacity: Math.min(1, pendK * 2),
                      background: `repeating-linear-gradient(-45deg, #FFB74D 0 10px, ${C.surplusTrack} 10px 20px)`,
                    }}
                  />
                ) : null}
                <div style={{position: 'absolute', left: PAD, top: 150, width: Math.max(0, end - PAD), height: 22, borderRadius: 11, background: r.color}} />
                <Chip text={shown} bg={r.color} ink={r.ink} scale={chipScale} style={{left: end, top: 161}} />
                {r.pending ? <Chip text="+$40 pendiente" bg={C.surplusTrack} ink={C.onSurplus} scale={pendK} dashed style={{left: PAD + BAR_W * ((r.value + (r.pending ?? 0) / 2) / MAX), top: 161 - 72}} /> : null}
              </div>
            );
          })}
        </div>
      </div>
      <Cursor x={cx} y={cy} f={f} presses={[m.click]} opacity={1 - tw(f, m.zoom, m.zoom + 8, 0, 1, STD)} />
      <AbsoluteFill style={{background: C.surplusTrack, opacity: tw(f, dur - 12, dur, 0, 1, EMPH)}} />
    </AbsoluteFill>
  );
};
