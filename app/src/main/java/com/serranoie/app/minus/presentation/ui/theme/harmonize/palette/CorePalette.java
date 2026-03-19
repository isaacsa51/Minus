package com.serranoie.app.minus.presentation.ui.theme.harmonize.palette;

import static java.lang.Double.max;
import static java.lang.Double.min;

import com.serranoie.app.minus.presentation.ui.theme.harmonize.hct.Hct;

/**
 * An intermediate concept between the key color for a UI theme, and a full color scheme. 5 sets of
 * tones are generated, all except one use the same hue as the key color, and all vary in chroma.
 */
public final class CorePalette {
    public Hct seed;
    public TonalPalette a1;
    public TonalPalette a2;
    public TonalPalette a3;
    public TonalPalette n1;
    public TonalPalette n2;
    public TonalPalette error;

    /**
     * Create key tones from a color.
     *
     * @param argb ARGB representation of a color
     */
    public static CorePalette of(int argb) {
        return new CorePalette(argb, false);
    }

    /**
     * Create content key tones from a color.
     *
     * @param argb ARGB representation of a color
     */
    public static CorePalette contentOf(int argb) {
        return new CorePalette(argb, true);
    }

    private CorePalette(int argb, boolean isContent) {
        Hct hct = Hct.fromInt(argb);
        double hue = hct.getHue();
        double chroma = hct.getChroma();

        if (isContent) {
            this.a1 = TonalPalette.fromHueAndChroma(hue, chroma);
            this.a2 = TonalPalette.fromHueAndChroma(hue, chroma / 3.);
            this.a3 = TonalPalette.fromHueAndChroma(hue + 60., chroma / 2.);
            this.n1 = TonalPalette.fromHueAndChroma(hue, min(chroma / 12., 4.));
            this.n2 = TonalPalette.fromHueAndChroma(hue, min(chroma / 6., 8.));
        } else {
            this.a1 = TonalPalette.fromHueAndChroma(hue, max(48., chroma));
            this.a2 = TonalPalette.fromHueAndChroma(hue, 16.);
            this.a3 = TonalPalette.fromHueAndChroma(hue + 60., 24.);
            this.n1 = TonalPalette.fromHueAndChroma(hue, 4.);
            this.n2 = TonalPalette.fromHueAndChroma(hue, 8.);
        }
        this.seed = hct;
        this.error = TonalPalette.fromHueAndChroma(25, 84.);
    }
}
