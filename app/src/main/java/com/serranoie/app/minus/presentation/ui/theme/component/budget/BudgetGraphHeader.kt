package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import java.math.BigDecimal

@Composable
internal fun BudgetGraphHeader(
    totalSpent: BigDecimal,
    currencyFormat: java.text.Format,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.total_spent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currencyFormat.format(totalSpent),
                style = MaterialTheme.typography.displaySmallEmphasized,
                fontSize = 28.sp,
                maxLines = 2,
                softWrap = true
            )
        }
    }
}
