package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
internal fun frequencyText(frequency: RecurrentFrequency?): String = when (frequency) {
    RecurrentFrequency.WEEKLY -> stringResource(R.string.recurrent_frequency_weekly)
    RecurrentFrequency.BIWEEKLY -> stringResource(R.string.recurrent_frequency_biweekly)
    RecurrentFrequency.MONTHLY -> stringResource(R.string.recurrent_frequency_monthly)
    null -> ""
}

/** Every [RecurrentFrequency] case, plus the null/"no frequency" fallback. */
@Preview(showBackground = true)
@Composable
private fun FrequencyTextPreview() {
    MinusTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            val frequencies: List<RecurrentFrequency?> = RecurrentFrequency.entries + null
            frequencies.forEach { frequency ->
                Text(text = "$frequency -> \"${frequencyText(frequency)}\"", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
