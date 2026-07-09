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
                type = ReleaseType.FEATURE,
            ),
            ChangelogItem(
                title = "Critical bug fix",
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

    private fun setGateContent(viewModel: ChangelogGateViewModel) {
        composeTestRule.setContent {
            MinusTheme {
                ChangelogGate(
                    currentVersionCode = testRelease.versionCode,
                    viewModel = viewModel,
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    @Test
    fun when_upgrade_detected_then_bottom_sheet_is_displayed() = runTest {
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Show(testRelease)
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()


        composeTestRule.onAllNodesWithText(releaseHeaderText()).onFirst().assertIsDisplayed()

        composeTestRule.onAllNodesWithText("Sample upgrade feature").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Critical bug fix").assertCountEquals(1)
    }

    @Test
    fun when_same_version_as_last_seen_then_no_bottom_sheet() = runTest {
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
    fun when_first_install_bootstrap_then_bottom_sheet_is_displayed() = runTest {
        // After the lastSeen-null-as-zero fix: first ever launch of a
        // changelog-equipped build (or any upgrade from a pre-gate build
        // whose DataStore key was never seeded) now resolves to Show, not
        // Skip. The evaluator returns Show directly here to cover that path
        // at the ViewModel/UI boundary.
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Show(testRelease)
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        composeTestRule.onAllNodesWithText(releaseHeaderText()).onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sample upgrade feature").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Critical bug fix").assertCountEquals(1)
    }

    @Test
    fun when_user_dismisses_sheet_then_content_disappears() = runTest {
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Show(testRelease)
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        composeTestRule.onAllNodesWithText(releaseHeaderText()).onFirst().assertIsDisplayed()

        viewModel.dismissSheet()
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        composeTestRule.onAllNodesWithText(releaseHeaderText()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Sample upgrade feature").assertCountEquals(0)
    }

    @Test
    fun when_viewmodel_changes_pending_release_after_render_then_sheet_appears() = runTest {
        val mockEvaluator = mockk<ChangelogTriggerEvaluator>()
        coEvery { mockEvaluator.invoke(any()) } returns ChangelogDecision.Skip
        val viewModel = ChangelogGateViewModel(mockEvaluator)

        setGateContent(viewModel)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        composeTestRule.onAllNodesWithText(releaseHeaderText()).assertCountEquals(0)

        coEvery { mockEvaluator.invoke(testRelease.versionCode) } returns ChangelogDecision.Show(testRelease)
        viewModel.evaluate(testRelease.versionCode)
        composeTestRule.waitForIdle()
        advanceUntilIdle()

        composeTestRule.onAllNodesWithText(releaseHeaderText()).onFirst().assertIsDisplayed()
    }
}