package com.serranoie.app.minus.presentation.ui.e2e.history

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogHistoryScreen
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.settings.Settings
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test

class ChangelogE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

    private fun setSettingsContent(
        onNavigateToChangelog: () -> Unit = {},
        onNavigateToBugReport: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MinusTheme {
                Settings(
                    modifier = Modifier.fillMaxSize(),
                    isCensored = false,
                    currentTheme = "system",
                    currentTypography = "default",
                    isMaterialYouEnabled = false,
                    isCreditQuickToggleFeatureEnabled = false,
                    recurrentPaymentsViewMode = RecurrentPaymentsViewMode.VERTICAL_LIST,
                    notificationHour = 9,
                    notificationMinute = 0,
                    recurrentNotificationHour = 8,
                    recurrentNotificationMinute = 0,
                    exactAlarmEnabled = false,
                    onThemeChange = {},
                    onTypographyChange = {},
                    onMaterialYouToggle = {},
                    onCreditQuickToggleFeatureToggle = {},
                    onRecurrentPaymentsViewModeChange = {},
                    onNotificationTimeChange = { _, _ -> },
                    onRecurrentNotificationTimeChange = { _, _ -> },
                    onOpenExactAlarmSettings = {},
                    periodMappingMode = PeriodMappingMode.ACTIVE_BUDGET,
                    onPeriodMappingModeChange = {},
                    onExportCsv = {},
                    onImportCsv = {},
                    onResetTutorial = {},
                    onBugReportClick = onNavigateToBugReport,
                    onNavigateToChangelog = onNavigateToChangelog,
                    onBack = onBack,
                )
            }
        }
    }

    private fun setHistoryContent(
        releases: List<VersionRelease>,
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MinusTheme {
                ChangelogHistoryScreen(
                    releases = releases,
                    onBack = onBack,
                )
            }
        }
    }

    private fun whatsNewTitle(): String =
        composeTestRule.activity.getString(R.string.changelog_settings_item_title)

    private fun whatsNewSubtitle(): String =
        composeTestRule.activity.getString(R.string.changelog_settings_item_subtitle, "1.0.0")

    private fun releaseHeader(versionName: String, date: String): String =
        composeTestRule.activity.getString(R.string.changelog_release_header, versionName, date)

    private fun previousVersionsLabel(): String =
        composeTestRule.activity.getString(R.string.changelog_previous_versions)

    private fun closeContentDesc(): String =
        composeTestRule.activity.getString(R.string.changelog_close)

    @Test
    fun when_settings_is_displayed_then_what_is_new_item_is_visible() {
        setSettingsContent()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(whatsNewTitle()).onFirst()
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText(whatsNewSubtitle()).onFirst()
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun when_tapping_what_is_new_then_onNavigateToChangelog_callback_fires() {
        var navigatedCount = 0
        setSettingsContent(onNavigateToChangelog = { navigatedCount += 1 })
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(whatsNewTitle()).onFirst().performScrollTo()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(whatsNewTitle()).onFirst().performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(navigatedCount).isEqualTo(1)
    }

    @Test
    fun when_history_screen_is_displayed_then_version_headers_are_visible_in_order() {
        setHistoryContent(releases = sampleReleases)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(releaseHeader("1.1.1", "2026-06-17")).assertIsDisplayed()
        composeTestRule.onNodeWithText(releaseHeader("1.0.0", "2026-05-18")).assertIsDisplayed()
    }

    @Test
    fun when_history_screen_is_displayed_then_section_headers_are_visible_per_release() {
        setHistoryContent(releases = sampleReleases)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("New Features").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Improvements").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Bug Fixes").assertCountEquals(1)
    }

    @Test
    fun when_history_screen_is_displayed_then_previous_versions_divider_is_visible() {
        setHistoryContent(releases = sampleReleases)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(previousVersionsLabel()).assertCountEquals(1)
    }

    @Test
    fun when_history_screen_is_displayed_with_only_one_release_then_no_previous_versions_divider() {
        setHistoryContent(releases = listOf(sampleReleases.first()))
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(previousVersionsLabel()).assertCountEquals(0)
    }

    @Test
    fun when_history_screen_is_displayed_then_item_titles_are_visible() {
        setHistoryContent(releases = sampleReleases)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Calculator-style expense entry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Budget tracking").assertIsDisplayed()
        composeTestRule.onNodeWithText("GitHub release automation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Launcher icon fix").assertIsDisplayed()
    }

    @Test
    fun when_history_screen_is_displayed_then_card_versions_are_not_rendered_inside_cards() {
        setHistoryContent(releases = sampleReleases)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("v1.1.1").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("v1.0.0").assertCountEquals(0)
    }

    @Test
    fun when_history_screen_is_displayed_then_back_button_is_visible() {
        setHistoryContent(releases = sampleReleases)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(closeContentDesc()).assertIsDisplayed()
    }

    @Test
    fun when_history_screen_back_button_is_pressed_then_onBack_callback_fires() {
        var backCount = 0
        setHistoryContent(releases = sampleReleases, onBack = { backCount += 1 })
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(closeContentDesc()).performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(backCount).isEqualTo(1)
    }

    @Test
    fun when_history_screen_only_has_bug_fixes_then_other_sections_do_not_render() {
        val bugFixOnly = listOf(
            VersionRelease(
                versionCode = 100101,
                versionName = "1.1.1",
                releaseDate = "2026-06-17",
                items = listOf(
                    ChangelogItem(
                        title = "Multi-currency transfer date conversion",
                        description = "Fixed date conversion edge cases for transfers crossing midnight UTC.",
                        type = ReleaseType.BUG_FIX,
                    ),
                ),
            ),
        )
        setHistoryContent(releases = bugFixOnly)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bug Fixes").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("New Features").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Improvements").assertCountEquals(0)
        composeTestRule.onAllNodesWithText(previousVersionsLabel()).assertCountEquals(0)
    }

    @Test
    fun when_history_screen_is_displayed_with_empty_list_then_no_version_headers_render() {
        setHistoryContent(releases = emptyList())
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(releaseHeader("1.1.1", "2026-06-17"))
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithText("New Features").assertCountEquals(0)
        composeTestRule.onAllNodesWithText(previousVersionsLabel()).assertCountEquals(0)
    }

    @Test
    fun when_settings_is_displayed_then_what_is_new_is_first_in_app_information_section() {
        setSettingsContent()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(whatsNewTitle()).onFirst().performScrollTo()
        composeTestRule.onAllNodesWithText(
            composeTestRule.activity.getString(R.string.settings_about_title)
        ).onFirst().performScrollTo()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(whatsNewTitle()).assertCountEquals(1)
        composeTestRule.onAllNodesWithText(
            composeTestRule.activity.getString(R.string.settings_about_title)
        ).assertCountEquals(1)
    }
}
