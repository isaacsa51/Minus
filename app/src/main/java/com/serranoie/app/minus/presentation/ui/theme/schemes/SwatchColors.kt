package com.serranoie.app.minus.presentation.ui.theme.schemes

import androidx.compose.ui.graphics.Color
import com.serranoie.app.minus.domain.model.AppColorScheme
import com.serranoie.app.minus.presentation.ui.theme.primaryDark
import com.serranoie.app.minus.presentation.ui.theme.primaryLight
import com.serranoie.app.minus.presentation.ui.theme.surfaceVariantDark
import com.serranoie.app.minus.presentation.ui.theme.surfaceLight
import com.serranoie.app.minus.presentation.ui.theme.tertiaryContainerDark
import com.serranoie.app.minus.presentation.ui.theme.tertiaryContainerLight
import com.serranoie.app.minus.presentation.ui.theme.tertiaryDark
import com.serranoie.app.minus.presentation.ui.theme.tertiaryLight

data class SwatchColors(
    val primary: Color,
    val tertiary: Color,
    val surface: Color
)

@Suppress("detekt.CyclomaticComplexMethod", "detekt.LongMethod")
fun getSwatchColors(scheme: AppColorScheme, isDark: Boolean): SwatchColors {
    return when (scheme) {
        AppColorScheme.BRAND -> if (isDark) {
            SwatchColors(
                primary = primaryDark,
                tertiary = tertiaryContainerDark,
                surface = surfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = primaryLight,
                tertiary = tertiaryContainerLight,
                surface = surfaceLight
            )
        }

        AppColorScheme.PINK -> if (isDark) {
            SwatchColors(
                primary = PinkPrimaryDark,
                tertiary = PinkTertiaryContainerDark,
                surface = PinkSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = PinkPrimaryLight,
                tertiary = PinkTertiaryContainerLight,
                surface = PinkSurfaceLight
            )
        }

        AppColorScheme.PINK_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = PinkNeutralPrimaryDark,
                tertiary = PinkNeutralTertiaryContainerDark,
                surface = PinkNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = PinkNeutralPrimaryLight,
                tertiary = PinkNeutralTertiaryContainerLight,
                surface = PinkNeutralSurfaceLight
            )
        }

        AppColorScheme.RED -> if (isDark) {
            SwatchColors(
                primary = RedPrimaryDark,
                tertiary = RedTertiaryContainerDark,
                surface = RedSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = RedPrimaryLight,
                tertiary = RedTertiaryContainerLight,
                surface = RedSurfaceLight
            )
        }

        AppColorScheme.RED_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = RedNeutralPrimaryDark,
                tertiary = RedNeutralTertiaryContainerDark,
                surface = RedNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = RedNeutralPrimaryLight,
                tertiary = RedNeutralTertiaryContainerLight,
                surface = RedNeutralSurfaceLight
            )
        }

        AppColorScheme.BLUE -> if (isDark) {
            SwatchColors(
                primary = BluePrimaryDark,
                tertiary = BlueTertiaryContainerDark,
                surface = BlueSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = BluePrimaryLight,
                tertiary = BlueTertiaryContainerLight,
                surface = BlueSurfaceLight
            )
        }

        AppColorScheme.BLUE_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = BlueNeutralPrimaryDark,
                tertiary = BlueNeutralTertiaryContainerDark,
                surface = BlueNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = BlueNeutralPrimaryLight,
                tertiary = BlueNeutralTertiaryContainerLight,
                surface = BlueNeutralSurfaceLight
            )
        }

        AppColorScheme.ORANGE -> if (isDark) {
            SwatchColors(
                primary = OrangePrimaryDark,
                tertiary = OrangeTertiaryContainerDark,
                surface = OrangeSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = OrangePrimaryLight,
                tertiary = OrangeTertiaryContainerLight,
                surface = OrangeSurfaceLight
            )
        }

        AppColorScheme.ORANGE_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = OrangeNeutralPrimaryDark,
                tertiary = OrangeNeutralTertiaryContainerDark,
                surface = OrangeNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = OrangeNeutralPrimaryLight,
                tertiary = OrangeNeutralTertiaryContainerLight,
                surface = OrangeNeutralSurfaceLight
            )
        }

        AppColorScheme.YELLOW -> if (isDark) {
            SwatchColors(
                primary = YellowPrimaryDark,
                tertiary = YellowTertiaryContainerDark,
                surface = YellowSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = YellowPrimaryLight,
                tertiary = YellowTertiaryContainerLight,
                surface = YellowSurfaceLight
            )
        }

        AppColorScheme.YELLOW_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = YellowNeutralPrimaryDark,
                tertiary = YellowNeutralTertiaryContainerDark,
                surface = YellowNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = YellowNeutralPrimaryLight,
                tertiary = YellowNeutralTertiaryContainerLight,
                surface = YellowNeutralSurfaceLight
            )
        }

        AppColorScheme.AQUA -> if (isDark) {
            SwatchColors(
                primary = AquaPrimaryDark,
                tertiary = AquaTertiaryContainerDark,
                surface = AquaSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = AquaPrimaryLight,
                tertiary = AquaTertiaryContainerLight,
                surface = AquaSurfaceLight
            )
        }

        AppColorScheme.AQUA_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = AquaNeutralPrimaryDark,
                tertiary = AquaNeutralTertiaryContainerDark,
                surface = AquaNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = AquaNeutralPrimaryLight,
                tertiary = AquaNeutralTertiaryContainerLight,
                surface = AquaNeutralSurfaceLight
            )
        }

        AppColorScheme.CYAN -> if (isDark) {
            SwatchColors(
                primary = CyanPrimaryDark,
                tertiary = CyanTertiaryContainerDark,
                surface = CyanSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = CyanPrimaryLight,
                tertiary = CyanTertiaryContainerLight,
                surface = CyanSurfaceLight
            )
        }

        AppColorScheme.CYAN_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = CyanNeutralPrimaryDark,
                tertiary = CyanNeutralTertiaryContainerDark,
                surface = CyanNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = CyanNeutralPrimaryLight,
                tertiary = CyanNeutralTertiaryContainerLight,
                surface = CyanNeutralSurfaceLight
            )
        }

        AppColorScheme.PURPLE -> if (isDark) {
            SwatchColors(
                primary = PurplePrimaryDark,
                tertiary = PurpleTertiaryContainerDark,
                surface = PurpleSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = PurplePrimaryLight,
                tertiary = PurpleTertiaryContainerLight,
                surface = PurpleSurfaceLight
            )
        }

        AppColorScheme.PURPLE_NEUTRAL -> if (isDark) {
            SwatchColors(
                primary = PurpleNeutralPrimaryDark,
                tertiary = PurpleNeutralTertiaryContainerDark,
                surface = PurpleNeutralSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = PurpleNeutralPrimaryLight,
                tertiary = PurpleNeutralTertiaryContainerLight,
                surface = PurpleNeutralSurfaceLight
            )
        }

        AppColorScheme.GREEN -> if (isDark) {
            SwatchColors(
                primary = GreenPrimaryDark,
                tertiary = GreenTertiaryContainerDark,
                surface = GreenSurfaceVariantDark
            )
        } else {
            SwatchColors(
                primary = GreenPrimaryLight,
                tertiary = GreenTertiaryContainerLight,
                surface = GreenSurfaceLight
            )
        }
    }
}
