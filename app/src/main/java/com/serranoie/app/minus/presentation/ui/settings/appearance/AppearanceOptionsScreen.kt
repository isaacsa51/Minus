@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.settings.appearance

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.AppColorScheme
import com.serranoie.app.minus.presentation.ui.settings.SettingsUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.FlexibleListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedExpandableList
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SelectablePaddedItem
import com.serranoie.app.minus.presentation.ui.theme.schemes.getSwatchColors

@Composable
fun AppearanceOptionsScreen(
    state: SettingsUiState,
    onThemeChange: (String) -> Unit,
    onTypographyChange: (String) -> Unit,
    onContrastChange: (String) -> Unit,
    onColorSchemeChange: (AppColorScheme) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onMaterialYouToggle: () -> Unit,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val darkTheme = when (state.currentTheme) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppearanceTopBar(onBack, scrollBehavior) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AppearanceMockupPager(state)
            }

            item {
                FlexibleListGroup(title = stringResource(R.string.settings_theme_title)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ThemeSection(state.currentTheme, onThemeChange)

                            if (state.currentColorScheme == AppColorScheme.BRAND) {
                                Text(stringResource(R.string.settings_contrast_title))
                                ContrastSection(state.currentContrast, onContrastChange)
                            }

                            MaterialYouSection(state.isMaterialYouEnabled, onMaterialYouToggle)
                        }

                        ColorSchemeSection(state.currentColorScheme, darkTheme, onColorSchemeChange)
                    }
                }
            }

            item {
                FlexibleListGroup(title = stringResource(R.string.settings_typography_title)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        TypographySection(state.currentTypography, onTypographyChange)
                    }
                }
            }

            item {
                PaddedListGroup(title = stringResource(R.string.settings_section_language)) {
                    LanguageSection(state.currentLanguage, onLanguageChange)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AppearanceTopBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    MediumTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun ThemeSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    val themeOptions = listOf(
        "System" to stringResource(R.string.settings_theme_system),
        "Light" to stringResource(R.string.settings_theme_light),
        "Dark" to stringResource(R.string.settings_theme_dark)
    )

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        themeOptions.forEachIndexed { index, (value, label) ->
            ToggleButton(
                checked = currentTheme == value,
                onCheckedChange = { onThemeChange(value) },
                modifier = Modifier.weight(1f),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    themeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                val icon = when (value) {
                    "Light" -> Icons.Default.LightMode
                    "Dark" -> Icons.Default.DarkMode
                    else -> Icons.Default.Brightness4
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                Text(
                    label,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContrastSection(
    currentContrast: String,
    onContrastChange: (String) -> Unit
) {
    val contrastOptions = listOf(
        "Normal" to stringResource(R.string.settings_contrast_normal),
        "Medium" to stringResource(R.string.settings_contrast_medium),
        "High" to stringResource(R.string.settings_contrast_high)
    )

    FlowRow(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        contrastOptions.forEachIndexed { index, (value, label) ->
            ToggleButton(
                checked = currentContrast == value,
                onCheckedChange = { onContrastChange(value) },
                modifier = Modifier.weight(1f),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    contrastOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    label,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MaterialYouSection(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            stringResource(R.string.settings_material_you_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun ColorSchemeSection(
    currentColorScheme: AppColorScheme,
    isDark: Boolean,
    onColorSchemeChange: (AppColorScheme) -> Unit
) {
    val schemes = AppColorScheme.entries
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_color_scheme_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(schemes) { scheme ->
                ColorSchemeSwatch(
                    scheme = scheme,
                    isDark = isDark,
                    isSelected = currentColorScheme == scheme,
                    onClick = { onColorSchemeChange(scheme) }
                )
            }
        }
    }
}

@Composable
private fun ColorSchemeSwatch(
    scheme: AppColorScheme,
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val swatchColors = getSwatchColors(scheme, isDark)
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Clover4Leaf) }
    val progress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        label = "morphProgress",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(54.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fullPath = MorphPolygonShape.createPath(morph, progress, size, size)

            if (isSelected) {
                drawPath(
                    path = fullPath,
                    color = borderColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            val innerSize = Size(
                width = size.width - if (isSelected) 1.dp.toPx() else 0.dp.toPx(),
                height = size.height - if (isSelected) 1.dp.toPx() else 0.dp.toPx()
            )
            val innerPath = MorphPolygonShape.createPath(morph, progress, innerSize, size)
            
            clipPath(innerPath) {
                drawRect(color = swatchColors.surface)
                
                drawRect(
                    color = swatchColors.primary,
                    size = size,
                    topLeft = Offset(-size.width / 2f, size.height / 2f)
                )
                drawRect(
                    color = swatchColors.tertiary,
                    size = size,
                    topLeft = Offset(size.width / 2f, size.height / 2f)
                )
            }
        }
    }
}

@Immutable
private class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(createPath(morph, progress, size, size))
    }

    companion object {
        fun createPath(
            morph: Morph,
            progress: Float,
            size: Size,
            containerSize: Size
        ): Path {
            val path = morph.toPath(progress).asComposePath()
            val bounds = path.getBounds()

            val maxDimension = maxOf(bounds.width, bounds.height)
            val scale = if (maxDimension > 0f) size.minDimension / maxDimension else 1f

            val matrix = Matrix()
            matrix.scale(scale, scale)
            path.transform(matrix)

            val newBounds = path.getBounds()
            path.translate(containerSize.center - newBounds.center)
            return path
        }
    }
}

@Composable
private fun TypographySection(
    currentTypography: String,
    onTypographyChange: (String) -> Unit
) {
    val mainOptions = listOf(
        "Condensed" to stringResource(R.string.settings_typography_condensed),
        "Expressive" to stringResource(R.string.settings_typography_expressive)
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            mainOptions.forEachIndexed { index, (value, label) ->
                ToggleButton(
                    checked = currentTypography == value,
                    onCheckedChange = { onTypographyChange(value) },
                    modifier = Modifier.weight(1f),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        mainOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        ToggleButton(
            checked = currentTypography == "System",
            onCheckedChange = { onTypographyChange("System") },
            modifier = Modifier.fillMaxWidth(),
            colors = ToggleButtonDefaults.toggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = stringResource(R.string.settings_theme_system),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LanguageSection(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val languages = listOf(
        "en" to stringResource(R.string.settings_language_en),
        "de" to stringResource(R.string.settings_language_de),
        "el" to stringResource(R.string.settings_language_el),
        "es" to stringResource(R.string.settings_language_es),
        "fr" to stringResource(R.string.settings_language_fr),
        "hi" to stringResource(R.string.settings_language_hi),
        "it" to stringResource(R.string.settings_language_it),
        "ja" to stringResource(R.string.settings_language_ja),
        "ko" to stringResource(R.string.settings_language_ko),
        "pt" to stringResource(R.string.settings_language_pt),
        "ru" to stringResource(R.string.settings_language_ru),
        "zh" to stringResource(R.string.settings_language_zh)
    )

    var isExpanded by remember { mutableStateOf(false) }
    val currentLabel = languages.find { it.first == currentLanguage }?.second ?: stringResource(R.string.settings_language_en)

    PaddedExpandableList(
        isExpanded = isExpanded,
        onToggleExpanded = { isExpanded = !isExpanded },
        headerLabel = stringResource(R.string.settings_language_header),
        containerPosition = if (isExpanded) PaddedListItemPosition.First else PaddedListItemPosition.Single,
        headerSubtitle = stringResource(R.string.settings_language_currently_format, currentLabel),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headerSubtitleContent = {
            Text(
                text = stringResource(R.string.settings_language_disclaimer),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        expandedContent = {
            languages.forEachIndexed { index, (code, label) ->
                val position = when {
                    index == languages.lastIndex -> PaddedListItemPosition.Last
                    else -> PaddedListItemPosition.Middle
                }

                SelectablePaddedItem(
                    label = label,
                    isActive = currentLanguage == code,
                    onClick = {
                        onLanguageChange(code)
                        isExpanded = false
                    },
                    position = position
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AppearanceOptionsScreenPreview() {
    MinusTheme {
        AppearanceOptionsScreen(
            state = SettingsUiState(
                currentTheme = "System",
                currentTypography = "Expressive",
                currentContrast = "Normal",
                isMaterialYouEnabled = true
            ),
            onThemeChange = {},
            onTypographyChange = {},
            onContrastChange = {},
            onMaterialYouToggle = {},
            onBack = {}
        )
    }
}

@Preview(name = "Dark Theme")
@Composable
private fun AppearanceOptionsScreenDarkPreview() {
    MinusTheme(darkTheme = true) {
        AppearanceOptionsScreen(
            state = SettingsUiState(
                currentTheme = "Dark",
                currentTypography = "Condensed",
                currentContrast = "High",
                isMaterialYouEnabled = false
            ),
            onThemeChange = {},
            onTypographyChange = {},
            onContrastChange = {},
            onMaterialYouToggle = {},
            onBack = {}
        )
    }
}
