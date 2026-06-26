package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogBottomSheet
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogHistoryScreen
import com.serranoie.app.minus.presentation.ui.changelog.components.ChangelogItemCard
import com.serranoie.app.minus.presentation.ui.changelog.components.ChangelogSectionHeader
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class ChangelogScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun changelogSectionHeaderNewFeatures() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogSectionHeader(title = "New Features", type = ReleaseType.FEATURE)
            }
        }
    }

    @Test
    fun changelogSectionHeaderImprovements() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogSectionHeader(title = "Improvements", type = ReleaseType.IMPROVEMENT)
            }
        }
    }

    @Test
    fun changelogSectionHeaderBugFixes() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogSectionHeader(title = "Bug Fixes", type = ReleaseType.BUG_FIX)
            }
        }
    }

    @Test
    fun changelogItemCardFeature() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogItemCard(
                    modifier = Modifier.fillMaxSize(),
                    item = ChangelogItem(
                        title = "Deep Insights v2",
                        description = "Predictive AI analysis for recurring subscriptions with enhanced 99% accuracy model.",
                        type = ReleaseType.FEATURE,
                    ),
                )
            }
        }
    }

    @Test
    fun changelogItemCardImprovement() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogItemCard(
                    modifier = Modifier.fillMaxSize(),
                    item = ChangelogItem(
                        title = "Launch Optimization",
                        description = "Overall application bootstrap speed improved by 25% through binary optimization.",
                        type = ReleaseType.IMPROVEMENT,
                    ),
                )
            }
        }
    }

    @Test
    fun changelogItemCardBugFix() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogItemCard(
                    modifier = Modifier.fillMaxSize(),
                    item = ChangelogItem(
                        title = "Launcher icon fix",
                        description = "Added the missing launcher icon into the app so it shows up correctly in the system app drawer.",
                        type = ReleaseType.BUG_FIX,
                    ),
                )
            }
        }
    }

    @Test
    fun changelogHistoryScreen() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogHistoryScreen(
                    releases = sampleReleases,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun changelogHistoryScreenDarkTheme() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme(darkTheme = true) {
                ChangelogHistoryScreen(
                    releases = sampleReleases,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun changelogHistoryScreenSpanish() {
        Locale.setDefault(Locale("es", "ES"))
        paparazzi.snapshot {
            MinusTheme {
                ChangelogHistoryScreen(
                    releases = sampleReleases,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun changelogHistoryScreenFrench() {
        Locale.setDefault(Locale("fr", "FR"))
        paparazzi.snapshot {
            MinusTheme {
                ChangelogHistoryScreen(
                    releases = sampleReleases,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun changelogHistoryScreenEmpty() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogHistoryScreen(
                    releases = emptyList(),
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun changelogBottomSheetFeature() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ChangelogBottomSheet(
                    release = sampleReleases.first(),
                    onDismiss = {},
                )
            }
        }
    }

    private val sampleReleases = listOf(
        VersionRelease(
            versionCode = 100101,
            versionName = "1.1.1",
            releaseDate = "2026-06-17",
            items = listOf(
                ChangelogItem(
                    title = "Launcher icon fix",
                    description = "Added the missing launcher icon into the app so it shows up correctly in the system app drawer.",
                    type = ReleaseType.BUG_FIX,
                ),
            ),
        ),
        VersionRelease(
            versionCode = 100000,
            versionName = "1.0.0",
            releaseDate = "2026-05-18",
            items = listOf(
                ChangelogItem(
                    title = "Calculator-style expense entry",
                    description = "Type amounts with the calculator-style numpad, including operators and live result preview.",
                    type = ReleaseType.FEATURE,
                ),
                ChangelogItem(
                    title = "Budget tracking",
                    description = "Set daily, weekly or monthly budgets and watch the progress meter react as you spend.",
                    type = ReleaseType.FEATURE,
                ),
                ChangelogItem(
                    title = "GitHub release automation",
                    description = "Tagged builds now publish a GitHub release automatically through Fastlane.",
                    type = ReleaseType.IMPROVEMENT,
                ),
            ),
        ),
    )
}
