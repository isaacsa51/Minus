import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C, gsf} from '../theme';
import {BOUNCY, EMPH, EMPH_ACC, IN_OUT, lerp, scene, sp, SPATIAL, tw, useSceneFrame} from '../anim';
import {Aura, DotGrid, RING_WARM} from '../components/Backgrounds';
import {Cursor} from '../components/Cursor';

const {m, dur} = scene('question');

const TYPED = 'hiciera las cuentas?';
const TYPE_STEP = 26 / TYPED.length;

export const S04Question: React.FC = () => {
  const f = useSceneFrame();

  if (f < m.if) {
    const s = lerp(1.22, 1, tw(f, 0, 22, 0, 1, EMPH));
    return (
      <AbsoluteFill style={{background: C.logoGreen, alignItems: 'center', justifyContent: 'center'}}>
        <div style={{...gsf(840, 118), fontSize: 300, color: C.onSurface, letterSpacing: -6, transform: `translateY(${-20 + 20 * tw(f, 0, 30, 0, 1)}px) scale(${s})`}}>¿Y si</div>
      </AbsoluteFill>
    );
  }

  if (f < m.grid) {
    const t = f - m.if;
    const chipT = f - m.chip;
    const chipIn = sp(f, m.chip, BOUNCY);
    const preChip = tw(f, m.chip - 14, m.chip, 0, 1, EMPH);
    const ifX = chipT >= 0 ? lerp(0, -900, tw(f, m.chip, m.chip + 10, 0, 1, EMPH_ACC)) : lerp(0, -70, preChip);
    const click = f >= m.click ? Math.max(0, 1 - (f - m.click) / 8) : 0;
    return (
      <AbsoluteFill>
        <Aura f={f + 300} ring={chipT >= 0 ? ['#C3FF95', '#E6F87E', '#FFDBCD', '#D4EC9E'] : RING_WARM} radius={430} />
        <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center'}}>
          <div style={{...gsf(420, 100, {slnt: -10}), fontSize: 330, color: C.tertiary, transform: `translateX(${ifX}px) scale(${lerp(0.9, 1, tw(f, m.if, m.if + 16, 0, 1, EMPH))})`}}>tu</div>
        </AbsoluteFill>
        {chipT < 0 ? (
          <div style={{position: 'absolute', left: lerp(1180, 790, preChip), top: 840, width: 90, height: 230, borderRadius: 26, background: '#FFB74D', filter: 'blur(14px)', opacity: preChip}} />
        ) : (
          <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center'}}>
            <div style={{position: 'absolute', width: 900, height: 700, borderRadius: '50%', background: 'radial-gradient(circle, rgba(195,255,149,0.95), rgba(195,255,149,0) 70%)', filter: 'blur(30px)', opacity: chipIn}} />
            <div
              style={{
                position: 'relative',
                padding: '30px 54px 40px',
                borderRadius: 40,
                background: 'linear-gradient(135deg, #E6F87E 0%, #C3FF95 60%, #A3CD51 100%)',
                transform: `translateX(${(1 - chipIn) * 620}px) scale(${0.85 + 0.15 * chipIn - 0.05 * click})`,
                boxShadow: '0 24px 60px rgba(81,101,38,0.25)',
              }}
            >
              <div style={{...gsf(860, 32), fontSize: 160, lineHeight: 1, color: C.onSurface, letterSpacing: 2}}>PRESUPUESTO</div>
              <Cursor x={lerp(1080, 900, tw(f, m.chip + 4, m.click - 2, 0, 1, EMPH))} y={lerp(380, 200, tw(f, m.chip + 4, m.click - 2, 0, 1, EMPH))} f={f} presses={[m.click]} />
            </div>
          </AbsoluteFill>
        )}
      </AbsoluteFill>
    );
  }

  const camY = lerp(0, -170, tw(f, m.grid, dur, 0, 1, IN_OUT));
  const camX = lerp(30, -10, tw(f, m.grid, m.collapse, 0, 1, IN_OUT));
  const collapse = tw(f, m.collapse, m.collapse + 14, 0, 1, EMPH_ACC);
  const chips = [
    {p: sp(f, m.pop1, BOUNCY), x: 104, y: 780},
    {p: sp(f, m.pop2, BOUNCY), x: 470, y: 968},
    {p: sp(f, m.pop3, BOUNCY), x: 128, y: 1160},
  ];
  const tagIn = sp(f, m.collapse + 6, SPATIAL);
  const fieldIn = sp(f, m.collapse + 4, SPATIAL);
  const typed = Math.max(0, Math.min(TYPED.length, Math.floor((f - m.type) / TYPE_STEP) + 1));
  const caretOn = f < m.type + TYPED.length * TYPE_STEP || Math.floor((f - m.type) / 14) % 2 === 0;
  const pulse = f >= m.pulse ? Math.sin(Math.min(1, (f - m.pulse) / 12) * Math.PI) * 0.035 : 0;
  const chipStyle = (i: number): React.CSSProperties => ({
    position: 'absolute',
    left: lerp(chips[i].x, 110, collapse),
    top: lerp(chips[i].y, 860, collapse),
    transform: `scale(${chips[i].p * lerp(1, 0.25, collapse)})`,
    transformOrigin: '0 0',
    opacity: 1 - collapse,
  });
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <DotGrid bg={C.secondaryContainer} dot="#B5BE9A" offsetX={camX} offsetY={camY} />
      <div style={{position: 'absolute', inset: 0, transform: `translate(${camX}px, ${camY}px)`}}>
        <div style={chipStyle(0)}>
          <div style={{background: C.tertiaryContainer, borderRadius: 10, padding: '6px 34px 18px', ...gsf(650, 100), fontSize: 160, color: C.onSurface, lineHeight: 1.1}}>¿Y si</div>
        </div>
        <div style={chipStyle(1)}>
          <div style={{position: 'relative', background: C.surface, borderRadius: 999, padding: '4px 58px 22px', ...gsf(420, 100, {slnt: -10}), fontSize: 150, color: C.tertiary, lineHeight: 1.1}}>
            tu
          </div>
        </div>
        <div style={chipStyle(2)}>
          <div style={{background: C.onPrimaryContainer, borderRadius: 40, padding: '10px 46px 26px', ...gsf(900, 104), fontSize: 104, color: '#C3FF95', lineHeight: 1.1, whiteSpace: 'nowrap'}}>presupuesto</div>
        </div>
        <div style={{position: 'absolute', left: 110, top: 860, transform: `scale(${tagIn})`, transformOrigin: '0 50%', opacity: tagIn}}>
          <div style={{background: C.onPrimaryContainer, color: '#C3FF95', ...gsf(600, 100), fontSize: 46, padding: '10px 24px 14px', borderRadius: 10, whiteSpace: 'nowrap'}}>¿Y si tu presupuesto</div>
        </div>
        <div
          style={{
            position: 'absolute',
            left: 90,
            top: 980,
            width: 900,
            height: 220,
            borderRadius: 48,
            background: C.surface,
            transform: `translateY(${(1 - fieldIn) * 200}px) scale(${1 + pulse})`,
            opacity: fieldIn,
            display: 'flex',
            alignItems: 'center',
            padding: '0 56px',
            boxShadow: '0 20px 50px rgba(58,77,16,0.12)',
          }}
        >
          <span style={{...gsf(520, 100), fontSize: 84, color: C.onPrimaryContainer, whiteSpace: 'nowrap', letterSpacing: -1}}>{TYPED.slice(0, f >= m.type ? typed : 0)}</span>
          <span style={{width: 7, height: 96, marginLeft: 6, borderRadius: 4, background: C.primary, opacity: caretOn ? 1 : 0}} />
        </div>
      </div>
    </AbsoluteFill>
  );
};
