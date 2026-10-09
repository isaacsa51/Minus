import React from 'react';
import {AbsoluteFill, interpolateColors} from 'remotion';
import {C, gsf} from '../theme';
import {EMPH, EMPH_ACC, IN_OUT, lerp, money, scene, sp, SPATIAL, STD, tw, useSceneFrame} from '../anim';
import {Aura, RING_WARM} from '../components/Backgrounds';
import {BudgetPill} from '../components/Pill';
import {Cursor} from '../components/Cursor';
import {Headline} from '../components/Kinetic';
import {InfoOutlineIcon, PencilIcon} from '../components/Icons';

const {m} = scene('sheet');

const PILL = {x: 70, y: 560 - 90.5, w: 940, h: 181};
const SC = 2.1;
const HANDLE_AREA = 40;
const CONTENT_H = 657;
const SHEET_H = (HANDLE_AREA + CONTENT_H) * SC;
const SHEET_TOP = 1920 - SHEET_H;
const SHEET_MARGIN = 28;
const CONTENT_LEFT = (1080 - 461 * SC) / 2;

const SPENT = {container: '#D3F0A8', main: '#A6C77E', ink: '#13200A'};
const SPENT_PCT = 60 / 1400;
const DAYS_PROGRESS = 1 - 13 / 14 - 0.01;
const TOGGLE_W = (423 - 4) / 3;
const TOGGLE_Y = 388;

const toCanvas = (ux: number, uy: number) => ({x: CONTENT_LEFT + ux * SC, y: SHEET_TOP + (HANDLE_AREA + uy) * SC});

const Reveal: React.FC<{f: number; at: number; children: React.ReactNode}> = ({f, at, children}) => {
  const k = tw(f, at, at + 12, 0, 1, EMPH);
  return <div style={{position: 'absolute', inset: 0, opacity: k, transform: `translateY(${(1 - k) * 22}px)`}}>{children}</div>;
};

const wavyEdge = (edge: number, h: number, amp: number, period: number) => {
  const pts: string[] = [`M0,0`, `L${edge},0`];
  for (let y = 0; y <= h; y += 2) pts.push(`L${(edge + amp * Math.sin((2 * Math.PI * y) / period)).toFixed(2)},${y}`);
  pts.push(`L0,${h}`, 'Z');
  return pts.join(' ');
};

const wavyArc = (cx: number, cy: number, r: number, progress: number, amp: number, wavelength: number) => {
  if (progress <= 0.001) return '';
  const waves = (2 * Math.PI * r) / wavelength;
  const end = progress * 2 * Math.PI;
  const pts: string[] = [];
  for (let a = 0; a <= end; a += 0.02) {
    const rr = r + amp * Math.sin(waves * a);
    pts.push(`${pts.length ? 'L' : 'M'}${(cx + rr * Math.sin(a)).toFixed(2)},${(cy - rr * Math.cos(a)).toFixed(2)}`);
  }
  return pts.join(' ');
};

const Toggle: React.FC<{label: string; index: number; sel: number}> = ({label, index, sel}) => {
  const outer = 23;
  const inner = lerp(8, 23, sel);
  const left = index === 0 ? outer : inner;
  const right = index === 2 ? outer : inner;
  return (
    <div
      style={{
        position: 'absolute',
        left: 19 + index * (TOGGLE_W + 2),
        top: TOGGLE_Y,
        width: TOGGLE_W,
        height: 46,
        borderRadius: `${left}px ${right}px ${right}px ${left}px`,
        background: interpolateColors(sel, [0, 1], [C.surfaceCont, C.primaryContainer]),
        color: interpolateColors(sel, [0, 1], [C.onSurfaceVariant, C.onPrimaryContainer]),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        ...gsf(lerp(400, 700, sel), lerp(65, 125, sel)),
        fontSize: 15.7,
      }}
    >
      {label}
    </div>
  );
};

