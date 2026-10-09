import React from 'react';
import {AbsoluteFill} from 'remotion';
import {C, gsf} from '../theme';
import {EMPH, IN_OUT, lerp, scene, tw, useSceneFrame} from '../anim';
import {Aura, RING_SPRING} from '../components/Backgrounds';
import {Headline} from '../components/Kinetic';

const {m} = scene('wordmark');

export const WORD_FINAL_SCALE = 0.3;
export const WORD_SIZE = 820;

export const S02Wordmark: React.FC = () => {
  const f = useSceneFrame();
  const open = tw(f, 0, 40, 0.45, 1, EMPH);
  const pan = tw(f, m.crop, m.zoomOut, 300, -140, IN_OUT);
  const zoom = tw(f, m.zoomOut, m.sharp, 0, 1, EMPH);
  const x = lerp(pan, 0, zoom);
  const scale = lerp(1, WORD_FINAL_SCALE, zoom);
  const blur = lerp(lerp(34, 14, tw(f, m.crop, m.crop + 30, 0, 1, EMPH)), 0, tw(f, m.zoomOut + 6, m.sharp, 0, 1, EMPH));
  const flat = tw(f, m.flatten, 150, 0, 1, EMPH);
  return (
    <AbsoluteFill>
      <Aura f={f} ring={RING_SPRING} open={open} flat={flat} flatColor={C.surface} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center'}}>
        <div
          style={{
            ...gsf(800, 110),
            fontSize: WORD_SIZE,
            lineHeight: 1,
            color: C.onPrimaryContainer,
            whiteSpace: 'nowrap',
            letterSpacing: -12,
            opacity: tw(f, m.crop, m.crop + 16, 0, 1, EMPH),
            transform: `translate(${x}px, ${-24 * zoom}px) scale(${scale})`,
            filter: blur > 0.2 ? `blur(${blur}px)` : undefined,
          }}
        >
          Minus
        </div>
      </AbsoluteFill>
      <div style={{position: 'absolute', left: 0, right: 0, top: 1112, display: 'flex', justifyContent: 'center'}}>
        <Headline f={f} lines={['Tu dinero, sin adivinanzas.']} start={m.tagline} stagger={3} size={50} wght={520} wdth={100} color={C.onPrimaryContainer} align="center" />
      </div>
    </AbsoluteFill>
  );
};
