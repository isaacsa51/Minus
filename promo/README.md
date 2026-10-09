# Minus — 36 s vertical promo

`output/minus-promo.mp4`: 1080×1920 (9:16), 60 fps, exactly 36.000 s (2160 frames), H.264 + AAC 320 kbps.

## Folder map

```
promo/
├── output/minus-promo.mp4        final video
├── remotion/                     runnable Remotion 4 project
│   ├── src/timeline.json         single source of truth: scene ranges, animation marks, audio cues, time scale
│   ├── src/theme.ts              Minus light-theme tokens + Google Sans Flex helper
│   ├── src/anim.ts               easings, M3 Expressive springs, pill color ramp
│   ├── src/components/           Aura, Cursor, Logo, Budget Pill, Phone + editor, icons, kinetic text
│   ├── src/scenes/S01…S11        one file per scene
│   ├── scripts/stills.mjs        renders review stills without a full render
│   └── scripts/mux.mjs           muxes the mastered WAV and trims to the timeline length
├── audio/
│   ├── generate_audio.py         synthesizes music + SFX from timeline.json, masters, self-checks
│   ├── stems/music.wav, sfx.wav  generated stems
│   └── cue_sheet.csv             every SFX hit with its time and frame
├── assets/                       Remotion public dir
│   ├── fonts/google_sans_flex.ttf   copied from app/src/main/res/font
│   ├── badges/                      Google Play, GitHub and IzzyOnDroid store badges
│   ├── logo/                        launcher-icon SVGs
│   └── audio/minus_promo_mix.wav    final mastered soundtrack used by the video
└── research/
    ├── RESEARCH.md               reference study, brand sources, verified budget logic
    ├── verification/             PromoScenarioTest.kt + its output (real BudgetStateCalculator)
    ├── reference-frames/         contact sheets of the reference video
    └── review/                   review stills, MP4 frame sheets, audio overview, render log, review_video.py
```

## Rebuild

```bash
cd promo/remotion && npm install
```

```bash
cd promo/audio && python generate_audio.py
```

```bash
cd promo/remotion && npm run build
```

`npm run build` renders the frames with Remotion, then `npm run finalize` remuxes the untouched H.264 stream with the mastered WAV, using Remotion's bundled ffmpeg. That trims the AAC padding so video, audio and container all match the timeline length exactly (36.000 s).

`generate_audio.py` needs `numpy` only. It reads `remotion/src/timeline.json`, so if you move an animation mark, the matching sound moves with it. Re-run it before rendering. It masters to −19 dBFS RMS with a −1.5 dBTP true-peak ceiling (4× oversampled). It fails loudly if the mix length doesn't match the timeline, exceeds that true peak, ends non-silent, or a cue has no audible onset.

If you change only the audio, skip the 10-minute render. `npm run remux-audio` swaps the new WAV into the existing MP4 and keeps the video stream untouched.

`npm run studio` opens Remotion Studio for scrubbing. `node scripts/stills.mjs ../research/review 0,600,1200` renders specific frames.

## Spanish version (`remotion-es/`)

This is a full copy of the project with every on-screen text in Mexican Spanish. UI strings come from the app's `values-es-rMX/strings.xml`, and dates and weekdays follow es-MX formatting ("01 jul", "Julio", D L M M J V S). The composition is `MinusPromoEs`.

The copy shares `assets/`, including the font, badges and the same soundtrack, because its `timeline.json` is identical. Its output is `output/minus-promo-es.mp4`.

```bash
cd promo/remotion-es && npm run build
```

If you change the Spanish timeline, regenerate the audio for it first: point `generate_audio.py`'s `TIMELINE`/`MIX_OUT` at `remotion-es`.

## Review checks done on the final file

- Streams: H.264 1080×1920, 60 fps, 2160 frames; AAC 48 kHz stereo; container duration 36.000 s.
- Frame-exact sheets (`research/review/mp4/`): every 30th frame plus the frames either side of each scene cut.
- Sync: the MP4's decoded audio cross-correlates with the master at 0 samples of lag, and every cue is placed on its animation frame from `timeline.json`.
- Loudness: −17.2 LUFS integrated, peak −5.5 dBFS after AAC encoding, fade to digital silence on the last sample. Taps are soft wooden clicks, not pitched pings, and bells are reserved for the final logo lock.
- Facts: every amount matches `research/verification/test-output.txt` from the app's own `BudgetStateCalculator`.

## Storyboard

Scenes and marks in `timeline.json` are written in design frames (`designFrames` = 1884) and stretched to the 2160 output frames by the time scale (`durationInFrames / designFrames` ≈ 1.146). Scenes call `useSceneFrame()`, and `generate_audio.py` applies the same factor, so pacing is changed in one place. The splash builds stay at the app's real 600 ms.

Every scene exits with a Gaussian blur and fade (`transitionFrames` = 16 design frames).

| Time | Scene | What it shows |
|---|---|---|
| 0.0–2.9 s | wordmark | Fade from black into the animated gradient, giant cropped "Minus" sharpening, README tagline |
| 2.9–4.2 s | splash | The app's splash animation (`ic_splash_icon_animated.xml`, kept at its real 600 ms): + − × pop in at 0/70/140 ms, the $ badge scales and rotates from −60° at 220 ms; then the camera dives into the green coin |
| 4.2–7.6 s | question | "What / if / BUDGET" cuts → dotted grid → typed "did the math?" |
| 7.6–19.0 s | pill | Type $45 → swipe up for calculator mode → $45+15 = $60 → tag a new "Groceries" category on the keyboard → save (pill $40) → swipe up, "+200" ("$200 will be added") → hold ⌫ to clear → swipe up, "−50" ("$50 will be subtracted") → hold ⌫ → zoom into the pill |
| 19.0–21.9 s | sheet | Tap the pill → BudgetPeriodSheet: $60 Spent · Available 96%, $1,400 Total budget 01 Jul → 14 Jul, 13 days remaining, split toggle, Calculated amount $100 → Weekly $700; sheet slides down on exit |
| 21.9–25.9 s | split | Day 2 under the four split modes: $100 / $103.08 / $140 / $100 + $40 pending |
| 25.9–28.0 s | choice | "Pending extra money" pill → Spread $103.08 vs Add it all to today $140 |
| 28.0–31.1 s | subs | Subscriptions: $92.64/mo, the two-week period calendar, Due soon / Upcoming list, zoom out into a phone |
| 31.1–33.5 s | analytics | Total Spent trend, min/max cards, calendar heatmap |
| 33.5–36.0 s | outro | Animated gradient, the splash logo build on the icon's #F2F3EB circle, "Minus", tagline, store badges |

All amounts are verified against the app's own calculator. See `research/RESEARCH.md`.
