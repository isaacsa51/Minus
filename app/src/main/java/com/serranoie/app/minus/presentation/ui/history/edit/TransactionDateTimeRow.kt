package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.util.prettyDate
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
internal fun TransactionDateTimeRow(
    date: LocalDate,
    time: LocalTime,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prettyDate(
                date.atStartOfDay(),
                forceShowDate = true,
                showTime = false,
                human = false
            ),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDateClick() }
                .padding(horizontal = 2.dp, vertical = 4.dp)
        )

        Text(
            text = "—",
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Text(
            text = String.format("%02d:%02d", time.hour, time.minute),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onTimeClick() }
                .padding(horizontal = 2.dp, vertical = 4.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun TransactionDateTimeRowPreview() {
    MinusTheme {
        TransactionDateTimeRow(
            date = LocalDate.now(),
            time = LocalTime.of(14, 30),
            onDateClick = {},
            onTimeClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
