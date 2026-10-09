import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C, gsf} from '../theme';
import {BOUNCY, EMPH, IN_OUT, lerp, scene, sp, SPATIAL, tw, useSceneFrame} from '../anim';
import {Headline} from '../components/Kinetic';
import {BackArrowIcon, PencilIcon} from '../components/Icons';

const {m} = scene('subs');

type Sub = {name: string; amount: string; due: string; color: string; avatar: string; onAvatar: string};

const COFFEE: Sub = {name: 'Club de café', amount: '$5', due: 'En 2 días (jul 3)', color: C.catTeal, avatar: '#C9ECEF', onAvatar: '#00363B'};
const STREAMING: Sub = {name: 'Streaming', amount: '$15.99', due: 'En 4 días (jul 5)', color: C.catMagenta, avatar: '#F7D3EC', onAvatar: '#4A0D38'};
const GYM: Sub = {name: 'Gimnasio', amount: '$30', due: 'En 2 semanas (jul 15)', color: C.catIndigo, avatar: '#DCDFFA', onAvatar: '#1C2766'};
const PHONE: Sub = {name: 'Plan de celular', amount: '$25', due: 'En 3 semanas (jul 22)', color: C.catPink, avatar: '#FAD5DC', onAvatar: '#4D0A19'};

const DUE_DOTS: Record<number, string[]> = {3: [C.catTeal], 5: [C.catMagenta], 10: [C.catTeal]};

const FIRST_COL = 3;
const CELL = 423 / 7;
const CAL_TOP = 465;
const LAST_DAY = 18;
const PERIOD_END = 14;
const ROWS = Math.ceil((FIRST_COL + LAST_DAY) / 7);
const FREQ_TOP = CAL_TOP + ROWS * CELL + 20;
const DUE_HEADER = FREQ_TOP + 132 + 22;
const ITEM_H = 150;
const ITEM_GAP = 8;
const ITEMS_TOP = DUE_HEADER + 36;
const UPCOMING_HEADER = ITEMS_TOP + 2 * (ITEM_H + ITEM_GAP) + 12;
const UPCOMING_TOP = UPCOMING_HEADER + 36;
const SCROLL = 250;

