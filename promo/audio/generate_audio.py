"""Synthesizes the Minus promo soundtrack: a soft F-major music bed plus UI sound effects.

Every effect is placed from ../remotion/src/timeline.json, the same file the Remotion scenes
read their animation marks from, so audio and picture share one source of truth.

Usage:  python generate_audio.py          (writes stems, the final mix and a cue sheet, then self-checks;
        mastered to -19 dBFS RMS with a -1.5 dBTP true-peak ceiling, measured with 4x oversampling)
Needs:  numpy
"""

from __future__ import annotations

import csv
import json
import math
import wave
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
PROMO = HERE.parent
TIMELINE = json.loads((PROMO / "remotion" / "src" / "timeline.json").read_text(encoding="utf-8"))
MIX_OUT = PROMO / "assets" / "audio" / "minus_promo_mix.wav"
STEMS = HERE / "stems"
CUE_SHEET = HERE / "cue_sheet.csv"

SR = 48_000
FPS = TIMELINE["fps"]
TIME_SCALE = TIMELINE["durationInFrames"] / TIMELINE["designFrames"]
DURATION = TIMELINE["durationInFrames"] / FPS
N = int(round(DURATION * SR))
BEAT = 60.0 / TIMELINE["bpm"]
TARGET_RMS_DB = -19.0
BAR = BEAT * 4
rng = np.random.default_rng(20261008)

NOTE_INDEX = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "Bb": 10, "B": 11}


def hz(name: str) -> float:
    pitch, octave = name[:-1], int(name[-1])
    midi = 12 * (octave + 1) + NOTE_INDEX[pitch]
    return 440.0 * 2 ** ((midi - 69) / 12)


def db(gain_db: float) -> float:
    return 10 ** (gain_db / 20)


def t_axis(seconds: float) -> np.ndarray:
    return np.arange(int(seconds * SR)) / SR


def exp_decay(t: np.ndarray, tau: float) -> np.ndarray:
    return np.exp(-t / tau)


def attack(t: np.ndarray, seconds: float) -> np.ndarray:
    return np.clip(t / max(seconds, 1e-4), 0, 1) ** 2


