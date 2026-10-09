import React from 'react';
import {interpolateColors} from 'remotion';
import {C, gsf} from '../theme';
import {lerp} from '../anim';
import {BudgetPill} from './Pill';
import {BackspaceIcon, BarChartIcon, CardIcon, CheckIcon, EventRepeatIcon, GearIcon, TagIcon} from './Icons';

export const SCREEN_W = 461;
export const SCREEN_H = 1000;

export const Phone: React.FC<{width: number; children: React.ReactNode; bezel?: number; style?: React.CSSProperties}> = ({width, children, bezel = 13, style}) => {
  const scale = (width - bezel * 2) / SCREEN_W;
  const screenH = SCREEN_H * scale;
  const r = 34 * scale;
  return (
    <div
      style={{
        position: 'relative',
        width,
        height: screenH + bezel * 2,
        borderRadius: r + bezel,
        background: C.onSurface,
        padding: bezel,
        boxSizing: 'border-box',
        boxShadow: '0 40px 90px rgba(26,28,21,0.28)',
        ...style,
      }}
    >
      <div style={{position: 'relative', width: width - bezel * 2, height: screenH, borderRadius: r, overflow: 'hidden', background: C.surface}}>
        <div style={{position: 'absolute', left: 0, top: 0, width: SCREEN_W, height: SCREEN_H, transform: `scale(${scale})`, transformOrigin: '0 0'}}>{children}</div>
      </div>
    </div>
  );
};

export type KeyId = 'k0' | 'k1' | 'k2' | 'k3' | 'k4' | 'k5' | 'k6' | 'k7' | 'k8' | 'k9' | 'dot' | 'back' | 'check' | 'eq' | 'div' | 'mul' | 'plus' | 'minus';

const COLS = [20, 127, 234, 341];
const KEY_W = 100;
const PAD_TOP = 553;
const IDLE_H = 105;
const CALC_H = 82.4;
const GAP = 8;

export const keyRect = (k: KeyId, calc: number) => {
  const opH = calc * CALC_H;
  const opGap = calc * GAP;
  const h = lerp(IDLE_H, CALC_H, calc);
  const rowY = (r: number) => PAD_TOP + opH + opGap + r * (h + GAP);
  const cell = (c: number, r: number, w = KEY_W, rows = 1) => ({x: COLS[c], y: rowY(r), w, h: rows * h + (rows - 1) * GAP});
  switch (k) {
    case 'div':
      return {x: COLS[0], y: PAD_TOP, w: KEY_W, h: opH};
    case 'mul':
      return {x: COLS[1], y: PAD_TOP, w: KEY_W, h: opH};
    case 'plus':
      return {x: COLS[2], y: PAD_TOP, w: KEY_W, h: opH};
    case 'minus':
      return {x: COLS[3], y: PAD_TOP, w: KEY_W, h: opH};
    case 'k7':
      return cell(0, 0);
    case 'k8':
      return cell(1, 0);
    case 'k9':
      return cell(2, 0);
    case 'back':
      return cell(3, 0);
    case 'k4':
      return cell(0, 1);
    case 'k5':
      return cell(1, 1);
    case 'k6':
      return cell(2, 1);
    case 'k1':
      return cell(0, 2);
    case 'k2':
      return cell(1, 2);
    case 'k3':
      return cell(2, 2);
    case 'dot':
      return cell(0, 3);
    case 'k0':
      return cell(1, 3, KEY_W * 2 + 7);
    case 'check': {
      const top = rowY(1);
      const bottom = lerp(rowY(3) + h, rowY(2) + h, calc);
      return {x: COLS[3], y: top, w: KEY_W, h: bottom - top};
    }
    case 'eq':
    default:
      return {x: COLS[3], y: rowY(3), w: KEY_W, h};
  }
};

export const keyCenter = (k: KeyId, calc = 0) => {
  const b = keyRect(k, calc);
  return {x: b.x + b.w / 2, y: b.y + b.h / 2};
};

