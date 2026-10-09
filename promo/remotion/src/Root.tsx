import React from 'react';
import {Composition} from 'remotion';
import {Promo} from './Promo';
import timeline from './timeline.json';
import {H, W} from './theme';

export const Root: React.FC = () => (
  <Composition id="MinusPromo" component={Promo} durationInFrames={timeline.durationInFrames} fps={timeline.fps} width={W} height={H} />
);
