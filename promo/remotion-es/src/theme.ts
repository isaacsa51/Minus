import type {CSSProperties} from 'react';

export const C = {
  surface: '#FAFAEE',
  surfaceLow: '#F4F4E8',
  surfaceCont: '#EEEFE3',
  surfaceHigh: '#E9E9DD',
  surfaceHighest: '#E3E3D7',
  surfaceVariant: '#E2E4D4',
  onSurface: '#1A1C15',
  onSurfaceVariant: '#45483C',
  outline: '#76786B',
  outlineVariant: '#C6C8B9',
  primary: '#516526',
  onPrimary: '#FFFFFF',
  primaryContainer: '#D4EC9E',
  onPrimaryContainer: '#3A4D10',
  inversePrimary: '#B8CF84',
  secondary: '#5A6147',
  secondaryContainer: '#DEE6C5',
  onSecondaryContainer: '#424A31',
  tertiary: '#656015',
  tertiaryContainer: '#EDE68C',
  onTertiaryContainer: '#4D4800',
  errorContainer: '#FFDAD6',
  onErrorContainer: '#93000A',
  inverseSurface: '#2F3129',
  logoGreen: '#A3CD51',
  logoDark: '#1A1C15',
  logoGray: '#45483C',
  iconBg: '#E5E7DD',
  editorSheet: '#F2F3E4',
  button: '#F2F3E4',
  onButton: '#2D2F26',
  editChip: '#EFEBAB',
  handle: '#BBC0A4',
  minCard: '#D2E4FF',
  onMinCard: '#0B1D36',
  maxCard: '#FFDBCD',
  onMaxCard: '#360F00',
  maxDot: '#ED6827',
  surplusTrack: '#FFECC6',
  onSurplus: '#261A00',
  catPink: '#EF5F7C',
  catMagenta: '#E65BBD',
  catIndigo: '#7D89E8',
  catTeal: '#00A0AD',
  dueSoon: '#FAEED3',
};

export const PILL_STOPS: Array<[number, string, string]> = [
  [0.0, '#82C153', '#C3FF95'],
  [0.25, '#A9B948', '#E6F87E'],
  [0.5, '#DCA900', '#FFECC6'],
  [0.75, '#FF972F', '#FFE6DA'],
  [0.96, '#FF9564', '#FFE9E1'],
];

export const FONT = 'GoogleSansFlex';

export const gsf = (
  wght: number,
  wdth = 100,
  extra: {rond?: number; slnt?: number} = {},
): CSSProperties => ({
  fontFamily: FONT,
  fontWeight: Math.round(Math.min(1000, Math.max(1, wght))),
  fontStretch: `${wdth}%`,
  fontStyle: 'normal',
  fontOpticalSizing: 'none',
  fontVariationSettings: `'wght' ${wght}, 'wdth' ${wdth}, 'ROND' ${extra.rond ?? 100}, 'slnt' ${extra.slnt ?? 0}`,
});

export const T = {
  titleWide: (size: number) => ({...gsf(700, 135), fontSize: size}),
  headlineEmph: (size: number) => ({...gsf(800, 150), fontSize: size}),
  condensed: (size: number) => ({...gsf(600, 85), fontSize: size}),
  numCondensed: (size: number) => ({...gsf(500, 65), fontSize: size}),
  body: (size: number) => ({...gsf(400, 100), fontSize: size}),
  label: (size: number) => ({...gsf(500, 100), fontSize: size}),
};

export const W = 1080;
export const H = 1920;