const LABELS: Partial<Record<KeyId, string>> = {k0: '0', k1: '1', k2: '2', k3: '3', k4: '4', k5: '5', k6: '6', k7: '7', k8: '8', k9: '9', dot: '•', div: '÷', mul: '×', plus: '+', minus: '-', eq: '='};
const ORDER: KeyId[] = ['div', 'mul', 'plus', 'minus', 'k7', 'k8', 'k9', 'back', 'k4', 'k5', 'k6', 'check', 'k1', 'k2', 'k3', 'dot', 'k0', 'eq'];

const keyColor = (k: KeyId) =>
  k === 'back' ? C.tertiaryContainer : k === 'check' ? C.primaryContainer : k === 'dot' || k === 'eq' || k === 'div' || k === 'mul' || k === 'plus' || k === 'minus' ? C.secondaryContainer : C.button;

export const KB_ROWS = ['qwertyuiop', 'asdfghjklñ', 'zxcvbnm'];
export const KB_TOP = 572;
const KB_KEY_W = 41.6;
const KB_GAP = 4.3;

export const kbKeyRect = (ch: string) => {
  if (ch === 'enter') return {x: 461 - 3 - 66, y: KB_TOP + 26 + 3 * 66, w: 66, h: 56};
  for (let r = 0; r < KB_ROWS.length; r++) {
    const i = KB_ROWS[r].indexOf(ch);
    if (i >= 0) {
      const rowW = KB_ROWS[r].length * KB_KEY_W + (KB_ROWS[r].length - 1) * KB_GAP;
      const x0 = (461 - rowW) / 2;
      return {x: x0 + i * (KB_KEY_W + KB_GAP), y: KB_TOP + 26 + r * 66, w: KB_KEY_W, h: 56};
    }
  }
  return {x: 0, y: 0, w: 0, h: 0};
};

const Keyboard: React.FC<{show: number; active?: string; activeAmt?: number}> = ({show, active, activeAmt = 0}) => {
  if (show <= 0.001) return null;
  const keys: Array<{ch: string; label: string; r: {x: number; y: number; w: number; h: number}; tone?: string}> = [];
  KB_ROWS.forEach((row) => row.split('').forEach((ch) => keys.push({ch, label: ch, r: kbKeyRect(ch)})));
  const y3 = KB_TOP + 26 + 2 * 66;
  const y4 = KB_TOP + 26 + 3 * 66;
  keys.push({ch: 'shift', label: '⇧', r: {x: 3, y: y3, w: 56, h: 56}, tone: '#DDDED3'});
  keys.push({ch: 'del', label: '⌫', r: {x: 461 - 3 - 56, y: y3, w: 56, h: 56}, tone: '#DDDED3'});
  keys.push({ch: 'sym', label: '?123', r: {x: 3, y: y4, w: 66, h: 56}, tone: '#DDDED3'});
  keys.push({ch: 'comma', label: ',', r: {x: 73, y: y4, w: 42, h: 56}, tone: '#DDDED3'});
  keys.push({ch: 'space', label: '', r: {x: 119, y: y4, w: 223, h: 56}});
  keys.push({ch: 'period', label: '.', r: {x: 346, y: y4, w: 42, h: 56}, tone: '#DDDED3'});
  keys.push({ch: 'enter', label: '↵', r: kbKeyRect('enter'), tone: C.primaryContainer});
  return (
    <div style={{position: 'absolute', left: 0, top: KB_TOP + (1 - show) * 440, width: 461, height: 440, background: '#E9EAE0', borderTop: '1px solid #D6D8CB'}}>
      {keys.map((k) => {
        const on = k.ch === active ? activeAmt : 0;
        return (
          <div
            key={k.ch}
            style={{
              position: 'absolute',
              left: k.r.x,
              top: k.r.y - KB_TOP,
              width: k.r.w,
              height: k.r.h,
              borderRadius: 9,
              background: interpolateColors(on, [0, 1], [k.tone ?? '#FBFBF6', C.primaryContainer]),
              transform: `scale(${1 + 0.08 * on})`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: C.onSurface,
              ...gsf(450, 100),
              fontSize: k.label.length > 1 ? 15 : 22,
            }}
          >
            {k.label}
          </div>
        );
      })}
    </div>
  );
};

