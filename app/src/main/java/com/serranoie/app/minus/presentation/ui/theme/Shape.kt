package com.serranoie.app.minus.presentation.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SlantedShape = GenericShape { size, _ ->
    val slant = 16f
    val r = 32f

    moveTo(slant + r, 0f)
    lineTo(size.width - r, 0f)
    quadraticTo(size.width, 0f, size.width - slant * r / size.height, r)
    lineTo(size.width - slant + slant * r / size.height, size.height - r)
    quadraticTo(size.width - slant, size.height, size.width - slant - r, size.height)
    lineTo(r, size.height)
    quadraticTo(0f, size.height, slant * r / size.height, size.height - r)
    lineTo(slant - slant * r / size.height, r)
    quadraticTo(slant, 0f, slant + r, 0f)
    close()
}

val shape = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
