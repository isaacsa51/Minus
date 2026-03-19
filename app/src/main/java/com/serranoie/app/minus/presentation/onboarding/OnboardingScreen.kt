package com.serranoie.app.minus.presentation.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.theme.component.DescriptionButton
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.NumberedRow

@Composable
fun OnboardingScreen(
	onSetBudget: () -> Unit = {}, onClose: () -> Unit = {}, onOnboardingComplete: () -> Unit = {}
) {
	WelcomeStep(
		onSetBudget = onSetBudget,
	)
}

@Composable
private fun WelcomeStep(
	onSetBudget: () -> Unit = {},
) {
	val localBottomSheetScrollState = LocalBottomSheetScrollState.current
	val navigationBarHeight =
		LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)
	Surface(
		modifier = Modifier
			.fillMaxSize()
			.padding(top = localBottomSheetScrollState.topPadding)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(start = 24.dp, end = 24.dp, bottom = navigationBarHeight),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Spacer(Modifier.height(24.dp))
			Text(
				text = "Bienvenido a Minus!",
				style = MaterialTheme.typography.displayMedium,
			)
			Spacer(Modifier.height(16.dp))
			Text(
				text = "Hola y bienvenido, empezemos a ahorrar juntos",
				style = MaterialTheme.typography.titleMedium,
				textAlign = TextAlign.Center,
			)
			Spacer(Modifier.height(48.dp))
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.Start,
			) {
				NumberedRow(
					number = 1,
					title = "Establece un presupuesto",
					subtitle = "Calcula aproximadamente cuanto dinero ocupas en un periodo de tiempo y mantente informado cuanto puedes guardar.",
				)
				NumberedRow(
					number = 2,
					title = "Guarda cada gasto",
					subtitle = "Esta app te ayudará a calcular cuanto ocupas gastar por día/semana/quincena o mes para que estes dentro de este rango y tengas noción de cuánto te queda.",
				)
				NumberedRow(
					number = 3,
					title = "Gasta sabiamente",
					subtitle = "Con el tiempo, aprenderas a sentir cuanto puedes ahorrar y cuanto puedes gastar.",
				)
			}
			Spacer(Modifier.height(48.dp))
			DescriptionButton(
				title = { Text("Establece un presupuesto") },
				contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
				onClick = {
					onSetBudget()
				})
		}
	}
}