export type Display =
  | {kind: 'empty'}
  | {kind: 'amount'; text: string}
  | {kind: 'expr'; expr: string; result?: string}
  | {kind: 'signed'; sign: string; text: string};

export type CategoryChip = {mode: 'placeholder' | 'typing' | 'saved'; text: string; caret?: boolean};

export const EditorScreen: React.FC<{
  pillProgress: number;
  pillColorProgress?: number;
  pillAmount: string;
  pillCentered: number;
  pillPreview?: string;
  editing: number;
  calc: number;
  display: Display;
  displayScale?: number;
  displayOpacity?: number;
  displayY?: number;
  chips: number;
  category: CategoryChip;
  categoryPress?: number;
  keyboard: number;
  kbActive?: string;
  kbActiveAmt?: number;
  pressed?: Partial<Record<KeyId, number>>;
}> = ({pillProgress, pillColorProgress, pillAmount, pillCentered, pillPreview, editing, calc, display, displayScale = 1, displayOpacity = 1, displayY = 0, chips, category, categoryPress = 0, keyboard, kbActive, kbActiveAmt, pressed = {}}) => {
  const pillW = 302 - 22 * editing;
  const ink = C.onSurface;
  return (
    <div style={{position: 'absolute', inset: 0, background: C.surface}}>
      <div style={{position: 'absolute', left: 0, top: 0, width: SCREEN_W, height: 548, background: C.editorSheet, borderRadius: '0 0 44px 44px'}} />
      <div style={{position: 'absolute', left: 18, top: 10}}>
        <BudgetPill w={pillW} h={58} progress={pillProgress} colorProgress={pillColorProgress} amount={pillAmount} centered={pillCentered} preview={pillPreview} />
      </div>
      <div style={{position: 'absolute', left: 340, top: 22, opacity: 1 - editing}}>
        <BarChartIcon size={34} color={ink} />
      </div>
      <div style={{position: 'absolute', left: 396, top: 22, opacity: 1 - editing}}>
        <GearIcon size={34} color={ink} />
      </div>
      <div style={{position: 'absolute', left: 308, top: 10, display: 'flex', gap: 3, opacity: editing, transform: `scale(${0.8 + 0.2 * editing})`, transformOrigin: '100% 50%'}}>
        <div style={{width: 64, height: 58, background: C.editChip, borderRadius: '29px 6px 6px 29px', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
          <CardIcon size={28} color={ink} />
        </div>
        <div style={{width: 64, height: 58, background: C.editChip, borderRadius: '6px 29px 29px 6px', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
          <EventRepeatIcon size={28} color={ink} />
        </div>
      </div>
      <div style={{position: 'absolute', right: 22, top: 170 + displayY, opacity: displayOpacity, transform: `scale(${displayScale})`, transformOrigin: '100% 50%', color: ink, textAlign: 'right', whiteSpace: 'nowrap'}}>
        {display.kind === 'amount' ? (
          <div style={{display: 'flex', alignItems: 'baseline', justifyContent: 'flex-end'}}>
            <span style={{...gsf(600, 60), fontSize: 66, marginRight: 4}}>$</span>
            <span style={{...gsf(600, 60), fontSize: 138, lineHeight: 1}}>{display.text}</span>
          </div>
        ) : display.kind === 'signed' ? (
          <div style={{display: 'flex', alignItems: 'baseline', justifyContent: 'flex-end'}}>
            <span style={{...gsf(600, 60), fontSize: 138, lineHeight: 1}}>{display.sign}</span>
            <span style={{...gsf(600, 60), fontSize: 66, margin: '0 4px'}}>$</span>
            <span style={{...gsf(600, 60), fontSize: 138, lineHeight: 1}}>{display.text}</span>
          </div>
        ) : display.kind === 'expr' ? (
          <>
            <div style={{display: 'flex', alignItems: 'baseline', justifyContent: 'flex-end'}}>
              <span style={{...gsf(600, 60), fontSize: 56, marginRight: 4}}>$</span>
              <span style={{...gsf(600, 60), fontSize: 116, lineHeight: 1}}>{display.expr}</span>
            </div>
            <div style={{display: 'flex', alignItems: 'baseline', justifyContent: 'flex-end', marginTop: 10, color: C.onSurfaceVariant, opacity: display.result ? 1 : 0}}>
              <span style={{...gsf(600, 60), fontSize: 56, marginRight: 8}}>=</span>
              <span style={{...gsf(600, 60), fontSize: 30, marginRight: 3}}>$</span>
              <span style={{...gsf(600, 60), fontSize: 56, lineHeight: 1}}>{display.result ?? ''}</span>
            </div>
          </>
        ) : null}
      </div>
      <div style={{position: 'absolute', right: 18, top: 458, display: 'flex', gap: 10, opacity: chips, transform: `translateY(${(1 - chips) * 16}px)`}}>
        {['café', 'transporte'].map((t) => (
          <div key={t} style={{height: 50, padding: '0 16px', borderRadius: 25, background: C.surface, display: 'flex', alignItems: 'center', color: ink, ...gsf(450, 100), fontSize: 17}}>
            {t}
          </div>
        ))}
        <div
          style={{
            height: 50,
            padding: '0 18px 0 14px',
            borderRadius: 25,
            background: C.surface,
            border: category.mode === 'typing' ? `2px solid ${C.primary}` : '2px solid transparent',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: category.mode === 'placeholder' ? C.onSurfaceVariant : ink,
            ...gsf(450, 100),
            fontSize: 17,
            transform: `scale(${1 - 0.06 * categoryPress})`,
          }}
        >
          <TagIcon size={22} color={category.mode === 'placeholder' ? C.onSurfaceVariant : ink} />
          <span>{category.text}</span>
          {category.mode === 'typing' ? <span style={{width: 2, height: 22, background: C.primary, opacity: category.caret ? 1 : 0}} /> : null}
        </div>
      </div>
      <div style={{position: 'absolute', left: 207, top: 531, width: 46, height: 5, borderRadius: 3, background: C.handle}} />
      {ORDER.map((k) => {
        const b = keyRect(k, calc);
        if (b.h < 1) return null;
        const isOp = k === 'div' || k === 'mul' || k === 'plus' || k === 'minus';
        const isEq = k === 'eq';
        const p = pressed[k] ?? 0;
        const vis = isOp || isEq ? calc : 1;
        if (vis <= 0.001) return null;
        return (
          <div
            key={k}
            style={{
              position: 'absolute',
              left: b.x,
              top: b.y,
              width: b.w,
              height: b.h,
              borderRadius: Math.min(52, b.h / 2) - 22 * p,
              background: keyColor(k),
              filter: p > 0 ? `brightness(${1 - 0.1 * p})` : undefined,
              transform: `scale(${(1 - 0.06 * p) * (isEq ? 0.7 + 0.3 * calc : 1)})`,
              opacity: isEq ? calc : 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              overflow: 'hidden',
              color: isOp || isEq ? C.onSecondaryContainer : C.onButton,
              ...gsf(450, 100),
              fontSize: isOp ? 44 * Math.min(1, calc * 1.3) : 50 * Math.min(1, b.h / 70),
            }}
          >
            {k === 'back' ? <BackspaceIcon size={44} color={C.onTertiaryContainer} /> : k === 'check' ? <CheckIcon size={44} color={C.onPrimaryContainer} /> : k === 'dot' ? <span style={{fontSize: 40, marginTop: -6}}>•</span> : LABELS[k]}
          </div>
        );
      })}
      <Keyboard show={keyboard} active={kbActive} activeAmt={kbActiveAmt} />
    </div>
  );
};
