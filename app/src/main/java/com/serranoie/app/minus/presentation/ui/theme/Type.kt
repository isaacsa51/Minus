package com.serranoie.app.minus.presentation.ui.theme

import android.graphics.fonts.FontVariationAxis
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R

val GoogleSansFlex = FontFamily(
	Font(R.font.google_sans_flex, FontWeight.Normal),
	Font(R.font.google_sans_flex, FontWeight.Medium),
	Font(R.font.google_sans_flex, FontWeight.SemiBold),
	Font(R.font.google_sans_flex, FontWeight.Bold)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplayLargeEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(155f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplayMediumEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(155f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplaySmallEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(600), FontVariation.width(155f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeadlineLargeEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(800), FontVariation.width(150f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeadlineMediumEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(150f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeadlineSmallEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(135f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitleLargeEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(135f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitleMediumEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(600), FontVariation.width(135f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitleSmallEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(600), FontVariation.width(135f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBodyLargeEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(500), FontVariation.width(115f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBodyMediumEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(500), FontVariation.width(115f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBodySmallEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(500), FontVariation.width(115f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexLabelLargeEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(125f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexLabelMediumEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(125f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexLabelSmallEmphasized = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(125f)
		)
	)
)

// Condensed Font Families - width below 100 for narrow appearance
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplayLargeCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(75f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplayMediumCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(75f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplaySmallCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(600), FontVariation.width(75f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeadlineLargeCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(800), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeadlineMediumCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexHeadlineSmallCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitleLargeCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitleMediumCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(600), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitleSmallCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(600), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBodyLargeCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(500), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBodyMediumCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(500), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBodySmallCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(500), FontVariation.width(85f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexLabelLargeCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(75f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexLabelMediumCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(75f)
		)
	)
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexLabelSmallCondensed = FontFamily(
	Font(
		R.font.google_sans_flex, variationSettings = FontVariation.Settings(
			FontVariation.weight(700), FontVariation.width(75f)
		)
	)
)

val Typography = Typography(
	displayLarge = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Normal,
		fontSize = 57.sp,
		lineHeight = 64.sp,
		letterSpacing = (-0.25).sp
	), displayMedium = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Normal,
		fontSize = 45.sp,
		lineHeight = 52.sp,
		letterSpacing = 0.sp
	), displaySmall = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Normal,
		fontSize = 36.sp,
		lineHeight = 44.sp,
		letterSpacing = 0.sp
	),

	headlineLarge = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Bold,
		fontSize = 32.sp,
		lineHeight = 40.sp,
		letterSpacing = 0.sp
	), headlineMedium = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Bold,
		fontSize = 28.sp,
		lineHeight = 36.sp,
		letterSpacing = 0.sp
	), headlineSmall = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.SemiBold,
		fontSize = 24.sp,
		lineHeight = 32.sp,
		letterSpacing = 0.sp
	),

	titleLarge = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.SemiBold,
		fontSize = 22.sp,
		lineHeight = 28.sp,
		letterSpacing = 0.sp
	), titleMedium = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Medium,
		fontSize = 16.sp,
		lineHeight = 24.sp,
		letterSpacing = 0.15.sp
	), titleSmall = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Medium,
		fontSize = 14.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.1.sp
	),

	bodyLarge = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Normal,
		fontSize = 16.sp,
		lineHeight = 24.sp,
		letterSpacing = 0.5.sp
	), bodyMedium = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Normal,
		fontSize = 14.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.25.sp
	), bodySmall = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Normal,
		fontSize = 12.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.4.sp
	),

	labelLarge = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Medium,
		fontSize = 14.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.1.sp
	), labelMedium = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Medium,
		fontSize = 12.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.5.sp
	), labelSmall = TextStyle(
		fontFamily = GoogleSansFlex,
		fontWeight = FontWeight.Medium,
		fontSize = 11.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.5.sp
	)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Typography.withEmphasizedStyles(): Typography {
	return this.copy(
		displayLargeEmphasized = TextStyle(
			fontFamily = GoogleSansFlexDisplayLargeEmphasized,
			fontSize = 64.sp,
			lineHeight = 72.sp,
			letterSpacing = 0.sp
		), displayMediumEmphasized = TextStyle(
			fontFamily = GoogleSansFlexDisplayMediumEmphasized,
			fontSize = 52.sp,
			lineHeight = 60.sp,
			letterSpacing = 0.sp
		), displaySmallEmphasized = TextStyle(
			fontFamily = GoogleSansFlexDisplaySmallEmphasized,
			fontSize = 44.sp,
			lineHeight = 52.sp,
			letterSpacing = 0.sp
		),

		// Emphasized Headline - Bold and wide for attention-grabbing headers
		headlineLargeEmphasized = TextStyle(
			fontFamily = GoogleSansFlexHeadlineLargeEmphasized,
			fontSize = 36.sp,
			lineHeight = 44.sp,
			letterSpacing = 0.sp
		), headlineMediumEmphasized = TextStyle(
			fontFamily = GoogleSansFlexHeadlineMediumEmphasized,
			fontSize = 32.sp,
			lineHeight = 40.sp,
			letterSpacing = 0.sp
		), headlineSmallEmphasized = TextStyle(
			fontFamily = GoogleSansFlexHeadlineSmallEmphasized,
			fontSize = 28.sp,
			lineHeight = 36.sp,
			letterSpacing = 0.sp
		), titleLargeEmphasized = TextStyle(
			fontFamily = GoogleSansFlexTitleLargeEmphasized,
			fontSize = 24.sp,
			lineHeight = 32.sp,
			letterSpacing = 0.15.sp
		), titleMediumEmphasized = TextStyle(
			fontFamily = GoogleSansFlexTitleMediumEmphasized,
			fontSize = 18.sp,
			lineHeight = 26.sp,
			letterSpacing = 0.2.sp
		), titleSmallEmphasized = TextStyle(
			fontFamily = GoogleSansFlexTitleSmallEmphasized,
			fontSize = 16.sp,
			lineHeight = 24.sp,
			letterSpacing = 0.15.sp
		), bodyLargeEmphasized = TextStyle(
			fontFamily = GoogleSansFlexBodyLargeEmphasized,
			fontSize = 18.sp,
			lineHeight = 28.sp,
			letterSpacing = 0.6.sp
		), bodyMediumEmphasized = TextStyle(
			fontFamily = GoogleSansFlexBodyMediumEmphasized,
			fontSize = 16.sp,
			lineHeight = 24.sp,
			letterSpacing = 0.4.sp
		), bodySmallEmphasized = TextStyle(
			fontFamily = GoogleSansFlexBodySmallEmphasized,
			fontSize = 14.sp,
			lineHeight = 20.sp,
			letterSpacing = 0.5.sp
		), labelLargeEmphasized = TextStyle(
			fontFamily = GoogleSansFlexLabelLargeEmphasized,
			fontSize = 16.sp,
			lineHeight = 24.sp,
			letterSpacing = 0.15.sp
		), labelMediumEmphasized = TextStyle(
			fontFamily = GoogleSansFlexLabelMediumEmphasized,
			fontSize = 14.sp,
			lineHeight = 20.sp,
			letterSpacing = 0.6.sp
		), labelSmallEmphasized = TextStyle(
			fontFamily = GoogleSansFlexLabelSmallEmphasized,
			fontSize = 12.sp,
			lineHeight = 16.sp,
			letterSpacing = 0.6.sp
		)
	)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Typography.withCondensedStyles(): Typography {
	return Typography(
		displayLarge = TextStyle(
			fontFamily = GoogleSansFlexDisplayLargeCondensed,
			fontSize = 64.sp,
			lineHeight = 72.sp,
			letterSpacing = 0.sp
		),
		displayMedium = TextStyle(
			fontFamily = GoogleSansFlexDisplayMediumCondensed,
			fontSize = 52.sp,
			lineHeight = 60.sp,
			letterSpacing = 0.sp
		),
		displaySmall = TextStyle(
			fontFamily = GoogleSansFlexDisplaySmallCondensed,
			fontSize = 44.sp,
			lineHeight = 52.sp,
			letterSpacing = 0.sp
		),
		headlineLarge = TextStyle(
			fontFamily = GoogleSansFlexHeadlineLargeCondensed,
			fontSize = 36.sp,
			lineHeight = 44.sp,
			letterSpacing = 0.sp
		),
		headlineMedium = TextStyle(
			fontFamily = GoogleSansFlexHeadlineMediumCondensed,
			fontSize = 32.sp,
			lineHeight = 40.sp,
			letterSpacing = 0.sp
		),
		headlineSmall = TextStyle(
			fontFamily = GoogleSansFlexHeadlineSmallCondensed,
			fontSize = 28.sp,
			lineHeight = 36.sp,
			letterSpacing = 0.sp
		),
		titleLarge = TextStyle(
			fontFamily = GoogleSansFlexTitleLargeCondensed,
			fontSize = 24.sp,
			lineHeight = 32.sp,
			letterSpacing = 0.15.sp
		),
		titleMedium = TextStyle(
			fontFamily = GoogleSansFlexTitleMediumCondensed,
			fontSize = 18.sp,
			lineHeight = 26.sp,
			letterSpacing = 0.2.sp
		),
		titleSmall = TextStyle(
			fontFamily = GoogleSansFlexTitleSmallCondensed,
			fontSize = 16.sp,
			lineHeight = 24.sp,
			letterSpacing = 0.15.sp
		),
		bodyLarge = TextStyle(
			fontFamily = GoogleSansFlexBodyLargeCondensed,
			fontSize = 18.sp,
			lineHeight = 28.sp,
			letterSpacing = 0.6.sp
		),
		bodyMedium = TextStyle(
			fontFamily = GoogleSansFlexBodyMediumCondensed,
			fontSize = 16.sp,
			lineHeight = 24.sp,
			letterSpacing = 0.4.sp
		),
		bodySmall = TextStyle(
			fontFamily = GoogleSansFlexBodySmallCondensed,
			fontSize = 14.sp,
			lineHeight = 20.sp,
			letterSpacing = 0.5.sp
		),
		labelLarge = TextStyle(
			fontFamily = GoogleSansFlexLabelLargeCondensed,
			fontSize = 16.sp,
			lineHeight = 24.sp,
			letterSpacing = 0.15.sp
		),
		labelMedium = TextStyle(
			fontFamily = GoogleSansFlexLabelMediumCondensed,
			fontSize = 14.sp,
			lineHeight = 20.sp,
			letterSpacing = 0.6.sp
		),
		labelSmall = TextStyle(
			fontFamily = GoogleSansFlexLabelSmallCondensed,
			fontSize = 12.sp,
			lineHeight = 16.sp,
			letterSpacing = 0.6.sp
		)
	)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ExpressiveTypography = Typography.withEmphasizedStyles()