const SheetContent: React.FC<{f: number}> = ({f}) => {
  const spentFill = tw(f, m.spent + 4, m.spent + 30, 0, SPENT_PCT, STD);
  const ring = tw(f, m.totals + 4, m.totals + 34, 0, DAYS_PROGRESS, STD);
  const weekly = sp(f, m.weekly + 2, {stiffness: 500, damping: 40, mass: 1});
  const amount = f < m.weekly ? Math.round(tw(f, m.calc + 2, m.calc + 18, 0, 100, STD)) : Math.round(tw(f, m.weekly + 2, m.weekly + 18, 100, 700, STD));
  return (
    <div style={{position: 'absolute', left: 0, top: 0, width: 461, height: CONTENT_H, color: C.onSurface}}>
      <Reveal f={f} at={m.open + 6}>
        <div style={{position: 'absolute', left: 20, top: 22, ...gsf(700, 135), fontSize: 27}}>Total budget</div>
        <div style={{position: 'absolute', left: 401, top: 25}}>
          <PencilIcon size={27} color={C.onSurface} />
        </div>
      </Reveal>
      <Reveal f={f} at={m.spent}>
        <div style={{position: 'absolute', left: 19, top: 78, width: 423, height: 120, borderRadius: 31, overflow: 'hidden', background: SPENT.container, color: SPENT.ink}}>
          <svg width={423} height={120} style={{position: 'absolute', left: 0, top: 0}}>
            <path d={wavyEdge(spentFill * 423, 120, 6.7, 47)} fill={SPENT.main} />
          </svg>
          <div style={{position: 'absolute', left: 27, top: 18}}>
            <div style={{...gsf(700, 125), fontSize: 37, lineHeight: 1.05}}>$60</div>
            <div style={{...gsf(400, 65), fontSize: 16, opacity: 0.7, marginTop: 2}}>Spent</div>
            <div style={{...gsf(300, 85), fontSize: 16.5, marginTop: 8}}>Available: 96%</div>
          </div>
        </div>
      </Reveal>
      <Reveal f={f} at={m.totals}>
        <div style={{position: 'absolute', left: 19, top: 207, width: 258, height: 134, borderRadius: 31, background: C.onSurface, color: C.surfaceVariant, padding: '20px 22px', boxSizing: 'border-box'}}>
          <div style={{...gsf(700, 125), fontSize: 28, lineHeight: 1.1}}>$1,400</div>
          <div style={{...gsf(400, 85), fontSize: 15, opacity: 0.75, marginTop: 2}}>Total budget</div>
          <div style={{display: 'flex', alignItems: 'center', marginTop: 12, ...gsf(400, 80), fontSize: 17}}>
            <span>01 Jul</span>
            <svg height={14} style={{flex: 1, margin: '0 9px'}} viewBox="0 0 100 14" preserveAspectRatio="none">
              <path d="M2 7H97M90 2l7 5-7 5" stroke={C.surfaceVariant} strokeWidth={1.8} fill="none" strokeLinecap="round" vectorEffect="non-scaling-stroke" />
            </svg>
            <span>14 Jul</span>
          </div>
        </div>
        <svg width={156} height={134} style={{position: 'absolute', left: 286, top: 207, overflow: 'visible'}}>
          <circle cx={78} cy={67} r={60} stroke={C.surfaceVariant} strokeWidth={6.7} fill="none" />
          <path d={wavyArc(78, 67, 60, ring, 2.2, 51)} stroke={C.primary} strokeWidth={6.7} fill="none" strokeLinecap="round" />
        </svg>
        <div style={{position: 'absolute', left: 286, top: 207, width: 156, height: 134, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center'}}>
          <div style={{...gsf(700, 151), fontSize: 25, lineHeight: 1}}>13</div>
          <div style={{...gsf(400, 65), fontSize: 16, lineHeight: '14px', opacity: 0.6, textAlign: 'center', marginTop: 4}}>
            Days
            <br />
            remaining
          </div>
        </div>
      </Reveal>
      <Reveal f={f} at={m.split}>
        <div style={{position: 'absolute', left: 19, top: 352, ...gsf(600, 85), fontSize: 20}}>How do you want to split the budget?</div>
        <Toggle label="Daily" index={0} sel={1 - weekly} />
        <Toggle label="Weekly" index={1} sel={weekly} />
        <Toggle label="Biweekly" index={2} sel={0} />
      </Reveal>
      <Reveal f={f} at={m.calc}>
        <div style={{position: 'absolute', left: 19, top: 440, width: 423, height: 100, borderRadius: 18, background: '#ECEDE0', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center'}}>
          <div style={{...gsf(400, 65), fontSize: 16, color: C.onSurfaceVariant}}>Calculated amount</div>
          <div style={{...gsf(700, 135), fontSize: 31, lineHeight: 1.2, transform: `scale(${1 + 0.06 * Math.sin(Math.min(1, Math.max(0, (f - m.weekly - 2) / 16)) * Math.PI)})`}}>{money(amount)}</div>
          <div style={{display: 'flex', alignItems: 'center', gap: 4, marginTop: 4, color: 'rgba(118,120,107,0.85)', ...gsf(500, 100), fontSize: 12.5}}>
            <InfoOutlineIcon size={15.7} color="rgba(118,120,107,0.85)" />
            Tap for the full breakdown
          </div>
        </div>
      </Reveal>
      <Reveal f={f} at={m.calc + 6}>
        <div style={{position: 'absolute', left: 19, top: 558, width: 423, height: 1.1, background: C.outlineVariant}} />
        <div style={{position: 'absolute', left: 19, top: 576, width: 423, height: 45, boxSizing: 'border-box', borderRadius: 23, border: `1.1px solid ${C.outlineVariant}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#BA1A1A', ...gsf(700, 125), fontSize: 15.7}}>
          Finalize budget period early
        </div>
      </Reveal>
    </div>
  );
};

export const S06Sheet: React.FC = () => {
  const f = useSceneFrame();
  const settle = tw(f, 0, 16, 0, 1, EMPH);
  const tapPress = f >= m.tap - 3 && f < m.tap + 10 ? Math.sin(Math.min(1, (f - m.tap + 3) / 13) * Math.PI) : 0;
  const open = sp(f, m.open, SPATIAL);
  const exit = tw(f, m.exit, m.exit + 20, 0, 1, EMPH_ACC);
  const sheetY = lerp(lerp(1920, SHEET_TOP, open), 1980, exit);
  const scrim = tw(f, m.open, m.open + 12, 0, 0.16, STD) * (1 - exit);
  const weeklyPt = toCanvas(19 + TOGGLE_W + 2 + TOGGLE_W / 2, TOGGLE_Y + 23);
  const toPill = tw(f, 0, m.tap - 2, 0, 1, IN_OUT);
  const away = tw(f, m.tap + 4, m.open + 16, 0, 1, IN_OUT);
  const toWeekly = tw(f, m.weekly - 34, m.weekly - 4, 0, 1, IN_OUT);
  const cx = lerp(lerp(lerp(1160, 860, toPill), 1180, away), weeklyPt.x, toWeekly);
  const cy = lerp(lerp(lerp(900, 590, toPill), 760, away), weeklyPt.y, toWeekly);
  const cursorOpacity = tw(f, 0, 6, 0, 1) * (1 - tw(f, m.tap + 6, m.tap + 14, 0, 1)) + tw(f, m.weekly - 34, m.weekly - 26, 0, 1);
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <Aura f={f + 600} ring={RING_WARM} cy={980} radius={520} />
      <AbsoluteFill style={{background: C.editorSheet, opacity: 1 - settle}} />
      <div style={{position: 'absolute', left: PILL.x, top: PILL.y, transform: `scale(${1 - 0.04 * tapPress})`, borderRadius: PILL.h / 2, boxShadow: `0 ${30 * settle}px ${70 * settle}px rgba(81,101,38,${0.18 * settle})`}}>
        <BudgetPill w={PILL.w} h={PILL.h} progress={0.6} amount="$40" />
      </div>
      <AbsoluteFill style={{background: '#000000', opacity: scrim}} />
      <div style={{position: 'absolute', left: 96, top: 132}}>
        <Headline f={f} lines={['Tap it to see', 'the whole period.']} start={4} stagger={4} size={100} />
      </div>
      <div style={{position: 'absolute', left: SHEET_MARGIN, top: sheetY, width: 1080 - SHEET_MARGIN * 2, height: SHEET_H + 200, background: C.surfaceLow, borderRadius: `${28 * SC}px ${28 * SC}px 0 0`, boxShadow: '0 -20px 60px rgba(26,28,21,0.18)'}}>
        <div style={{position: 'absolute', left: 540 - SHEET_MARGIN - 16 * SC, top: 20 * SC, width: 32 * SC, height: 4 * SC, borderRadius: 2 * SC, background: C.onSurfaceVariant, opacity: 0.4}} />
        <div style={{position: 'absolute', left: CONTENT_LEFT - SHEET_MARGIN, top: HANDLE_AREA * SC, transform: `scale(${SC})`, transformOrigin: '0 0'}}>
          <SheetContent f={f} />
        </div>
      </div>
      <Cursor x={cx} y={cy} f={f} presses={[m.tap, m.weekly]} opacity={Math.min(1, cursorOpacity) * (1 - tw(f, m.exit - 8, m.exit + 2, 0, 1))} />
    </AbsoluteFill>
  );
};
