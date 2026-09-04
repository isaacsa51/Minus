package com.serranoie.app.minus.presentation.ui.theme

import android.os.Build
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

fun roundness(value: Float): FontVariation.Setting {
    require(value in 0f..100f) { "Roundness (ROND) value must be between 0f and 100f" }
    return FontVariation.Setting("ROND", value)
}

// Version code guard since on API 27 fonts with custom width render incorrectly
internal fun safeVariableWidth(width: Float): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) width else 100f
}

@OptIn(ExperimentalTextApi::class)
fun googleSansFlex(
    weight: Int = 400,
    width: Float = 100f,
    isRounded: Boolean = true
) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.width(safeVariableWidth(width)),
            roundness(if (isRounded) 100f else 0f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
private val GoogleSansFlexBaseRounded = googleSansFlex(isRounded = true)
@OptIn(ExperimentalTextApi::class)
private val GoogleSansFlexBaseNonRounded = googleSansFlex(isRounded = false)

val GoogleSansFlex = GoogleSansFlexBaseRounded

private fun FontFamily?.isRounded(): Boolean {
    return this == GoogleSansFlexBaseRounded
}

fun getTypography(isRounded: Boolean = true): Typography {
    val baseFamily = if (isRounded) GoogleSansFlexBaseRounded else GoogleSansFlexBaseNonRounded
    
    return Typography(
        displayLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val Typography = getTypography()

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
fun Typography.withEmphasizedStyles(isRounded: Boolean = true): Typography {
    return this.copy(
        displayLargeEmphasized = TextStyle(
            fontFamily = googleSansFlex(700, 155f, isRounded),
            fontSize = 64.sp,
            lineHeight = 72.sp,
            letterSpacing = 0.sp
        ),
        displayMediumEmphasized = TextStyle(
            fontFamily = googleSansFlex(600, 132f, isRounded),
            fontSize = 52.sp,
            lineHeight = 60.sp,
            letterSpacing = 0.sp
        ),
        displaySmallEmphasized = TextStyle(
            fontFamily = googleSansFlex(700, 125f, isRounded),
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        headlineLargeEmphasized = TextStyle(
            fontFamily = googleSansFlex(800, 150f, isRounded),
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineMediumEmphasized = TextStyle(
            fontFamily = googleSansFlex(700, 150f, isRounded),
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineSmallEmphasized = TextStyle(
            fontFamily = googleSansFlex(700, 135f, isRounded),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        titleLargeEmphasized = TextStyle(
            fontFamily = googleSansFlex(700, 135f, isRounded),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.15.sp
        ),
        titleMediumEmphasized = TextStyle(
            fontFamily = googleSansFlex(600, 135f, isRounded),
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.2.sp
        ),
        titleSmallEmphasized = TextStyle(
            fontFamily = googleSansFlex(600, 115f, isRounded),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyLargeEmphasized = TextStyle(
            fontFamily = googleSansFlex(500, 115f, isRounded),
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.6.sp
        ),
        bodyMediumEmphasized = TextStyle(
            fontFamily = googleSansFlex(500, 115f, isRounded),
            fontSize = 16.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        bodySmallEmphasized = TextStyle(
            fontFamily = googleSansFlex(500, 115f, isRounded),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.5.sp
        ),
        labelLargeEmphasized = TextStyle(
            fontFamily = googleSansFlex(500, 115f, isRounded),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        labelMediumEmphasized = TextStyle(
            fontFamily = googleSansFlex(700, 125f, isRounded),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.6.sp
        ),
        labelSmallEmphasized = TextStyle(
            fontFamily = googleSansFlex(700, 125f, isRounded),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.6.sp
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
fun Typography.withCondensedStyles(isRounded: Boolean = true): Typography {
    return this.copy(
        displayLarge = TextStyle(
            fontFamily = googleSansFlex(500, 65f, isRounded),
            fontSize = 64.sp,
            lineHeight = 72.sp,
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            fontFamily = googleSansFlex(700, 75f, isRounded),
            fontSize = 52.sp,
            lineHeight = 60.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = googleSansFlex(600, 75f, isRounded),
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = googleSansFlex(800, 85f, isRounded),
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = googleSansFlex(700, 85f, isRounded),
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = googleSansFlex(700, 85f, isRounded),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = googleSansFlex(700, 85f, isRounded),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.15.sp
        ),
        titleMedium = TextStyle(
            fontFamily = googleSansFlex(600, 85f, isRounded),
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.2.sp
        ),
        titleSmall = TextStyle(
            fontFamily = googleSansFlex(400, 85f, isRounded),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = googleSansFlex(400, 70f, isRounded),
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.6.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = googleSansFlex(400, 80f, isRounded),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.4.sp
        ),
        bodySmall = TextStyle(
            fontFamily = googleSansFlex(400, 85f, isRounded),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.5.sp
        ),
        labelLarge = TextStyle(
            fontFamily = googleSansFlex(400, 75f, isRounded),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        labelMedium = TextStyle(
            fontFamily = googleSansFlex(400, 65f, isRounded),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.6.sp
        ),
        labelSmall = TextStyle(
            fontFamily = googleSansFlex(400, 75f, isRounded),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.6.sp
        )
    )
}

val Typography.displayLargeCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(500, 65f, bodyLarge.fontFamily.isRounded()),
            fontSize = 64.sp,
            lineHeight = 72.sp,
            letterSpacing = 0.sp
        )
    } else displayLarge

val Typography.displayMediumCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(700, 75f, bodyLarge.fontFamily.isRounded()),
            fontSize = 52.sp,
            lineHeight = 60.sp,
            letterSpacing = 0.sp
        )
    } else displayMedium

val Typography.displaySmallCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(600, 75f, bodyLarge.fontFamily.isRounded()),
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        )
    } else displaySmall

val Typography.headlineLargeCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(800, 85f, bodyLarge.fontFamily.isRounded()),
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        )
    } else headlineLarge

val Typography.headlineMediumCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(700, 85f, bodyLarge.fontFamily.isRounded()),
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        )
    } else headlineMedium

val Typography.headlineSmallCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(700, 85f, bodyLarge.fontFamily.isRounded()),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        )
    } else headlineSmall

val Typography.titleLargeCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(700, 85f, bodyLarge.fontFamily.isRounded()),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.15.sp
        )
    } else titleLarge

