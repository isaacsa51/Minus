package com.serranoie.app.minus.presentation.island

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.island.sdk.BriefCardPayload
import com.sameerasw.island.sdk.HostInfo
import com.sameerasw.island.sdk.IslandHostHandle
import com.sameerasw.island.sdk.IslandPayload
import com.sameerasw.island.sdk.IslandSurfaceKey
import com.sameerasw.island.sdk.IslandTone
import com.sameerasw.island.sdk.compose.ComposeIslandProviderService
import com.sameerasw.island.sdk.compose.islandSnapshot
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.TotalBudgetCard
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import com.serranoie.app.minus.presentation.util.font.format.formatLocalized
import com.serranoie.app.minus.presentation.util.font.format.toDate
import com.serranoie.app.minus.presentation.widget.exceededLabelRes
import com.serranoie.app.minus.presentation.widget.periodLabelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Island provider for Essentials: a budget card in the island Brief, plus the page it opens.
 * Essentials binds this only after the user approves Minus in its Island settings.
 */
class MinusIslandService : ComposeIslandProviderService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var publishing: Job? = null
    private val pageKey = IslandSurfaceKey.brief(CARD_KEY)

    override fun onAttached(host: IslandHostHandle, info: HostInfo) {
        publishing?.cancel()
        publishing = scope.launch {
            IslandBudgetStore.observe(applicationContext).collectLatest {
                publish(host, it)
                if (it == null) return@collectLatest
                while (true) {
                    val now = LocalDateTime.now()
                    delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1000)
                    publish(host, it)
                }
            }
        }
    }

    override fun onHostDetached() {
        publishing?.cancel()
        super.onHostDetached()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun hasExpandedContent(itemKey: String) = itemKey == pageKey

    /** The Brief row is drawn by Essentials, so it mirrors the budget pill: split remaining, then the period. */
    private fun publish(host: IslandHostHandle, snapshot: IslandBudgetSnapshot?) {
        val card = snapshot?.let { s ->
            val range = "${s.startDate.formatLocalized(DATE_PATTERN)} → ${s.finishDate.formatLocalized(DATE_PATTERN)}"
            if (s.date != LocalDate.now()) {
                BriefCardPayload(
                    key = CARD_KEY,
                    title = money(s.periodLeft, s.currency),
                    subtitle = getString(R.string.island_brief_stale) + "\n" + range,
                    iconId = ICON,
                )
            } else {
                BriefCardPayload(
                    key = CARD_KEY,
                    title = money(s.splitRemaining, s.currency),
                    subtitle = getString(splitLabel(s)) + "\n" + range,
                    iconId = ICON,
                    tone = if (s.isAlert) IslandTone.ALERT else IslandTone.ACCENT,
                    trailingText = getString(R.string.island_brief_days_short, s.daysLeft),
                    pageHeightDp = PAGE_HEIGHT_DP,
                )
            }
        }
        host.publish(IslandPayload(briefCards = listOfNotNull(card)), mapOf(ICON to icon()))
    }

    private fun icon() = ContextCompat.getDrawable(this, R.drawable.ic_notification)!!.toBitmap(ICON_PX, ICON_PX)

    @Composable
    override fun ExpandedContent(itemKey: String) {
        val snapshot by IslandBudgetStore.observe(applicationContext).collectAsState()
        val s = snapshot ?: return
        val info = hostInfoState.value
        val density = LocalDensity.current
        fun px(value: Int?) = with(density) { (value ?: 0).toDp() }
        val sidePadding = px((info?.outsetPx ?: 0) + (info?.expandedPaddingPx ?: 0))

        MinusTheme(darkTheme = false) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .onSizeChanged { host?.resize(itemKey, it.height) }
                    .islandSnapshot { host?.snapshot(itemKey, it) }
                    .clip(RoundedCornerShape(px(info?.cornerPx)))
                    .background(Color.Black)
                    .clickable { host?.openApp(itemKey) },
            ) {
                Column(Modifier.padding(start = sidePadding, end = sidePadding, bottom = sidePadding)) {
                    Spacer(Modifier.height(px(info?.cameraClearancePx)))
                    TotalBudgetCard(
                        budget = s.settingsTotalBudget,
                        budgetState = BudgetState.EMPTY.copy(totalBudget = s.totalBudget),
                        budgetSettings = BudgetSettings(
                            totalBudget = s.settingsTotalBudget,
                            period = s.viewPeriod,
                            startDate = s.startDate,
                            currencyCode = s.currency,
                            rollOverLimit = s.rollOverLimit,
                        ),
                        currencyCode = s.currency,
                        startDate = s.startDate.toDate(),
                        finishDate = s.finishDate.toDate(),
                        creditOwed = s.creditOwed,
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(splitLabel(s)),
                            value = money(s.splitRemaining, s.currency),
                            colors = if (s.isAlert) {
                                colors.copy(contentColor = MaterialTheme.colorScheme.errorContainer)
                            } else {
                                colors
                            },
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.island_page_period_left),
                            value = money(s.periodLeft, s.currency),
                            colors = colors,
                        )
                    }
                }
            }
        }
    }

    private fun splitLabel(s: IslandBudgetSnapshot): Int = when {
        s.isOverBudget -> R.string.budget_pill_over_budget
        s.isOverSplit -> s.viewPeriod.exceededLabelRes()
        else -> s.viewPeriod.periodLabelRes()
    }

    private fun money(value: BigDecimal, currency: String): String =
        if (value < BigDecimal.ZERO) "−" + formatCurrencySymbolOnly(value.negate(), currency) else formatCurrencySymbolOnly(value, currency)

    private companion object {
        const val CARD_KEY = "budget"
        const val ICON = "minus"
        const val ICON_PX = 96
        const val PAGE_HEIGHT_DP = 240
        const val DATE_PATTERN = "dd MMM"
    }
}
