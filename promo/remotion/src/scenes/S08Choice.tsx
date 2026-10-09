import React from 'react';
import {AbsoluteFill, interpolateColors} from 'remotion';
import {C, gsf} from '../theme';
import {EMPH, IN_OUT, lerp, scene, sp, SPATIAL, tw, useSceneFrame} from '../anim';
import {Aura, RING_WARM} from '../components/Backgrounds';
import {BudgetPill} from '../components/Pill';
import {Cursor} from '../components/Cursor';
import {Headline} from '../components/Kinetic';
import {RedoIcon, WeekIcon} from '../components/Icons';

const {m} = scene('choice');

const PILL = {x: 70, y: 420, w: 940, h: 181};
const CARD_X = 70;
const CARD_W = 940;
const CARD_H = 500;
const A_Y = 660;
const B_Y = A_Y + CARD_H + 6;

type Opt = {title: string; desc: string; amount: string; outcome: string; icon: 'week' | 'redo'};

const OPTS: Opt[] = [
  {title: 'Spread over the days left', desc: 'Allocate all unused remaining budget to all remaining days.', amount: '$103.08', outcome: '$103.08 for upcoming days', icon: 'week'},
  {title: 'Add it all to today', desc: 'Add all unused budget to today only.', amount: '$140', outcome: '$100 for tomorrow', icon: 'redo'},
];

const ChoiceCard: React.FC<{opt: Opt; sel: number; first: boolean; y: number; enter: number}> = ({opt, sel, first, y, enter}) => {
  const bg = interpolateColors(sel, [0, 1], [C.button, C.secondaryContainer]);
  const ink = interpolateColors(sel, [0, 1], [C.onButton, C.onSecondaryContainer]);
  const iconBg = interpolateColors(sel, [0, 1], [C.secondaryContainer, C.onSecondaryContainer]);
  const iconInk = interpolateColors(sel, [0, 1], [C.onSecondaryContainer, C.secondaryContainer]);
  const big = 56;
  const small = 10;
  const inner = lerp(small, big, sel);
  const radius = first ? `${big}px ${big}px ${inner}px ${inner}px` : `${inner}px ${inner}px ${big}px ${big}px`;
  const Icon = opt.icon === 'week' ? WeekIcon : RedoIcon;
  return (
    <div
      style={{
        position: 'absolute',
        left: CARD_X,
        top: y,
        width: CARD_W,
        height: CARD_H,
        borderRadius: radius,
        background: bg,
        color: ink,
        padding: '46px 52px',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        transform: `translateY(${(1 - enter) * 520}px)`,
        opacity: Math.min(1, enter * 1.5),
      }}
    >
      <div style={{display: 'flex', alignItems: 'center', gap: 26}}>
        <div style={{width: 84, height: 84, borderRadius: 42, background: iconBg, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0}}>
          <Icon size={46} color={iconInk} />
        </div>
        <div style={{...gsf(620, 115), fontSize: 48, lineHeight: 1.1}}>{opt.title}</div>
      </div>
      <div style={{...gsf(400, 100), fontSize: 36, lineHeight: 1.3}}>{opt.desc}</div>
      <div>
        <div style={{...gsf(720, 135), fontSize: 96, lineHeight: 1}}>{opt.amount}</div>
        <div style={{...gsf(420, 75), fontSize: 34, opacity: 0.8, marginTop: 10}}>{opt.outcome}</div>
      </div>
    </div>
  );
};

export const S08Choice: React.FC = () => {
  const f = useSceneFrame();
  const settle = tw(f, 0, 12, 0, 1, EMPH);
  const tapPress = f >= m.tap && f < m.tap + 12 ? 1 - Math.abs(f - m.tap - 4) / 8 : 0;
  const selB = sp(f, m.tapB + 2, {stiffness: 500, damping: 40, mass: 1});
  const enterA = sp(f, m.cardA, SPATIAL);
  const enterB = sp(f, m.cardB, SPATIAL);
  const toPill = tw(f, 2, m.tap - 2, 0, 1, IN_OUT);
  const toB = tw(f, m.tap + 8, m.tapB - 2, 0, 1, IN_OUT);
  const cx = lerp(lerp(1150, 820, toPill), 760, toB);
  const cy = lerp(lerp(900, PILL.y + 120, toPill), B_Y + 300, toB);
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <Aura f={f + 970} ring={RING_WARM} radius={560} cy={1100} />
      <AbsoluteFill style={{background: C.surplusTrack, opacity: 1 - settle}} />
      <div style={{position: 'absolute', left: 96, top: 120}}>
        <Headline f={f} lines={['Extra money?', 'Your call.']} start={4} stagger={4} size={104} />
      </div>
      <div style={{position: 'absolute', left: PILL.x, top: PILL.y, transform: `scale(${lerp(1.12, 1, settle) - 0.04 * Math.max(0, tapPress)})`, borderRadius: PILL.h / 2, boxShadow: '0 24px 60px rgba(77,72,0,0.16)'}}>
        <BudgetPill w={PILL.w} h={PILL.h} progress={0} amount="$100" surplus={1} surplusAmount="$40" />
      </div>
      <ChoiceCard opt={OPTS[0]} sel={1 - selB} first y={A_Y} enter={enterA} />
      <ChoiceCard opt={OPTS[1]} sel={selB} first={false} y={B_Y} enter={enterB} />
      <Cursor x={cx} y={cy} f={f} presses={[m.tap, m.tapB]} opacity={tw(f, 0, 6, 0, 1)} />
    </AbsoluteFill>
  );
};