val Typography.titleMediumCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(600, 85f, bodyLarge.fontFamily.isRounded()),
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.2.sp
        )
    } else titleMedium

val Typography.titleSmallCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(400, 85f, bodyLarge.fontFamily.isRounded()),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        )
    } else titleSmall

val Typography.bodyLargeCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(400, 70f, bodyLarge.fontFamily.isRounded()),
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.6.sp
        )
    } else bodyLarge

val Typography.bodyMediumCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(400, 80f, bodyLarge.fontFamily.isRounded()),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.4.sp
        )
    } else bodyMedium

val Typography.bodySmallCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(400, 85f, bodyLarge.fontFamily.isRounded()),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.5.sp
        )
    } else bodySmall

val Typography.labelLargeCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(400, 75f, bodyLarge.fontFamily.isRounded()),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        )
    } else labelLarge

val Typography.labelMediumCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(400, 65f, bodyLarge.fontFamily.isRounded()),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.6.sp
        )
    } else labelMedium

val Typography.labelSmallCondensed: TextStyle
    get() = if (bodyLarge.fontFamily == GoogleSansFlexBaseRounded || bodyLarge.fontFamily == GoogleSansFlexBaseNonRounded) {
        TextStyle(
            fontFamily = googleSansFlex(400, 75f, bodyLarge.fontFamily.isRounded()),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.6.sp
        )
    } else labelSmall

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ExpressiveTypography = Typography.withEmphasizedStyles()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val CondensedTypography = Typography.withCondensedStyles()
