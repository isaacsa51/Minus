import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C, gsf} from '../theme';
import {EMPH, EMPH_ACC, FPS, TIME_SCALE, lerp, scene, tw, useSceneFrame} from '../anim';
import {LogoMark} from '../components/Logo';
import {splashTiles} from '../components/SplashLogo';
import {WORD_FINAL_SCALE, WORD_SIZE} from './S02Wordmark';

const {m} = scene('splash');

const ICON = 420;
const UNIT = ICON / 255;
const ICON_LEFT = 540 - ICON / 2;
const ICON_TOP = 940 - ICON / 2;
const toCanvas = (ux: number, uy: number) => ({x: ICON_LEFT + (ux + 6) * UNIT, y: ICON_TOP + (uy + 6) * UNIT});
const ZOOM_POINT = toCanvas(131, 174.5);

export const S03Splash: React.FC = () => {
  const f = useSceneFrame();
  const ms = ((f - m.plus) * TIME_SCALE * 1000) / FPS;
  const tiles = splashTiles(ms);

  const zoom = tw(f, m.zoom, 72, 0, 1, EMPH_ACC);
  const z = lerp(1, 70, zoom);
  const cx = lerp(ZOOM_POINT.x, 540, zoom);
  const cy = lerp(ZOOM_POINT.y, 960, zoom);
  const fadeWord = tw(f, m.fade, m.fade + 9, 0, 1, EMPH_ACC);
  const green = tw(f, 64, 71, 0, 1, EMPH);

  return (
    <AbsoluteFill style={{background: C.surface, overflow: 'hidden'}}>
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: 936 - (WORD_SIZE * WORD_FINAL_SCALE) / 2,
          textAlign: 'center',
          ...gsf(800, 110),
          fontSize: WORD_SIZE * WORD_FINAL_SCALE,
          lineHeight: 1,
          letterSpacing: -12 * WORD_FINAL_SCALE,
          color: C.onPrimaryContainer,
          opacity: 1 - fadeWord,
          filter: fadeWord > 0 ? `blur(${fadeWord * 14}px)` : undefined,
          transform: `translateY(${fadeWord * 70}px) scale(${1 - 0.08 * fadeWord})`,
        }}
      >
        Minus
      </div>
      <div style={{position: 'absolute', left: 0, right: 0, top: 1112, textAlign: 'center', ...gsf(520, 100), fontSize: 50, color: C.onPrimaryContainer, opacity: 1 - fadeWord, transform: `translateY(${fadeWord * 70}px)`}}>
        Tu dinero, sin adivinanzas.
      </div>
      <div style={{position: 'absolute', inset: 0, transform: `translate(${cx - ZOOM_POINT.x}px, ${cy - ZOOM_POINT.y}px)`}}>
        <div style={{position: 'absolute', inset: 0, transform: `scale(${z})`, transformOrigin: `${ZOOM_POINT.x}px ${ZOOM_POINT.y}px`}}>
          <div style={{position: 'absolute', left: ICON_LEFT, top: ICON_TOP}}>
            <LogoMark size={ICON} tiles={tiles} />
          </div>
        </div>
      </div>
      <AbsoluteFill style={{background: C.logoGreen, opacity: green}} />
    </AbsoluteFill>
  );
};
