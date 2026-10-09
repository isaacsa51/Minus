import React from 'react';
import {interpolateColors} from 'remotion';
import {C, gsf} from '../theme';
import {clamp01, pillColors} from '../anim';

export const BudgetPill: React.FC<{
  w: number;
  h: number;
  progress: number;
  colorProgress?: number;
  label?: string;
  amount: string;
  centered?: number;
  surplus?: number;
  surplusAmount?: string;
  scale?: number;
  preview?: string;
  style?: React.CSSProperties;
}> = ({w, h, progress, colorProgress, label = 'Today', amount, centered = 0, surplus = 0, surplusAmount = '', scale = 1, preview, style}) => {
  const pc = pillColors(colorProgress ?? progress);
  const track = interpolateColors(surplus, [0, 1], [pc.track, C.surplusTrack]);
  const content = interpolateColors(surplus, [0, 1], [pc.content, C.onSurplus]);
  const fs = h * 0.36;
  const pad = h * 0.36;
  const labelFace = 1 - clamp01(centered);
  return (
    <div
      style={{
        position: 'relative',
        width: w,
        height: h,
        borderRadius: h / 2,
        background: track,
        overflow: 'hidden',
        transform: `scale(${scale})`,
        color: content,
        ...style,
      }}
    >
      <div
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          bottom: 0,
          width: `${clamp01(progress) * 100}%`,
          background: pc.fill,
          borderRadius: `0 ${h / 2}px ${h / 2}px 0`,
          opacity: 1 - surplus,
        }}
      />
      <div style={{position: 'absolute', inset: 0, opacity: (1 - surplus) * labelFace, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: `0 ${pad}px`, transform: `translateX(${-centered * w * 0.06}px)`}}>
        <span style={{...gsf(600, 85), fontSize: fs, whiteSpace: 'nowrap'}}>{label}</span>
        <span style={{...gsf(600, 85), fontSize: fs, whiteSpace: 'nowrap'}}>{amount}</span>
      </div>
      <div style={{position: 'absolute', inset: 0, opacity: (1 - surplus) * (1 - labelFace), display: 'flex', alignItems: 'center', justifyContent: 'center', transform: `translateX(${labelFace * w * 0.06}px)`}}>
        <span style={{...gsf(600, 85), fontSize: preview ? fs : fs * 1.3, whiteSpace: 'nowrap'}}>{preview ?? amount}</span>
      </div>
      {surplus > 0 ? (
        <div style={{position: 'absolute', inset: 0, opacity: surplus, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: h * 0.02}}>
          <span style={{...gsf(600, 135), fontSize: h * 0.25, whiteSpace: 'nowrap'}}>Pending extra money</span>
          <span style={{...gsf(700, 125), fontSize: h * 0.17, whiteSpace: 'nowrap'}}>Tap to manage: {surplusAmount}</span>
        </div>
      ) : null}
    </div>
  );
};
