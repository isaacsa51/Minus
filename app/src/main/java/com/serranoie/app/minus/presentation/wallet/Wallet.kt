package com.serranoie.app.minus.presentation.wallet

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.editor.EditBudgetContent
import com.serranoie.app.minus.presentation.ui.theme.component.wallet.WalletStatusBarStub

const val WALLET_SHEET = "wallet"

@Composable
fun Wallet(
	forceChange: Boolean = false,
	activityResultRegistryOwner: ActivityResultRegistryOwner? = null,
	budgetViewModel: BudgetViewModel = hiltViewModel(),
	onClose: () -> Unit = {},
	onOnboardingComplete: () -> Unit = {},
) {
	val uiState by budgetViewModel.uiState.collectAsStateWithLifecycle()
	val budgetSettings = uiState.budgetSettings

	Column(modifier = Modifier.fillMaxSize()) {
		WalletStatusBarStub()
		Surface(modifier = Modifier.fillMaxSize()) {
			EditBudgetContent(
				budgetSettings = budgetSettings,
				title = if (budgetSettings != null) "Editar presupuesto" else "Nuevo presupuesto",
				buttonLabel = if (budgetSettings != null) "Actualizar" else "Aplicar",
				showPreviousValuesChip = budgetSettings != null,
				onBack = onClose,
				onApply = { newSettings ->
					budgetViewModel.saveBudgetSettings(
						newSettings,
						forceNewPeriodBoundary = forceChange || budgetSettings == null
					)
					if (forceChange || budgetSettings == null) {
						onOnboardingComplete()
					}
					onClose()
				},
			)
		}
	}
}
