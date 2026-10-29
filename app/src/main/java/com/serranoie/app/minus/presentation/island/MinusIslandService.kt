package com.serranoie.app.minus.presentation.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
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
import com.serranoie.app.minus.presentation.util.font.format.formatLocalized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
                    title = islandMoney(s.periodLeft, s.currency),
                    subtitle = getString(R.string.island_brief_stale) + "\n" + range,
                    iconId = ICON,
                )
            } else {
                BriefCardPayload(
                    key = CARD_KEY,
                    title = islandMoney(s.splitRemaining, s.currency),
                    subtitle = getString(islandSplitLabel(s)) + "\n" + range,
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
        IslandBudgetPage(
            snapshot = s,
            info = hostInfoState.value,
            onOpen = { host?.openApp(itemKey) },
            modifier = Modifier
                .onSizeChanged { host?.resize(itemKey, it.height) }
                .islandSnapshot { host?.snapshot(itemKey, it) },
        )
    }

    private companion object {
        const val CARD_KEY = "budget"
        const val ICON = "minus"
        const val ICON_PX = 96
        const val PAGE_HEIGHT_DP = 240
        const val DATE_PATTERN = "dd MMM"
    }
}
