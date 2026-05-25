package com.serranoie.app.minus.presentation.ui.onboarding

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
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
	val statusBarHeight = LocalWindowInsets.current.calculateTopPadding()
	val navigationBarHeight =
		LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)
	Surface(
		modifier = Modifier
			.fillMaxSize()
			.padding(top = if (localBottomSheetScrollState.topPadding > 0.dp) localBottomSheetScrollState.topPadding else statusBarHeight)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(start = 24.dp, end = 24.dp, bottom = navigationBarHeight),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = stringResource(R.string.onboarding_welcome_title),
				style = MaterialTheme.typography.headlineMediumEmphasized,
				modifier = Modifier.padding(top = 16.dp),
			)
			Spacer(Modifier.height(4.dp))
			Text(
				text = stringResource(R.string.onboarding_welcome_subtitle),
				style = MaterialTheme.typography.titleMediumEmphasized,
				textAlign = TextAlign.Center,
			)
			Spacer(Modifier.height(16.dp))
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.Start,
			) {
				NumberedRow(
					number = 1,
					title = stringResource(R.string.onboarding_step_1_title),
					subtitle = stringResource(R.string.onboarding_step_1_subtitle),
				)
				NumberedRow(
					number = 2,
					title = stringResource(R.string.onboarding_step_2_title),
					subtitle = stringResource(R.string.onboarding_step_2_subtitle),
				)
				NumberedRow(
					number = 3,
					title = stringResource(R.string.onboarding_step_3_title),
					subtitle = stringResource(R.string.onboarding_step_3_subtitle),
				)
			}
			Spacer(Modifier.height(48.dp))
			DescriptionButton(
				title = { Text(stringResource(R.string.onboarding_set_budget_button)) },
				contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
				onClick = {
					onSetBudget()
				})
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
	MinusTheme {
		OnboardingScreen()
	}
}
