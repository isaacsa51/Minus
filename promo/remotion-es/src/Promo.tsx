import React from 'react';
import {AbsoluteFill, Audio, Sequence, staticFile, continueRender, delayRender} from 'remotion';
import {C, FONT} from './theme';
import {IN_OUT, scene, SceneId, TIME_SCALE, tw, useSceneFrame} from './anim';
import timeline from './timeline.json';
import {S02Wordmark} from './scenes/S02Wordmark';
import {S03Splash} from './scenes/S03Splash';
import {S04Question} from './scenes/S04Question';
import {S05Pill} from './scenes/S05Pill';
import {S06Sheet} from './scenes/S06Sheet';
import {S07Split} from './scenes/S07Split';
import {S08Choice} from './scenes/S08Choice';
import {S09Subs} from './scenes/S09Subs';
import {S10Analytics} from './scenes/S10Analytics';
import {S11Outro} from './scenes/S11Outro';

const fontHandle = delayRender('Loading Google Sans Flex');
const face = new FontFace(FONT, `url(${staticFile('fonts/google_sans_flex.ttf')}) format('truetype')`, {
  weight: '1 1000',
  stretch: '25% 151%',
});
face
  .load()
  .then((loaded) => {
    document.fonts.add(loaded);
    continueRender(fontHandle);
  })
  .catch((err) => {
    console.error(err);
    continueRender(fontHandle);
  });

const SCENES: Array<[SceneId, React.FC]> = [
  ['wordmark', S02Wordmark],
  ['splash', S03Splash],
  ['question', S04Question],
  ['pill', S05Pill],
  ['sheet', S06Sheet],
  ['split', S07Split],
  ['choice', S08Choice],
  ['subs', S09Subs],
  ['analytics', S10Analytics],
  ['outro', S11Outro],
];

const TRANSITION = timeline.transitionFrames;

const BlurFadeOut: React.FC<{dur: number; length: number; children: React.ReactNode}> = ({dur, length, children}) => {
  const f = useSceneFrame();
  const k = length > 0 ? tw(f, dur, dur + length, 0, 1, IN_OUT) : 0;
  return <AbsoluteFill style={{opacity: 1 - k, filter: k > 0.001 ? `blur(${k * 30}px)` : undefined}}>{children}</AbsoluteFill>;
};

const FadeFromBlack: React.FC = () => {
  const f = useSceneFrame();
  const k = tw(f, 0, 42, 0, 1, IN_OUT);
  return k < 1 ? <AbsoluteFill style={{background: '#000000', opacity: 1 - k}} /> : null;
};

export const Promo: React.FC = () => (
  <AbsoluteFill style={{background: C.surface}}>
    {SCENES.map(([id, Comp], i) => {
      const s = scene(id);
      const length = i === SCENES.length - 1 ? 0 : TRANSITION;
      return (
        <Sequence key={id} from={Math.round(s.from * TIME_SCALE)} durationInFrames={Math.round((s.dur + length) * TIME_SCALE)} name={id} style={{zIndex: SCENES.length - i}}>
          <BlurFadeOut dur={s.dur} length={length}>
            <Comp />
          </BlurFadeOut>
        </Sequence>
      );
    })}
    <AbsoluteFill style={{zIndex: 100, pointerEvents: 'none'}}>
      <FadeFromBlack />
    </AbsoluteFill>
    <Audio src={staticFile('audio/minus_promo_mix.wav')} />
  </AbsoluteFill>
);