const Item: React.FC<{sub: Sub; top: number; reveal: number}> = ({sub, top, reveal}) => (
  <div
    style={{
      position: 'absolute',
      left: 19,
      top,
      width: 423,
      height: ITEM_H,
      borderRadius: 22,
      background: C.surfaceCont,
      padding: '17px 18px',
      boxSizing: 'border-box',
      opacity: reveal,
      transform: `translateY(${(1 - reveal) * 40}px)`,
    }}
  >
    <div style={{display: 'flex', alignItems: 'center', gap: 13}}>
      <div style={{width: 47, height: 47, borderRadius: 24, background: sub.avatar, color: sub.onAvatar, display: 'flex', alignItems: 'center', justifyContent: 'center', ...gsf(700, 125), fontSize: 16}}>{sub.name[0]}</div>
      <div style={{flex: 1}}>
        <div style={{...gsf(620, 85), fontSize: 18.5}}>{sub.name}</div>
        <div style={{...gsf(400, 100), fontSize: 13.5, color: C.onSurfaceVariant, marginTop: 2}}>{sub.due}</div>
      </div>
      <div style={{...gsf(700, 115), fontSize: 16.5}}>{sub.amount}</div>
    </div>
    <div style={{height: 1, background: C.outlineVariant, opacity: 0.5, margin: '13px 0'}} />
    <div style={{display: 'flex', gap: 5, alignItems: 'center'}}>
      <div style={{flex: 1, height: 40, borderRadius: 20, background: C.secondary, color: C.onPrimary, display: 'flex', alignItems: 'center', justifyContent: 'center', ...gsf(700, 125), fontSize: 13.5}}>Marcar pagado</div>
      <div style={{flex: 1, height: 40, borderRadius: 20, background: C.surfaceVariant, color: C.onSurfaceVariant, display: 'flex', alignItems: 'center', justifyContent: 'center', ...gsf(700, 125), fontSize: 13.5}}>Omitir</div>
      <div style={{width: 40, height: 40, borderRadius: 20, background: C.surfaceVariant, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
        <PencilIcon size={18} color={C.secondary} />
      </div>
      <div style={{width: 40, height: 40, borderRadius: 20, background: '#BA1A1A', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
        <svg width={18} height={18} viewBox="0 0 24 24">
          <path d="M7 21a2 2 0 0 1-2-2V7H4V5h5V4h6v1h5v2h-1v12a2 2 0 0 1-2 2H7z" fill="#FFFFFF" />
        </svg>
      </div>
    </div>
  </div>
);

const SubscriptionsScreen: React.FC<{f: number}> = ({f}) => {
  const hero = sp(f, m.hero, BOUNCY);
  const commitment = 92.64 * Math.min(1, sp(f, m.hero + 2, {stiffness: 90, damping: 16, mass: 1}));
  const freq = tw(f, m.dots + 6, m.dots + 40, 0, 1, EMPH);
  return (
    <div style={{position: 'absolute', left: 0, top: 0, width: 461, height: UPCOMING_TOP + 2 * (ITEM_H + ITEM_GAP), background: C.surface, color: C.onSurface}}>
      <div style={{position: 'absolute', left: 18, top: 24}}>
        <BackArrowIcon size={30} color={C.onSurface} />
      </div>
      <div style={{position: 'absolute', left: 20, top: 70, ...gsf(700, 135), fontSize: 33}}>Suscripciones</div>
      <div style={{position: 'absolute', left: 19, top: 151, width: 423, height: 136, borderRadius: 24, background: C.primaryContainer, color: C.onPrimaryContainer, padding: '24px 23px', boxSizing: 'border-box', transform: `scale(${0.92 + 0.08 * hero})`}}>
        <div style={{...gsf(450, 85), fontSize: 16}}>Compromiso mensual</div>
        <div style={{display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 2, opacity: tw(f, m.hero, m.hero + 6, 0, 1, EMPH)}}>
          <span style={{...gsf(780, 135), fontSize: 36}}>${commitment.toFixed(2)}</span>
          <span style={{...gsf(500, 100), fontSize: 17}}>/mes</span>
        </div>
        <div style={{...gsf(450, 85), fontSize: 15, marginTop: 8}}>2% de tu presupuesto en este período</div>
      </div>
      <div style={{position: 'absolute', left: 19, top: 311, width: 423, height: 44, display: 'flex', gap: 3}}>
        {['Todo el período', 'Semanal', 'Categoría'].map((t, i) => (
          <div key={t} style={{flex: 1, borderRadius: i === 0 ? 22 : i === 2 ? '8px 22px 22px 8px' : 8, background: i === 0 ? C.primary : C.surfaceVariant, color: i === 0 ? C.onPrimary : C.onSurfaceVariant, display: 'flex', alignItems: 'center', justifyContent: 'center', ...gsf(450, 90), fontSize: 15}}>
            {t}
          </div>
        ))}
      </div>
      <div style={{position: 'absolute', left: 28, top: 386, ...gsf(700, 135), fontSize: 25}}>Julio</div>
      {['D', 'L', 'M', 'M', 'J', 'V', 'S'].map((d, i) => (
        <div key={i} style={{position: 'absolute', left: 19 + i * CELL, top: 430, width: CELL, textAlign: 'center', ...gsf(700, 125), fontSize: 14, color: C.onSurfaceVariant}}>
          {d}
        </div>
      ))}
      {Array.from({length: LAST_DAY}).map((_, d) => {
        const day = d + 1;
        const idx = FIRST_COL + d;
        const col = idx % 7;
        const row = Math.floor(idx / 7);
        const outside = day > PERIOD_END;
        const dots = DUE_DOTS[day];
        const pop = dots ? sp(f, m.dots + Object.keys(DUE_DOTS).indexOf(String(day)) * 6, BOUNCY) : 0;
        const big = 18;
        const small = 9;
        const tl = row === 0 && col === 0 ? big : small;
        const tr = row === 0 && col === 6 ? big : small;
        const bl = row === ROWS - 1 && col === 0 ? big : small;
        const br = row === ROWS - 1 && col === 6 ? big : small;
        return (
          <div
            key={day}
            style={{
              position: 'absolute',
              left: 19 + col * CELL + 1.1,
              top: CAL_TOP + row * CELL + 1.1,
              width: CELL - 2.2,
              height: CELL - 2.2,
              borderRadius: `${tl}px ${tr}px ${br}px ${bl}px`,
              background: outside ? 'transparent' : day === 1 ? C.primaryContainer : C.surfaceCont,
              border: outside ? '1.1px solid rgba(198,200,185,0.5)' : undefined,
              boxSizing: 'border-box',
            }}
          >
            <div style={{position: 'absolute', top: 8, width: '100%', textAlign: 'center', ...gsf(400, 75), fontSize: 15, color: day === 1 ? C.onPrimaryContainer : C.onSurface}}>{day}</div>
            {dots ? (
              <div style={{position: 'absolute', top: 34, width: '100%', display: 'flex', justifyContent: 'center', gap: 4}}>
                {dots.map((c, i) => (
                  <div key={i} style={{width: 9, height: 9, borderRadius: 5, background: c, transform: `scale(${pop})`}} />
                ))}
              </div>
            ) : null}
          </div>
        );
      })}
      <div style={{position: 'absolute', left: 19, top: FREQ_TOP, width: 423, height: 132, borderRadius: 24, background: C.surfaceCont, padding: '18px 19px', boxSizing: 'border-box'}}>
        <div style={{...gsf(450, 85), fontSize: 16}}>Por frecuencia de facturación</div>
        <div style={{display: 'flex', gap: 3, marginTop: 14, height: 12}}>
          <div style={{width: `${76.6 * freq}%`, borderRadius: 6, background: C.catIndigo}} />
          <div style={{width: `${23.4 * freq}%`, borderRadius: 6, background: C.catPink}} />
        </div>
        <div style={{display: 'flex', gap: 90, marginTop: 14, ...gsf(450, 85), fontSize: 15}}>
          {[
            ['Mensual', '$70.99', C.catIndigo],
            ['Semanal', '$21.65', C.catPink],
          ].map(([l, v, c]) => (
            <div key={l} style={{display: 'flex', gap: 8, alignItems: 'flex-start'}}>
              <div style={{width: 9, height: 9, borderRadius: 5, background: c, marginTop: 6}} />
              <div>
                <div>{l}</div>
                <div style={{...gsf(700, 90), fontSize: 16}}>{v}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
      <div style={{position: 'absolute', left: 18, top: DUE_HEADER, ...gsf(620, 115), fontSize: 18}}>Próximos a vencer</div>
      <Item sub={COFFEE} top={ITEMS_TOP} reveal={tw(f, m.list - 6, m.list + 8, 0, 1, EMPH)} />
      <Item sub={STREAMING} top={ITEMS_TOP + ITEM_H + ITEM_GAP} reveal={tw(f, m.list, m.list + 14, 0, 1, EMPH)} />
      <div style={{position: 'absolute', left: 18, top: UPCOMING_HEADER, ...gsf(620, 115), fontSize: 18}}>Próximos</div>
      <Item sub={GYM} top={UPCOMING_TOP} reveal={tw(f, m.list + 6, m.list + 20, 0, 1, EMPH)} />
      <Item sub={PHONE} top={UPCOMING_TOP + ITEM_H + ITEM_GAP} reveal={tw(f, m.list + 12, m.list + 26, 0, 1, EMPH)} />
    </div>
  );
};

export const S09Subs: React.FC = () => {
  const f = useSceneFrame();
  const toCal = tw(f, m.pan, m.list - 4, 0, 1, IN_OUT);
  const toList = tw(f, m.list, m.zoomOut - 2, 0, 1, IN_OUT);
  const out = tw(f, m.zoomOut, m.settle, 0, 1, EMPH);
  const enter = sp(f, m.in, SPATIAL);
  const s = lerp(lerp(lerp(2.2, 1.9, toCal), 1.9, toList), 1.2, out);
  const fx = 230.5;
  const fy = lerp(lerp(lerp(222, 545, toCal), ITEMS_TOP + 120, toList), SCROLL + 500, out);
  const ax = 540;
  const ay = lerp(990, 1160, out);
  const screenX = ax - fx * s;
  const screenY = ay - (fy - SCROLL) * s;
  const clip = {
    x: lerp(0, screenX, out),
    y: lerp(0, screenY, out),
    w: lerp(1080, 461 * s, out),
    h: lerp(1920, 1000 * s, out),
    r: lerp(0, 34 * s, out),
  };
  const bezel = 13 * out;
  return (
    <AbsoluteFill style={{background: C.surface, overflow: 'hidden'}}>
      <AbsoluteFill style={{background: C.secondaryContainer, opacity: out}} />
      <div style={{position: 'absolute', left: clip.x - bezel, top: clip.y - bezel, width: clip.w + bezel * 2, height: clip.h + bezel * 2, borderRadius: clip.r + bezel, background: C.onSurface, opacity: out, boxShadow: `0 40px 90px rgba(26,28,21,${0.25 * out})`}} />
      <div style={{position: 'absolute', left: clip.x, top: clip.y, width: clip.w, height: clip.h, borderRadius: clip.r, overflow: 'hidden', background: C.surface}}>
        <div style={{position: 'absolute', left: ax - fx * s - clip.x, top: ay - fy * s - clip.y + (1 - enter) * 500, transform: `scale(${s})`, transformOrigin: '0 0'}}>
          <SubscriptionsScreen f={f} />
        </div>
      </div>
      <div style={{position: 'absolute', left: 0, right: 0, top: 0, height: 470, background: `linear-gradient(${C.surface} 78%, rgba(250,250,238,0))`, opacity: 1 - out}} />
      <div style={{position: 'absolute', left: 96, top: 120}}>
        <Headline f={f} lines={['Pagos recurrentes,', 'sin olvidos.']} start={4} stagger={4} size={88} />
      </div>
    </AbsoluteFill>
  );
};
