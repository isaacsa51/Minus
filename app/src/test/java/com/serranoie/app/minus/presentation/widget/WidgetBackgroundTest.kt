package com.serranoie.app.minus.presentation.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.unit.ColorProvider
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBackgroundTest {

    private val modifierNames = GlanceModifier
        .widgetBackground(ColorProvider(Color.White))
        .foldIn(emptyList<String>()) { acc, element -> acc + element.javaClass.simpleName }

    @Test
    fun `marks the root as the launcher background`() {
        assertTrue(modifierNames.toString(), "AppWidgetBackgroundModifier" in modifierNames)
    }

    @Test
    fun `clips to the system widget radius`() {
        assertTrue(modifierNames.toString(), "CornerRadiusModifier" in modifierNames)
    }
}