def one_pole_lowpass(x: np.ndarray, cutoff_hz: np.ndarray | float) -> np.ndarray:
    cutoff = np.broadcast_to(np.asarray(cutoff_hz, dtype=float), x.shape)
    alpha = 1 - np.exp(-2 * math.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += alpha[i] * (x[i] - acc)
        y[i] = acc
    return y


def highpass_static(x: np.ndarray, cutoff_hz: float) -> np.ndarray:
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec *= 1 / np.sqrt(1 + (cutoff_hz / np.maximum(f, 1e-3)) ** 4)
    return np.fft.irfft(spec, len(x))


def lowpass_static(x: np.ndarray, cutoff_hz: float) -> np.ndarray:
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec *= 1 / np.sqrt(1 + (f / cutoff_hz) ** 4)
    return np.fft.irfft(spec, len(x))


def place(buf: np.ndarray, sound: np.ndarray, start_s: float, gain: float = 1.0, pan: float = 0.0) -> None:
    start = int(round(start_s * SR))
    if sound.ndim == 1:
        left = math.cos((pan + 1) * math.pi / 4)
        right = math.sin((pan + 1) * math.pi / 4)
        sound = np.stack([sound * left, sound * right], axis=1) * math.sqrt(2)
    end = min(N, start + len(sound))
    if end > start:
        buf[start:end] += sound[: end - start] * gain


# ---------------------------------------------------------------- sound design: effects

def sfx_pop(note: str) -> np.ndarray:
    t = t_axis(0.42)
    f0 = hz(note)
    glide = f0 * (1 + 0.35 * np.exp(-t / 0.02))
    phase = 2 * math.pi * np.cumsum(glide) / SR
    body = np.sin(phase) * exp_decay(t, 0.07) + 0.12 * np.sin(2 * phase) * exp_decay(t, 0.03)
    return body * attack(t, 0.005)


def sfx_click() -> np.ndarray:
    t = t_axis(0.1)
    noise = lowpass_static(highpass_static(rng.standard_normal(len(t)), 300), 2400) * exp_decay(t, 0.006)
    noise /= np.max(np.abs(noise)) + 1e-9
    body = np.sin(2 * math.pi * 165 * t) * exp_decay(t, 0.022) + 0.3 * np.sin(2 * math.pi * 480 * t) * exp_decay(t, 0.008)
    return (0.55 * noise + body) * attack(t, 0.0015)


def sfx_tick(pitch: float = 1800.0) -> np.ndarray:
    t = t_axis(0.05)
    noise = lowpass_static(highpass_static(rng.standard_normal(len(t)), pitch * 0.5), pitch * 1.6) * exp_decay(t, 0.004)
    return noise / (np.max(np.abs(noise)) + 1e-9) + 0.25 * np.sin(2 * math.pi * 220 * t) * exp_decay(t, 0.01)


def sfx_whoosh(dur: float, direction: str) -> np.ndarray:
    t = t_axis(dur)
    k = t / dur
    noise = rng.standard_normal(len(t))
    sweep = 350 * (14 ** k) if direction == "up" else 5200 * (0.07 ** k)
    filtered = one_pole_lowpass(one_pole_lowpass(noise, sweep), sweep * 1.4)
    peak = 0.62 if direction == "up" else 0.35
    env = np.where(k < peak, np.sin(0.5 * math.pi * k / peak) ** 2, np.cos(0.5 * math.pi * (k - peak) / (1 - peak)) ** 2)
    out = filtered * env
    return out / (np.max(np.abs(out)) + 1e-9)


def sfx_swell(dur: float) -> np.ndarray:
    t = t_axis(dur + 0.06)
    k = np.clip(t / dur, 0, 1)
    noise = one_pole_lowpass(rng.standard_normal(len(t)), 500 + 5000 * k ** 2)
    gliss = np.sin(2 * math.pi * np.cumsum(220 + 700 * k ** 2) / SR)
    env = k ** 2.2 * np.where(t > dur, np.exp(-(t - dur) / 0.015), 1)
    out = (noise / (np.max(np.abs(noise)) + 1e-9) * 0.8 + 0.35 * gliss) * env
    return out / (np.max(np.abs(out)) + 1e-9)


def sfx_impact(f_hi: float = 78.0, f_lo: float = 44.0, tau: float = 0.26, length: float = 0.9) -> np.ndarray:
    t = t_axis(length)
    freq = f_lo + (f_hi - f_lo) * np.exp(-t / 0.07)
    phase = 2 * math.pi * np.cumsum(freq) / SR
    sub = np.sin(phase) * exp_decay(t, tau)
    harm = 0.3 * np.sin(2 * phase) * exp_decay(t, tau * 0.4)
    noise = lowpass_static(rng.standard_normal(len(t)), 1400) * exp_decay(t, 0.05) * 0.6
    return (sub + harm + noise) * attack(t, 0.003)


def sfx_thump() -> np.ndarray:
    return sfx_impact(f_hi=110, f_lo=62, tau=0.11, length=0.45) * 0.9


def bell(f0: float, length: float = 1.4, tau: float = 0.55) -> np.ndarray:
    t = t_axis(length)
    partials = [(1.0, 1.0, tau), (2.0, 0.2, tau * 0.7), (2.76, 0.12, tau * 0.35)]
    out = sum(a * np.sin(2 * math.pi * f0 * r * t) * exp_decay(t, d) for r, a, d in partials)
    return out * attack(t, 0.003)


def marimba(f0: float, length: float = 0.9) -> np.ndarray:
    t = t_axis(length)
    out = np.sin(2 * math.pi * f0 * t) * exp_decay(t, 0.3) + 0.16 * np.sin(2 * math.pi * f0 * 3.93 * t) * exp_decay(t, 0.035)
    return out * attack(t, 0.004)


def sfx_chime(notes: list[str]) -> np.ndarray:
    out = np.zeros(int(1.9 * SR))
    for i, n in enumerate(notes):
        b = bell(hz(n)) * (0.9 ** i)
        s = int(i * 0.075 * SR)
        out[s : s + len(b)] += b[: len(out) - s]
    return out / (np.max(np.abs(out)) + 1e-9)


def sfx_press() -> np.ndarray:
    t = t_axis(0.2)
    body = np.sin(2 * math.pi * 140 * t) * exp_decay(t, 0.03)
    click = sfx_click()
    body[: len(click)] += 0.6 * click
    return body


def sfx_bloom(dur: float) -> np.ndarray:
    t = t_axis(dur)
    out = np.zeros(len(t))
    for i, n in enumerate(["F4", "A4", "C5", "E5", "G5", "C6"]):
        f0 = hz(n)
        env = attack(t, 0.35 + 0.05 * i) * np.exp(-np.maximum(t - 0.4, 0) / (dur * 0.45))
        vib = 1 + 0.002 * np.sin(2 * math.pi * (4.2 + i * 0.3) * t)
        out += np.sin(2 * math.pi * np.cumsum(f0 * vib) / SR) * env / (1 + i * 0.35)
    breath = one_pole_lowpass(rng.standard_normal(len(t)), 2500) * attack(t, 0.25) * np.exp(-t / (dur * 0.35))
    out += 0.6 * breath / (np.max(np.abs(breath)) + 1e-9)
    return out / (np.max(np.abs(out)) + 1e-9)


def render_cue(cue: dict) -> list[tuple[float, np.ndarray, float]]:
    """Returns (offset seconds, mono sound, pan) parts for one cue."""
    kind = cue["type"]
    if kind == "pop":
        return [(0, sfx_pop(cue["note"]), rng.uniform(-0.25, 0.25))]
    if kind == "click":
        return [(0, sfx_click(), 0.15)]
    if kind == "whoosh":
        return [(0, sfx_whoosh(cue["dur"], cue["dir"]), 0.0)]
    if kind == "swell":
        return [(0, sfx_swell(cue["dur"]), 0.0)]
    if kind == "impact":
        return [(0, sfx_impact(), 0.0)]
    if kind == "thump":
        return [(0, sfx_thump(), 0.0)]
    if kind == "bloom":
        return [(0, sfx_bloom(cue["dur"]), 0.0)]
    if kind == "chime":
        return [(0, sfx_chime(cue["notes"]), 0.1)]
    if kind == "note":
        return [(0, marimba(hz(cue["note"])), rng.uniform(-0.3, 0.3))]
    if kind == "press":
        return [(0, sfx_press(), 0.0)]
    if kind in ("typing", "ticks"):
        spacing = cue["spacing"] * TIME_SCALE / FPS
        parts = []
        for i in range(cue["count"]):
            pitch = rng.uniform(1500, 2200) if kind == "typing" else 2400 - i * 80
            parts.append((i * spacing, sfx_tick(pitch), rng.uniform(-0.2, 0.2)))
        return parts
    if kind == "plucks":
        spacing = cue["spacing"] * TIME_SCALE / FPS
        return [(i * spacing, marimba(hz(n), 0.7), (i - 2) * 0.15) for i, n in enumerate(cue["notes"])]
    raise ValueError(f"unknown cue type {kind}")


def scene_start(name: str) -> float:
    return round(TIMELINE["scenes"][name]["from"] * TIME_SCALE)


def cue_time(cue: dict) -> float:
    s = TIMELINE["scenes"][cue["scene"]]
    return (scene_start(cue["scene"]) + (s["marks"][cue["mark"]] + cue.get("offset", 0)) * TIME_SCALE) / FPS


# ---------------------------------------------------------------- music bed

PROGRESSION = [
    ["F3", "A3", "C4", "E4", "G4"],
    ["D3", "F3", "A3", "C4", "E4"],
    ["Bb2", "D3", "F3", "A3", "C4"],
    ["C3", "E3", "G3", "A3", "D4"],
    ["F3", "A3", "C4", "E4"],
    ["A2", "C3", "E3", "G3", "B3"],
    ["Bb2", "D3", "F3", "A3", "C4"],
    ["G2", "Bb2", "D3", "F3", "A3"],
]
FINAL_AT = scene_start("outro") / FPS
CHORDS = [(BAR * i, PROGRESSION[i % len(PROGRESSION)]) for i in range(int(math.ceil(FINAL_AT / BAR)))]
CHORDS[-1] = (CHORDS[-1][0], ["C3", "F3", "G3", "Bb3", "D4"])
FINAL_CHORD = ["F2", "C3", "F3", "A3", "C4", "E4", "G4"]


def pad_voice(f0: float, length: float, detune_cents: float) -> np.ndarray:
    t = t_axis(length)
    out = np.zeros(len(t))
    for c, a in ((-detune_cents, 0.5), (0.0, 0.7), (detune_cents, 0.5)):
        f = f0 * 2 ** (c / 1200)
        out += a * (np.sin(2 * math.pi * f * t) + 0.18 * np.sin(4 * math.pi * f * t) + 0.06 * np.sin(6 * math.pi * f * t))
    return out


def build_music() -> np.ndarray:
    music = np.zeros((N, 2))
    sections = CHORDS + [(FINAL_AT, FINAL_CHORD)]
    for i, (start, notes) in enumerate(sections):
        end = sections[i + 1][0] if i + 1 < len(sections) else DURATION
        is_final = i == len(sections) - 1
        length = (end - start) + (0.0 if is_final else 0.9)
        t = t_axis(length)
        env = attack(t, 0.5 if i else 1.2)
        if not is_final:
            env *= np.clip((length - t) / 0.9, 0, 1) ** 1.5
        else:
            env *= np.exp(-t / 3.2)
        for j, n in enumerate(notes):
            voice = pad_voice(hz(n), length, 6 + j) * env * 0.11
            place(music, voice, start, gain=1.0, pan=(j / max(1, len(notes) - 1) - 0.5) * 0.7)
        root = hz(notes[0])
        while root > 110:
            root /= 2
        bass_t = t_axis(length)
        bass = np.sin(2 * math.pi * root * bass_t) * env * 0.16
        place(music, bass, start, gain=1.0)

    arp_order = [0, 2, 4, 2]
    for bar_i, (start, notes) in enumerate(CHORDS):
        if bar_i == 0:
            continue
        for step in range(4):
            when = start + step * BEAT
            if when >= FINAL_AT - 0.05:
                break
            tone = notes[arp_order[step] % len(notes)]
            f0 = hz(tone) * 2
            vel = 0.8 if step == 0 else 0.55
            place(music, marimba(f0, 1.0) * 0.045 * vel, when, pan=-0.35 + 0.7 * (step / 3))

    for k in range(int(FINAL_AT / (BEAT / 2))):
        when = k * BEAT / 2
        if when < BAR * 2 or k % 2 == 0:
            continue
        t = t_axis(0.09)
        hat = highpass_static(rng.standard_normal(len(t)), 7000) * exp_decay(t, 0.018)
        place(music, hat * 0.02, when, pan=0.35)

    for k in range(int(FINAL_AT / (BEAT * 2))):
        when = k * BEAT * 2
        if when < BAR * 4:
            continue
        place(music, sfx_impact(f_hi=95, f_lo=50, tau=0.14, length=0.5) * 0.12, when)

    for i, n in enumerate(["F4", "A4", "C5", "E5", "G5"]):
        place(music, marimba(hz(n), 1.6) * 0.05, FINAL_AT + 0.9 + i * BEAT / 4, pan=-0.4 + 0.2 * i)
    return music


# ---------------------------------------------------------------- reverb + mastering

def reverb(x: np.ndarray, seconds: float = 2.1, predelay: float = 0.022, wet: float = 0.25) -> np.ndarray:
    length = int(seconds * SR)
    t = np.arange(length) / SR
    out = np.empty_like(x)
    size = 1 << int(math.ceil(math.log2(len(x) + length)))
    for ch in range(2):
        ir = rng.standard_normal(length) * np.exp(-t * 6.9 / seconds)
        ir = lowpass_static(ir, 5200)
        ir = np.concatenate([np.zeros(int(predelay * SR)), ir])[:length]
        ir /= np.sqrt(np.sum(ir ** 2))
        wet_sig = np.fft.irfft(np.fft.rfft(x[:, ch], size) * np.fft.rfft(ir, size), size)[: len(x)]
        out[:, ch] = x[:, ch] + wet * wet_sig
    return out


def envelope(x: np.ndarray, window_s: float = 0.03) -> np.ndarray:
    mono = np.abs(x).max(axis=1)
    w = int(window_s * SR)
    kernel = np.ones(w) / w
    return np.convolve(mono, kernel, mode="same")


def write_wav(path: Path, x: np.ndarray) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = np.clip(np.round(x * 32767), -32768, 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def main() -> None:
    sfx = np.zeros((N, 2))
    rows = []
    for cue in TIMELINE["cues"]:
        start = cue_time(cue)
        for offset, sound, pan in render_cue(cue):
            place(sfx, sound, start + offset, gain=db(cue["gain"]), pan=pan)
        rows.append((f"{start:.3f}", round(start * FPS), cue["scene"], cue["mark"], cue["type"], cue["gain"]))
    sfx = reverb(sfx, seconds=1.4, wet=0.14)

    music = reverb(build_music(), seconds=2.6, wet=0.32)
    music *= db(-21) / (np.sqrt(np.mean(music ** 2)) + 1e-9)

    duck = envelope(sfx, 0.06)
    duck = 1 - 0.45 * np.clip(duck / (duck.max() + 1e-9) * 2.2, 0, 1)
    music *= duck[:, None]

    mix = music + sfx
    mix = highpass_static_stereo(mix, 28)
    mix *= db(TARGET_RMS_DB) / np.sqrt(np.mean(mix ** 2))
    knee = 0.72
    over = np.abs(mix) > knee
    mix[over] = np.sign(mix[over]) * (knee + (1 - knee) * np.tanh((np.abs(mix[over]) - knee) / (1 - knee)))
    mix *= min(1.0, db(-1.5) / true_peak(mix))

    fade = int(0.9 * SR)
    ramp = np.cos(np.linspace(0, math.pi / 2, fade)) ** 2
    mix[-fade:] *= ramp[:, None]
    mix[-1] = 0.0
    music[-fade:] *= ramp[:, None]
    sfx[-fade:] *= ramp[:, None]

    write_wav(MIX_OUT, mix)
    write_wav(STEMS / "music.wav", music / max(1.0, np.max(np.abs(music)) / db(-1)))
    write_wav(STEMS / "sfx.wav", sfx / max(1.0, np.max(np.abs(sfx)) / db(-1)))
    with CUE_SHEET.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(["seconds", "frame", "scene", "mark", "type", "gain_db"])
        writer.writerows(sorted(rows, key=lambda r: float(r[0])))
    check(mix, sfx)


def highpass_static_stereo(x: np.ndarray, cutoff: float) -> np.ndarray:
    return np.stack([highpass_static(x[:, c], cutoff) for c in range(2)], axis=1)


def true_peak(x: np.ndarray, factor: int = 4) -> float:
    peak = 0.0
    for ch in range(x.shape[1]):
        spec = np.fft.rfft(x[:, ch])
        up = np.fft.irfft(spec, len(x) * factor) * factor
        peak = max(peak, float(np.max(np.abs(up))))
    return peak


def check(mix: np.ndarray, sfx: np.ndarray) -> None:
    assert mix.shape == (N, 2), mix.shape
    peak_db = 20 * math.log10(true_peak(mix))
    assert peak_db <= -1.4, f"true peak {peak_db:.2f} dBTP"
    assert np.all(mix[-1] == 0), "ending is not silent"
    rms_db = 20 * math.log10(np.sqrt(np.mean(mix ** 2)))
    env = envelope(sfx, 0.01)
    misses = []
    for cue in TIMELINE["cues"]:
        i = int(cue_time(cue) * SR)
        before = env[max(0, i - int(0.05 * SR)) : i].mean() if i > 0 else 0
        window = max(0.12, cue.get("dur", 0) * 0.9)
        after = env[i : i + int(window * SR)].max()
        if after <= before:
            misses.append((cue["scene"], cue["mark"], cue["type"]))
    assert not misses, f"cues without an onset: {misses}"
    print(f"ok  duration={len(mix) / SR:.3f}s  true_peak={peak_db:.2f} dBTP  rms={rms_db:.2f} dBFS  cues={len(TIMELINE['cues'])}")
    print(f"wrote {MIX_OUT}")


if __name__ == "__main__":
    main()
