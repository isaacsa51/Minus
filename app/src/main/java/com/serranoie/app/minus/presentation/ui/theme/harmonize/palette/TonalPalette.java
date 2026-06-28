package com.serranoie.app.minus.presentation.ui.theme.harmonize.palette;

import com.serranoie.app.minus.presentation.ui.theme.harmonize.hct.Hct;

import java.util.HashMap;
import java.util.Map;

/**
 * Tones for a fixed HCT hue + chroma. Built internally by {@link CorePalette};
 * callers only invoke {@link #tone(int)} to materialize a specific tone.
 */
public final class TonalPalette {
    Map<Double, Integer> cache;
    double hue;
    double chroma;

    private TonalPalette(double hue, double chroma) {
        cache = new HashMap<>();
        this.hue = hue;
        this.chroma = chroma;
    }

    /** Visible to {@link CorePalette} for palette construction. */
    static TonalPalette of(double hue, double chroma) {
        return new TonalPalette(hue, chroma);
    }

    /**
     * Create an ARGB color with HCT hue and chroma of this palette, and the provided HCT tone.
     *
     * @param tone HCT tone, measured from 0 to 100.
     * @return ARGB representation of a color with that tone.
     */
    @SuppressWarnings("ComputeIfAbsentUseValue")
    public int tone(double tone) {
        Integer color = cache.get(tone);
        if (color == null) {
            color = Hct.from(this.hue, this.chroma, tone).toInt();
            cache.put(tone, color);
        }
        return color;
    }

    public int tone(int tone) {
        Integer color = cache.get((double) tone);
        if (color == null) {
            color = Hct.from(this.hue, this.chroma, tone).toInt();
            cache.put((double) tone, color);
        }
        return color;
    }
}
