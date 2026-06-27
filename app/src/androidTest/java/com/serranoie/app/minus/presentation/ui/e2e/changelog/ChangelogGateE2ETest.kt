package com.serranoie.app.minus.presentation.ui.e2e.changelog

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.changelog.ChangelogDecision
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.domain.usecase.ChangelogTriggerEvaluator
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogGate
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogGateViewModel
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * E2E tests for the "What's New" bottom sheet trigger flow.
 *
 * The bottom sheet appears at app launch when [ChangelogTriggerEvaluator]
 * returns [ChangelogDecision.Show]. The trigger logic itself is covered
 * exhaustively by `ChangelogTriggerEvaluatorTest` (pure unit tests). These
 * tests focus on the UI gate — does the sheet actually render / dismiss
 * given the trigger's decision.
 *
 * Strategy: pass a real `ChangelogGateViewModel` with a mocked
 * `ChangelogTriggerEvaluator` directly (the gate accepts the ViewModel as a
 * parameter to keep it Hilt-free in tests). The mock controls whether the
 * trigger returns `Show` or `Skip`, which drives whether the sheet shows.
 *
 * Note on duplicate header text: the release header string ("Version X.Y.Z · …")
 * is rendered TWICE when the sheet is showing — once by `ChangelogBottomSheet`
 * itself, and once by the `ChangelogReleaseMeta` row inside the embedded
 * `ChangelogHistoryContent`. Tests that check the sheet is showing use
 * `onFirst()` for the header (intent is "sheet visible", not "header count").
 * Tests that check the sheet is NOT showing use `assertCountEquals(0)` (both
 * nodes must be gone). Item titles appear exactly once per item, so they
 * use the strict `assertCountEquals(1)`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangelogGateE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testRelease = VersionRelease(
        versionCode = 200,
        versionName = "2.0.0",
        releaseDate = "2026-06-25",
        items = listOf(
            ChangelogItem(
                title = "Sample upgrade feature",
                description = "What's new in this release.",
                type = ReleaseType.FEATURE,
            ),
            ChangelogItem(
                title = "Critical bug fix",
                description = "Fixes crash on startup.",
                type = ReleaseType.BUG_FIX,
            ),
        ),
    )

    private fun releaseHeaderText(): String =
        composeTestRule.activity.getString(
            R.string.changelog_release_header,
            testRelease.versionName,
            testRelease.releaseDate,
        )

    /**
     * Renders [ChangelogGate] with the given ViewModel. The composable
     * triggers `viewModel.evaluate(currentVersionCode)` from a
     * `LaunchedEffect`, so the mock setup decides whether the sheet shows.
     */
    private fun setGateContent(viewModel: ChangelogGateViewModel) {
        composeTestRule.setContent {
            MinusTheme {
                ChangelogGate(
                    currentVersionCode = testRelease.versionCode,
                    viewModel = viewModel,
                ) {
                    // Main content placeholder — the gate wraps whatever
                    // the app's real root composable would render.
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    @Test
    fun when_upgrade_detected_then_bottom_sheet_is_displayed() = runTest {
        // Scenario: user has the previous version installed (lastSeen < current).
        // On launch, the trigger returns Show(release) and the sheet appears.
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Show(testRelease)
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()  // let viewModelScope coroutine finish

        // Header appears twice (sheet + embedded history list) — check at
        // least one is visible.
        composeTestRule.onAllNodesWithText(releaseHeaderText()).onFirst().assertIsDisplayed()
        // Item titles appear exactly once each.
        composeTestRule.onAllNodesWithText("Sample upgrade feature").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Critical bug fix").assertCountEquals(1)
    }

    @Test
    fun when_same_version_as_last_seen_then_no_bottom_sheet() = runTest {
        // Scenario: app launched at the same version the user already saw.
        // Trigger returns Skip, no sheet.
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Skip
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        composeTestRule.onAllNodesWithText(releaseHeaderText()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Sample upgrade feature").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Critical bug fix").assertCountEquals(0)
    }

    @Test
    fun when_first_install_then_no_bottom_sheet() = runTest {
        // Scenario: brand-new install, DataStore has no lastSeen entry.
        // The trigger returns Skip on first install (the caller is expected
        // to seed lastSeen with the current versionCode separately so the
        // sheet doesn't fire on every fresh install).
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Skip
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        composeTestRule.onAllNodesWithText(releaseHeaderText()).assertCountEquals(0)
    }

    @Test
    fun when_user_dismisses_sheet_then_content_disappears() = runTest {
        // Scenario: sheet is showing, user taps close/dismiss. Sheet must
        // be removed from the composition (not just hidden visually).
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Show(testRelease)
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        // Confirm the sheet was rendered first
        composeTestRule.onAllNodesWithText(releaseHeaderText()).onFirst().assertIsDisplayed()

        // User dismisses
        viewModel.dismissSheet()
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        // Sheet content is gone
        composeTestRule.onAllNodesWithText(releaseHeaderText()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Sample upgrade feature").assertCountEquals(0)
    }

    @Test
    fun when_viewmodel_changes_pending_release_after_render_then_sheet_appears() = runTest {
        // Scenario: gate initially shows nothing, then the ViewModel decides
        // to show a release (e.g. via a deferred trigger or a debug action).
        // The StateFlow update must propagate to the UI.
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Skip
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        // Initially no sheet
        composeTestRule.onAllNodesWithText(releaseHeaderText()).assertCountEquals(0)

        // ViewModel decides to show the release (e.g. after a re-evaluation)
        coEvery { mockEvaluator.invoke(testRelease.versionCode) } returns ChangelogDecision.Show(testRelease)
        viewModel.evaluate(testRelease.versionCode)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        // Sheet now visible (use onFirst because the header text appears
        // twice — once from the sheet, once from the embedded history list).
        composeTestRule.onAllNodesWithText(releaseHeaderText()).onFirst().assertIsDisplayed()
    }
}