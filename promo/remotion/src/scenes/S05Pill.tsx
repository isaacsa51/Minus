import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C} from '../theme';
import {EMPH, EMPH_ACC, IN_OUT, lerp, money, scene, sp, SPATIAL, STD, tw, useSceneFrame} from '../anim';
import {Headline} from '../components/Kinetic';
import {Cursor} from '../components/Cursor';
import {CategoryChip, Display, EditorScreen, KeyId, keyCenter, Phone, SCREEN_W} from '../components/Phone';

const {m, dur} = scene('pill');

export const PHONE_W = 660;
export const PHONE_LEFT = 540 - PHONE_W / 2;
export const PHONE_TOP = 430;
const BEZEL = 13;
export const PHONE_SCALE = (PHONE_W - BEZEL * 2) / SCREEN_W;
export const PILL_CENTER = {x: PHONE_LEFT + BEZEL + (18 + 151) * PHONE_SCALE, y: PHONE_TOP + BEZEL + (10 + 29) * PHONE_SCALE};
export const PILL_ZOOM = 940 / (302 * PHONE_SCALE);
export const PILL_TARGET = {x: 540, y: 560};

const toCanvas = (p: {x: number; y: number}) => ({x: PHONE_LEFT + BEZEL + p.x * PHONE_SCALE, y: PHONE_TOP + BEZEL + p.y * PHONE_SCALE});
const key = (k: KeyId, calc: number) => toCanvas(keyCenter(k, calc));
const CATEGORY_CHIP = toCanvas({x: 377, y: 483});
const WORD = 'Groceries';

const tap = (f: number, at: number) => {
  const d = f - at;
  if (d < -3 || d > 12) return 0;
  return d < 0 ? (d + 3) / 3 : 1 - d / 12;
};
const hold = (f: number, at: number, until: number) => (f >= at - 3 && f < until ? Math.min(1, (f - at + 3) / 3) : f >= until && f < until + 8 ? 1 - (f - until) / 8 : 0);

const at = <T,>(f: number, steps: Array<[number, T]>, initial: T): T => {
  let v = initial;
  for (const [t, value] of steps) if (f >= t) v = value;
  return v;
};

const ramp = (f: number, steps: Array<[number, number]>, initial: number, len = 24) => {
  let v = initial;
  let prev = initial;
  for (const [t, value] of steps) {
    if (f >= t) {
      v = lerp(prev, value, tw(f, t, t + len, 0, 1, STD));
      prev = value;
    }
  }
  return v;
};

