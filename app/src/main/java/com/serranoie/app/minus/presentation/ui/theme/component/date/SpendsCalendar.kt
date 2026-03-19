package com.serranoie.app.minus.presentation.ui.theme.component.date

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SpendsCalendar(
	modifier: Modifier = Modifier
) {
	Box(modifier) {
		Text(text = "SpendsCalendar")
	}
}

@Preview(name = "SpendsCalendar")
@Composable
private fun PreviewSpendsCalendar() {
	SpendsCalendar()
}