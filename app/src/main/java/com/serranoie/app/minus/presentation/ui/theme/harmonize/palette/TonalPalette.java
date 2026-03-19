package com.serranoie.app.minus.presentation.ui.theme.harmonize.palette;

import com.serranoie.app.minus.presentation.ui.theme.harmonize.hct.Hct;

import java.util.HashMap;
import java.util.Map;

/**
 * A convenience class for retrieving colors that are constant in hue and chroma, but vary in tone.
 */
public final class TonalPalette {
    Map<Double, Integer> cache;
    double hue;
    double chroma;

    /**
     * Create tones using the HCT hue and chroma from a color.
     *
     * @param argb ARGB representation of a color
     * @return Tones matching that color's hue and chroma.
     */
    public static TonalPalette fromInt(int argb) {
        Hct hct = Hct.fromInt(argb);
        return TonalPalette.fromHueAndChroma(hct.getHue(), hct.getChroma());
    }

    /**
     * Create tones from a defined HCT hue and chroma.
     *
     * @param hue HCT hue
     * @param chroma HCT chroma
     * @return Tones matching hue and chroma.
     */
    public static TonalPalette fromHueAndChroma(double hue, double chroma) {
        return new TonalPalette(hue, chroma);
    }

    private TonalPalette(double hue, double chroma) {
        cache = new HashMap<>();
        this.hue = hue;
        this.chroma = chroma;
    }

    /**
     * Create an ARGB color with HCT hue and chroma of this Tones instance, and the provided HCT tone.
     *
     * @param tone HCT tone, measured from 0 to 100.
     * @return ARGB representation of a color with that tone.
     */
    // AndroidJdkLibsChecker is higher priority than ComputeIfAbsentUseValue (b/119581923)
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
        Integer color = cache.get((double)tone);
        if (color == null) {
            color = Hct.from(this.hue, this.chroma, tone).toInt();
            cache.put((double)tone, color);
        }
        return color;
    }
}
