import React from 'react';
import {AbsoluteFill, Img, staticFile} from 'remotion';
import {C, gsf} from '../theme';
import {BOUNCY, EMPH, FPS, TIME_SCALE, lerp, scene, sp, SPATIAL, tw, useSceneFrame} from '../anim';
import {Aura, RING_SPRING} from '../components/Backgrounds';
import {LogoMark} from '../components/Logo';
import {splashTiles} from '../components/SplashLogo';
import {Headline} from '../components/Kinetic';

const {m} = scene('outro');

const ICON_BG = '#F2F3EB';
const CIRCLE = 500;
const CY = 640;
const BADGE_H = 92;
const BADGES = [
  {src: 'badges/google_play.png', w: (564 / 168) * BADGE_H},
  {src: 'badges/github.png', w: (564 / 168) * BADGE_H},
  {src: 'badges/izzyondroid.png', w: (1200 / 353) * BADGE_H},
];
const BADGE_GAP = 22;

export const S11Outro: React.FC = () => {
  const f = useSceneFrame();
  const open = tw(f, 0, 30, 0.4, 1, EMPH);
  const circle = sp(f, 2, SPATIAL);
  const ms = ((f - m.plus) * TIME_SCALE * 1000) / FPS;
  const tiles = splashTiles(ms);
  const lock = f >= m.lock ? 1 + Math.sin(Math.min(1, (f - m.lock) / 14) * Math.PI) * 0.05 : 1;
  const word = tw(f, m.lock - 8, m.lock + 12, 0, 1, EMPH);
  const totalW = BADGES.reduce((a, b) => a + b.w, 0) + BADGE_GAP * (BADGES.length - 1);
  let x = 540 - totalW / 2;
  return (
    <AbsoluteFill style={{overflow: 'hidden'}}>
      <Aura f={f + 1668} ring={RING_SPRING} open={open} radius={600} cy={CY + 120} speed={0.9} />
      <div style={{position: 'absolute', left: 540 - CIRCLE / 2, top: CY - CIRCLE / 2, width: CIRCLE, height: CIRCLE, borderRadius: CIRCLE / 2, background: ICON_BG, transform: `scale(${circle * lock})`, boxShadow: '0 30px 70px rgba(58,77,16,0.14)'}} />
      <div style={{position: 'absolute', left: 540 - 165, top: CY - 165, transform: `scale(${lock})`}}>
        <LogoMark size={330} tiles={tiles} />
      </div>
      <div style={{position: 'absolute', left: 0, right: 0, top: 960, textAlign: 'center', ...gsf(800, 110), fontSize: 200, lineHeight: 1, letterSpacing: -4, color: C.onSurface, opacity: word, filter: word < 1 ? `blur(${(1 - word) * 26}px)` : undefined, transform: `scale(${lerp(1.12, 1, word)})`}}>
        Minus
      </div>
      <div style={{position: 'absolute', left: 0, right: 0, top: 1200, display: 'flex', justifyContent: 'center'}}>
        <Headline f={f} lines={['Tu dinero, sin adivinanzas.']} start={m.tagline} stagger={3} size={50} wght={520} wdth={100} color={C.onSurfaceVariant} align="center" />
      </div>
      {BADGES.map((b, i) => {
        const left = x;
        x += b.w + BADGE_GAP;
        const k = sp(f, m.badges + i * 5, BOUNCY);
        return (
          <Img
            key={b.src}
            src={staticFile(b.src)}
            style={{position: 'absolute', left, top: 1460, width: b.w, height: BADGE_H, opacity: Math.min(1, k * 1.5), transform: `translateY(${(1 - k) * 60}px) scale(${0.85 + 0.15 * k})`}}
          />
        );
      })}
    </AbsoluteFill>
  );
};
