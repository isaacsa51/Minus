import React from 'react';

type P = {size: number; color: string; style?: React.CSSProperties};

const Svg: React.FC<P & {children: React.ReactNode}> = ({size, style, children}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" style={{display: 'block', ...style}}>
    {children}
  </svg>
);

export const BarChartIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <rect x="4" y="10" width="4" height="10" rx="2" fill={p.color} />
    <rect x="10" y="4" width="4" height="16" rx="2" fill={p.color} />
    <rect x="16" y="13" width="4" height="7" rx="2" fill={p.color} />
  </Svg>
);

export const GearIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <g fill={p.color}>
      {Array.from({length: 8}).map((_, i) => (
        <rect key={i} x="10" y="1.6" width="4" height="5" rx="1.2" transform={`rotate(${i * 45} 12 12)`} />
      ))}
      <circle cx="12" cy="12" r="7.4" />
    </g>
    <circle cx="12" cy="12" r="3" fill="#F2F3E4" />
  </Svg>
);

export const BackspaceIcon: React.FC<P & {cut?: string}> = (p) => (
  <Svg {...p}>
    <path d="M8.2 4.5H19.5a2.5 2.5 0 0 1 2.5 2.5v10a2.5 2.5 0 0 1-2.5 2.5H8.2a2 2 0 0 1-1.6-.8L2 12l4.6-6.7a2 2 0 0 1 1.6-.8z" fill={p.color} />
    <path d="M11.2 9.2l5.6 5.6M16.8 9.2l-5.6 5.6" stroke={p.cut ?? '#EDE68C'} strokeWidth="2" strokeLinecap="round" />
  </Svg>
);

export const BackArrowIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <path d="M20 12H5.5M11.5 5.5L5 12l6.5 6.5" stroke={p.color} strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </Svg>
);

export const CheckIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <path d="M4.5 12.5l5 5 10-10.5" stroke={p.color} strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </Svg>
);

export const CardIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <rect x="3" y="5.5" width="18" height="13" rx="2.5" stroke={p.color} strokeWidth="2" fill="none" />
    <rect x="3" y="8.5" width="18" height="2.6" fill={p.color} />
  </Svg>
);

export const EventRepeatIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <path d="M12 20H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v4" stroke={p.color} strokeWidth="2" fill="none" strokeLinecap="round" />
    <path d="M4 9.5h16M8 3v3.5M16 3v3.5" stroke={p.color} strokeWidth="2" strokeLinecap="round" />
    <path d="M21 17.2a4 4 0 1 1-1.3-3" stroke={p.color} strokeWidth="1.9" fill="none" strokeLinecap="round" />
    <path d="M20.6 11.6v2.8h-2.8" stroke={p.color} strokeWidth="1.9" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </Svg>
);

export const WeekIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <rect x="3" y="5" width="18" height="14" rx="2.5" stroke={p.color} strokeWidth="2" fill="none" />
    <path d="M8 6v12M12 6v12M16 6v12" stroke={p.color} strokeWidth="1.8" />
  </Svg>
);

export const RedoIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <path d="M4 16.5c1.4-4.4 6-6.6 10.3-5.2 1.6.5 3 1.5 4 2.7" stroke={p.color} strokeWidth="2.3" fill="none" strokeLinecap="round" />
    <path d="M19.5 9.2v5.3h-5.3" stroke={p.color} strokeWidth="2.3" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </Svg>
);

export const HistoryIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <path d="M4.5 12a7.5 7.5 0 1 0 2.2-5.3" stroke={p.color} strokeWidth="2" fill="none" strokeLinecap="round" />
    <path d="M4 4.5v3.6h3.6M12 8v4.3l3 1.8" stroke={p.color} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </Svg>
);

export const TagIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <path d="M3 5.5V11a2 2 0 0 0 .6 1.4l8 8a2 2 0 0 0 2.8 0l5.6-5.6a2 2 0 0 0 0-2.8l-8-8A2 2 0 0 0 10.6 3.5H5a2 2 0 0 0-2 2z" fill={p.color} />
    <circle cx="7.5" cy="8" r="1.6" fill="#FFFFFF" opacity="0.85" />
  </Svg>
);

export const CalendarIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <rect x="4" y="5" width="16" height="15" rx="2" stroke={p.color} strokeWidth="2" fill="none" />
    <path d="M4 9.5h16M8 3v3.5M16 3v3.5" stroke={p.color} strokeWidth="2" strokeLinecap="round" />
    <path d="M8 13.5h2M12 13.5h2M16 13.5h.5" stroke={p.color} strokeWidth="2" strokeLinecap="round" />
  </Svg>
);

export const PencilIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" fill={p.color} />
  </Svg>
);

export const InfoOutlineIcon: React.FC<P> = (p) => (
  <Svg {...p}>
    <circle cx="12" cy="12" r="9" stroke={p.color} strokeWidth="2" fill="none" />
    <path d="M12 11v6" stroke={p.color} strokeWidth="2" strokeLinecap="round" />
    <circle cx="12" cy="7.6" r="1.25" fill={p.color} />
  </Svg>
);
