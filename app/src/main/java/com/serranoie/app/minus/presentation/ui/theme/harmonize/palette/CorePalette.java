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
    public TonalPalette n1;

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
            this.a1 = TonalPalette.of(hue, chroma);
            this.n1 = TonalPalette.of(hue, min(chroma / 12., 4.));
        } else {
            this.a1 = TonalPalette.of(hue, max(48., chroma));
            this.n1 = TonalPalette.of(hue, 4.);
        }
        this.seed = hct;
    }
}