export const S05Pill: React.FC = () => {
  const f = useSceneFrame();
  const enter = sp(f, m.phoneIn, SPATIAL);

  const calcUp = (start: number, on: number) => tw(f, start, on, 0, 1, IN_OUT);
  const calcDown = (t: number) => 1 - tw(f, t, t + 14, 0, 1, EMPH);
  const calc =
    f < m.check + 2
      ? calcUp(m.swipe, m.calcOn)
      : f < m.swipe2
        ? calcDown(m.check + 2)
        : f < m.cleared1
          ? calcUp(m.swipe2, m.calcOn2)
          : f < m.swipe3
            ? calcDown(m.cleared1)
            : f < m.cleared2
              ? calcUp(m.swipe3, m.calcOn3)
              : calcDown(m.cleared2);

  const display: Display = at<Display>(
    f,
    [
      [m.press4, {kind: 'amount', text: '4'}],
      [m.press5, {kind: 'amount', text: '45'}],
      [m.opPlus, {kind: 'expr', expr: '45+'}],
      [m.one, {kind: 'expr', expr: '45+1', result: '46'}],
      [m.five, {kind: 'expr', expr: '45+15', result: '60'}],
      [m.check + 16, {kind: 'empty'}],
      [m.adjPlus, {kind: 'signed', sign: '+', text: '0'}],
      [m.two, {kind: 'signed', sign: '+', text: '2'}],
      [m.zeroA, {kind: 'signed', sign: '+', text: '20'}],
      [m.zeroB, {kind: 'signed', sign: '+', text: '200'}],
      [m.cleared1, {kind: 'empty'}],
      [m.adjMinus, {kind: 'signed', sign: '-', text: '0'}],
      [m.five2, {kind: 'signed', sign: '-', text: '5'}],
      [m.zeroC, {kind: 'signed', sign: '-', text: '50'}],
      [m.cleared2, {kind: 'empty'}],
    ],
    {kind: 'empty'},
  );
  const lastInput = at(f, [[m.press4, m.press4], [m.press5, m.press5], [m.opPlus, m.opPlus], [m.one, m.one], [m.five, m.five], [m.adjPlus, m.adjPlus], [m.two, m.two], [m.zeroA, m.zeroA], [m.zeroB, m.zeroB], [m.adjMinus, m.adjMinus], [m.five2, m.five2], [m.zeroC, m.zeroC]], 0);
  const pop = sp(f, lastInput, SPATIAL);
  const fly = f >= m.check && f < m.check + 16 ? tw(f, m.check + 2, m.check + 16, 0, 1, EMPH_ACC) : 0;

  const editing = Math.max(
    tw(f, m.press4, m.press4 + 10, 0, 1, EMPH) * (1 - tw(f, m.check + 4, m.check + 16, 0, 1, EMPH)),
    f >= m.adjPlus && f < m.cleared1 + 12 ? tw(f, m.adjPlus, m.adjPlus + 10, 0, 1, EMPH) * (1 - tw(f, m.cleared1, m.cleared1 + 12, 0, 1, EMPH)) : 0,
    f >= m.adjMinus ? tw(f, m.adjMinus, m.adjMinus + 10, 0, 1, EMPH) * (1 - tw(f, m.cleared2, m.cleared2 + 12, 0, 1, EMPH)) : 0,
  );

  const remaining = ramp(f, [[m.press4, 96], [m.press5, 55], [m.opPlus, 100], [m.one, 54], [m.five, 40]], 100, 14);
  const progress = ramp(
    f,
    [[m.press4, 0.04], [m.press5, 0.45], [m.opPlus, 0], [m.one, 0.46], [m.five, 0.6], [m.two, 0.58], [m.zeroA, 0.4], [m.zeroB, 0], [m.cleared1, 0.6], [m.five2, 0.65], [m.zeroC, 1], [m.cleared2, 0.6]],
    0,
  );
  const preview = at<string | undefined>(
    f,
    [
      [m.two, '$2 will be added'],
      [m.zeroA, '$20 will be added'],
      [m.zeroB, '$200 will be added'],
      [m.cleared1, undefined],
      [m.five2, '$5 will be subtracted'],
      [m.zeroC, '$50 will be subtracted'],
      [m.cleared2, undefined],
    ],
    undefined,
  );

  const typed = f < m.type ? 0 : Math.min(WORD.length, Math.floor((f - m.type) / 4) + 1);
  const category: CategoryChip =
    f < m.chip + 2
      ? {mode: 'placeholder', text: 'Category'}
      : f < m.enter
        ? {mode: 'typing', text: WORD.slice(0, typed), caret: Math.floor(f / 14) % 2 === 0 || typed < WORD.length}
        : {mode: 'saved', text: WORD};
  const chips = f < m.check + 4 ? tw(f, m.press4 + 4, m.press4 + 16, 0, 1, EMPH) : 1 - tw(f, m.check + 4, m.check + 14, 0, 1, EMPH);
  const keyboard = tw(f, m.kbUp, m.kbUp + 14, 0, 1, EMPH) * (1 - tw(f, m.enter + 2, m.enter + 16, 0, 1, EMPH_ACC));
  const letterIndex = f >= m.type && f < m.type + WORD.length * 4 ? Math.floor((f - m.type) / 4) : -1;
  const kbActive = f >= m.enter - 2 && f < m.enter + 8 ? 'enter' : letterIndex >= 0 ? WORD[letterIndex].toLowerCase() : undefined;
  const kbActiveAmt = kbActive === 'enter' ? tap(f, m.enter) : letterIndex >= 0 ? 1 - ((f - m.type) % 4) / 4 : 0;

  const pressed: Partial<Record<KeyId, number>> = {
    k4: tap(f, m.press4),
    k5: Math.max(tap(f, m.press5), tap(f, m.five), tap(f, m.five2)),
    plus: Math.max(tap(f, m.opPlus), tap(f, m.adjPlus)),
    minus: tap(f, m.adjMinus),
    k1: tap(f, m.one),
    k2: tap(f, m.two),
    k0: Math.max(tap(f, m.zeroA), tap(f, m.zeroB), tap(f, m.zeroC)),
    check: tap(f, m.check),
    back: Math.max(hold(f, m.clear1, m.cleared1), hold(f, m.clear2, m.cleared2)),
  };

  const k5Idle = key('k5', 0);
  const segs: Array<[number, number, {x: number; y: number}, {x: number; y: number}]> = [
    [m.phoneIn + 16, m.press4 - 6, {x: 1160, y: 1900}, key('k4', 0)],
    [m.press4 + 4, m.press5 - 4, key('k4', 0), key('k5', 0)],
    [m.press5 + 6, m.swipe - 2, key('k5', 0), {x: k5Idle.x, y: k5Idle.y + 60}],
    [m.swipe, m.calcOn, {x: k5Idle.x, y: k5Idle.y + 60}, {x: k5Idle.x, y: k5Idle.y - 110}],
    [m.calcOn + 2, m.opPlus - 4, {x: k5Idle.x, y: k5Idle.y - 110}, key('plus', 1)],
    [m.opPlus + 4, m.one - 4, key('plus', 1), key('k1', 1)],
    [m.one + 4, m.five - 4, key('k1', 1), key('k5', 1)],
    [m.five + 6, m.chip - 4, key('k5', 1), CATEGORY_CHIP],
    [m.enter + 10, m.check - 4, {x: 1180, y: 1300}, key('check', 1)],
    [m.check + 6, m.swipe2 - 2, key('check', 1), {x: k5Idle.x, y: k5Idle.y + 60}],
    [m.swipe2, m.calcOn2, {x: k5Idle.x, y: k5Idle.y + 60}, {x: k5Idle.x, y: k5Idle.y - 110}],
    [m.calcOn2 + 2, m.adjPlus - 3, {x: k5Idle.x, y: k5Idle.y - 110}, key('plus', 1)],
    [m.adjPlus + 3, m.two - 3, key('plus', 1), key('k2', 1)],
    [m.two + 3, m.zeroA - 3, key('k2', 1), key('k0', 1)],
    [m.zeroB + 6, m.clear1 - 4, key('k0', 1), key('back', 1)],
    [m.cleared1 + 2, m.swipe3 - 2, key('back', 1), {x: k5Idle.x, y: k5Idle.y + 60}],
    [m.swipe3, m.calcOn3, {x: k5Idle.x, y: k5Idle.y + 60}, {x: k5Idle.x, y: k5Idle.y - 110}],
    [m.calcOn3 + 2, m.adjMinus - 3, {x: k5Idle.x, y: k5Idle.y - 110}, key('minus', 1)],
    [m.adjMinus + 3, m.five2 - 3, key('minus', 1), key('k5', 1)],
    [m.five2 + 3, m.zeroC - 3, key('k5', 1), key('k0', 1)],
    [m.zeroC + 4, m.clear2 - 4, key('k0', 1), key('back', 1)],
    [m.cleared2 + 2, m.zoom + 6, key('back', 1), {x: 1180, y: 1500}],
  ];
  let cur = segs[0][2];
  for (const [t0, t1, a, b] of segs) {
    if (f >= t0) {
      const k = tw(f, t0, t1, 0, 1, IN_OUT);
      cur = {x: lerp(a.x, b.x, k), y: lerp(a.y, b.y, k)};
    }
  }
  const cursorOpacity = 1 - tw(f, m.chip + 6, m.chip + 14, 0, 1) + tw(f, m.enter + 10, m.enter + 16, 0, 1);
  const tagFor = (s: number, e: number) => f >= s - 6 && f < e + 10;
  const tag = tagFor(m.swipe, m.calcOn) || tagFor(m.swipe2, m.calcOn2) || tagFor(m.swipe3, m.calcOn3) ? 'Swipe up' : tagFor(m.clear1, m.cleared1) || tagFor(m.clear2, m.cleared2) ? 'Hold' : undefined;
  const tagIn = tagFor(m.swipe, m.calcOn) ? m.swipe - 6 : tagFor(m.swipe2, m.calcOn2) ? m.swipe2 - 6 : tagFor(m.swipe3, m.calcOn3) ? m.swipe3 - 6 : tagFor(m.clear1, m.cleared1) ? m.clear1 - 6 : m.clear2 - 6;

  const zoomK = tw(f, m.zoom, dur, 0, 1, EMPH_ACC);
  const z = lerp(1, PILL_ZOOM, zoomK);
  const tx = lerp(PILL_CENTER.x, PILL_TARGET.x, zoomK) - PILL_CENTER.x * z;
  const ty = lerp(PILL_CENTER.y, PILL_TARGET.y, zoomK) - PILL_CENTER.y * z;

  const amountText = f >= m.check ? '$40' : money(Math.round(remaining));
  const centered = editing;

  return (
    <AbsoluteFill style={{background: C.primaryContainer, overflow: 'hidden'}}>
      <div style={{position: 'absolute', left: 96, top: 128}}>
        <Headline f={f} lines={['Just type', 'what you spent.']} start={6} stagger={4} size={104} exit={m.swipe - 10} color={C.onPrimaryContainer} />
      </div>
      <div style={{position: 'absolute', left: 96, top: 128}}>
        <Headline f={f} lines={['Swipe up', 'to calculate.']} start={m.swipe - 4} stagger={4} size={104} exit={m.chip - 12} color={C.onPrimaryContainer} />
      </div>
      <div style={{position: 'absolute', left: 96, top: 128}}>
        <Headline f={f} lines={['Tag it with', 'a new category.']} start={m.chip - 6} stagger={4} size={100} exit={m.swipe2 - 10} color={C.onPrimaryContainer} />
      </div>
      <div style={{position: 'absolute', left: 96, top: 140}}>
        <Headline f={f} lines={['Start with + or −,', 'adjust the budget.']} start={m.swipe2 - 4} stagger={4} size={88} exit={m.zoom - 8} color={C.onPrimaryContainer} />
      </div>
      <div style={{position: 'absolute', inset: 0, transform: `translate(${tx}px, ${ty}px) scale(${z})`, transformOrigin: '0 0'}}>
        <div style={{position: 'absolute', left: PHONE_LEFT, top: PHONE_TOP, transform: `translateY(${(1 - enter) * 1100}px) rotate(${(1 - enter) * 6}deg)`, transformOrigin: '50% 100%'}}>
          <Phone width={PHONE_W}>
            <EditorScreen
              pillProgress={progress}
              pillColorProgress={progress}
              pillAmount={amountText}
              pillCentered={centered}
              pillPreview={preview}
              editing={editing}
              calc={calc}
              display={display}
              displayScale={(0.9 + 0.1 * pop) * lerp(1, 0.25, fly)}
              displayOpacity={1 - fly}
              displayY={-lerp(0, 170, fly)}
              chips={chips}
              category={category}
              categoryPress={tap(f, m.chip)}
              keyboard={keyboard}
              kbActive={kbActive}
              kbActiveAmt={kbActiveAmt}
              pressed={pressed}
            />
          </Phone>
        </div>
      </div>
      <Cursor x={cur.x} y={cur.y} f={f} presses={[m.press4, m.press5, m.swipe, m.opPlus, m.one, m.five, m.chip, m.check, m.swipe2, m.adjPlus, m.two, m.zeroA, m.zeroB, m.clear1, m.swipe3, m.adjMinus, m.five2, m.zeroC, m.clear2]} tag={tag} tagIn={tagIn} opacity={Math.min(1, cursorOpacity) * (1 - tw(f, m.zoom - 6, m.zoom + 2, 0, 1))} />
    </AbsoluteFill>
  );
};

