package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class SubscriptionsActions(
    val onBack: () -> Unit = {},
    val onConfirmPaid: (Transaction, LocalDate) -> Unit = { _, _ -> },
    val onSkip: (Transaction, LocalDate) -> Unit = { _, _ -> },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Subscriptions(
    state: SubscriptionsUiState = SubscriptionsUiState(),
    actions: SubscriptionsActions = SubscriptionsActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    val currencyFormat = remember(state.currencyCode) { symbolOnlyCurrencyFormat(state.currencyCode) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.subscriptions_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = actions.onBack,
                        modifier = Modifier.testTag("SubscriptionsBackButton"),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (!state.isLoading && state.dueSoon.isEmpty() && state.upcoming.isEmpty()) {
            SubscriptionsEmptyState(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .testTag("SubscriptionsScreen"),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("SubscriptionsScreen"),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item("calendar") {
                SubscriptionsCalendarSection(
                    monthStart = state.calendarMonthStart,
                    chargesByDay = state.chargesByDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item("hero") {
                SubscriptionsHeroCard(
                    monthlyTotal = state.monthlyTotal,
                    activeCount = state.activeCount,
                    daysUntilNextCharge = state.daysUntilNextCharge,
                    currencyFormatted = currencyFormat.format(state.monthlyTotal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            if (state.dueSoon.isNotEmpty()) {
                item("due-soon-header") {
                    SectionHeader(stringResource(R.string.subscriptions_due_soon_header))
                }
                itemsIndexed(
                    items = state.dueSoon,
                    key = { _, item -> "due-${item.transaction.id}" },
                ) { _, item ->
                    val isDueToday = item.nextChargeDate == LocalDate.now()
                    if (isDueToday) {
                        DueTodayCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onConfirmPaid = { actions.onConfirmPaid(item.transaction, item.nextChargeDate) },
                            onSkip = { actions.onSkip(item.transaction, item.nextChargeDate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    } else {
                        SubscriptionRow(
                            item = item,
                            currencyFormat = currencyFormat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            if (state.upcoming.isNotEmpty()) {
                item("upcoming-header") {
                    SectionHeader(stringResource(R.string.subscriptions_upcoming_header))
                }
                itemsIndexed(
                    items = state.upcoming,
                    key = { _, item -> "upcoming-${item.transaction.id}" },
                ) { _, item ->
                    SubscriptionRow(
                        item = item,
                        currencyFormat = currencyFormat,
                        muted = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SubscriptionsCalendarSection(
    monthStart: LocalDate,
    chargesByDay: Map<LocalDate, List<Transaction>>,
    modifier: Modifier = Modifier,
) {
    val daysInMonth = monthStart.lengthOfMonth()
    // dayOfWeek.value is Monday=1..Sunday=7; shift so Sunday lands at column 0, matching the
    // Sun-first header row below.
    val leadingBlanks = monthStart.dayOfWeek.value % 7
    val weeks = (leadingBlanks + daysInMonth + 6) / 7
    val today = remember { LocalDate.now() }
    val weekdayLabels = remember {
        (0..6).map { offset ->
            DayOfWeek.of(((DayOfWeek.SUNDAY.value - 1 + offset) % 7) + 1)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .take(3)
        }
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmallCondensed,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        for (week in 0 until weeks) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                for (dayOfWeekColumn in 0 until 7) {
                    val cellIndex = week * 7 + dayOfWeekColumn
                    val dayNumber = cellIndex - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                    ) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = monthStart.withDayOfMonth(dayNumber)
                            CalendarDayCell(
                                day = dayNumber,
                                charges = chargesByDay[date].orEmpty(),
                                isToday = date == today,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    charges: List<Transaction>,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                if (isToday) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                RoundedCornerShape(10.dp),
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmallCondensed,
            color = if (isToday) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (charges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                charges.take(2).forEach { transaction ->
                    val (background, _) = avatarColorRoles[avatarColorIndexFor(transaction.id)].colors()
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(background, CircleShape),
                    )
                }
                if (charges.size > 2) {
                    Text(
                        text = "+${charges.size - 2}",
                        style = MaterialTheme.typography.labelSmallCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsHeroCard(
    monthlyTotal: BigDecimal,
    activeCount: Int,
    daysUntilNextCharge: Long?,
    currencyFormatted: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.subscriptions_monthly_commitment_label),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = currencyFormatted,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.subscriptions_per_month_suffix),
                    style = MaterialTheme.typography.bodyMediumCondensed,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (daysUntilNextCharge != null) {
                    stringResource(
                        R.string.subscriptions_hero_subtitle_with_next,
                        activeCount,
                        relativeDateText(daysUntilNextCharge),
                    )
                } else {
                    stringResource(R.string.subscriptions_hero_subtitle_no_upcoming, activeCount)
                },
                style = MaterialTheme.typography.bodyMediumCondensed,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun relativeDateText(daysUntil: Long): String = when {
    daysUntil <= 0L -> stringResource(R.string.upcoming_recurrent_today)
    daysUntil == 1L -> stringResource(R.string.upcoming_recurrent_tomorrow)
    daysUntil < 7L -> stringResource(R.string.upcoming_recurrent_in_days, daysUntil)
    else -> stringResource(R.string.upcoming_recurrent_in_weeks, daysUntil / 7)
}

@Composable
private fun frequencyText(frequency: RecurrentFrequency?): String = when (frequency) {
    RecurrentFrequency.WEEKLY -> stringResource(R.string.recurrent_frequency_weekly)
    RecurrentFrequency.BIWEEKLY -> stringResource(R.string.recurrent_frequency_biweekly)
    RecurrentFrequency.MONTHLY -> stringResource(R.string.recurrent_frequency_monthly)
    null -> ""
}

private val avatarColorRoles = listOf(
    AvatarColors { MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer },
    AvatarColors { MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer },
    AvatarColors { MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer },
)

private fun interface AvatarColors {
    @Composable
    fun colors(): Pair<Color, Color>
}

private fun avatarColorIndexFor(transactionId: Long): Int =
    Math.floorMod(transactionId, avatarColorRoles.size.toLong()).toInt()

@Composable
private fun SubscriptionAvatar(
    label: String,
    transactionId: Long,
    modifier: Modifier = Modifier,
    backgroundOverride: Color? = null,
    contentColorOverride: Color? = null,
) {
    val (background, content) = avatarColorRoles[avatarColorIndexFor(transactionId)].colors()
    Box(
        modifier = modifier
            .size(36.dp)
            .background(backgroundOverride ?: background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMediumCondensed,
            fontWeight = FontWeight.Bold,
            color = contentColorOverride ?: content,
        )
    }
}

@Composable
private fun SubscriptionRow(
    item: UpcomingRecurrentItem,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val alpha = if (muted) 0.6f else 1f
    val today = remember { LocalDate.now() }
    val daysUntil = remember(item.nextChargeDate) { ChronoUnit.DAYS.between(today, item.nextChargeDate) }
    val name = item.transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        SubscriptionAvatar(
            label = name.first().uppercaseChar().toString(),
            transactionId = item.transaction.id,
            modifier = Modifier.alpha(alpha),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMediumCondensed,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                text = "${frequencyText(item.transaction.recurrentFrequency)} · ${relativeDateText(daysUntil)}",
                style = MaterialTheme.typography.bodyMediumCondensed,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        Text(
            text = currencyFormat.format(item.transaction.amount),
            style = MaterialTheme.typography.titleSmallEmphasized,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
    }
}

@Composable
private fun DueTodayCard(
    item: UpcomingRecurrentItem,
    currencyFormat: NumberFormat,
    onConfirmPaid: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = item.transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorNotGood.copy(alpha = 0.16f)),
        border = BorderStroke(1.dp, colorNotGood.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubscriptionAvatar(
                    label = name.first().uppercaseChar().toString(),
                    transactionId = item.transaction.id,
                    backgroundOverride = colorNotGood.copy(alpha = 0.3f),
                    contentColorOverride = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMediumCondensed,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = frequencyText(item.transaction.recurrentFrequency),
                        style = MaterialTheme.typography.bodyMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = currencyFormat.format(item.transaction.amount),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.subscriptions_due_today_label),
                style = MaterialTheme.typography.labelMediumCondensed,
                fontWeight = FontWeight.Bold,
                color = colorNotGood,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onConfirmPaid) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.subscriptions_mark_as_paid),
                        style = MaterialTheme.typography.labelMediumCondensed,
                    )
                }
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.subscriptions_skip_this_cycle),
                        style = MaterialTheme.typography.labelMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.EventRepeat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.subscriptions_empty_title),
            style = MaterialTheme.typography.titleMediumCondensed,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.subscriptions_empty_body),
            style = MaterialTheme.typography.bodyMediumCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun SubscriptionsPreview() {
    val today = LocalDate.now()
    val dueTodayItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("16.99"),
            comment = "Netflix",
            date = today.minusMonths(2).atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today,
        isInCurrentPeriod = true,
    )
    val soonItem = dueTodayItem.copy(
        transaction = dueTodayItem.transaction.copy(id = 2L, comment = "Spotify", amount = BigDecimal("9.99"), recurrentFrequency = RecurrentFrequency.WEEKLY),
        nextChargeDate = today.plusDays(2),
    )
    val upcomingItem = dueTodayItem.copy(
        transaction = dueTodayItem.transaction.copy(id = 3L, comment = "Gym membership", amount = BigDecimal("49.99")),
        nextChargeDate = today.plusDays(12),
    )
    val monthStart = today.withDayOfMonth(1)
    MinusTheme {
        Subscriptions(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueTodayItem, soonItem),
                upcoming = listOf(upcomingItem),
                monthlyTotal = BigDecimal("76.97"),
                activeCount = 3,
                currencyCode = "USD",
                daysUntilNextCharge = 0L,
                calendarMonthStart = monthStart,
                chargesByDay = mapOf(
                    today to listOf(dueTodayItem.transaction),
                    today.plusDays(2) to listOf(soonItem.transaction),
                    today.plusDays(12) to listOf(upcomingItem.transaction, soonItem.transaction),
                ),
            ),
        )
    }
}
