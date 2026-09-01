package com.serranoie.app.minus.presentation.ui.settings.bugreport

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportIssueType
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiEffect
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiIntent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BugReportViewModelTest {

    private val zipGenerator: BugReportZipGenerator = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = BugReportViewModel(zipGenerator)

    private fun BugReportViewModel.fillSubmittableBug() {
        onIntent(BugReportUiIntent.ChangeTitle("Crash on save"))
        onIntent(BugReportUiIntent.ChangeDescription("It crashes every time"))
        onIntent(BugReportUiIntent.ChangeStep(0, "Tap save"))
    }

    @Test
    fun `selecting a bug report shows the reproduction steps`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.SelectIssueType(BugReportIssueType.BugReport))
            val state = expectMostRecentItem()
            assertThat(state.selectedIssueType).isEqualTo(BugReportIssueType.BugReport)
            assertThat(state.showReproductionSteps).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a feature request hides the reproduction steps`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.SelectIssueType(BugReportIssueType.FeatureRequest))
            val state = expectMostRecentItem()
            assertThat(state.selectedIssueType).isEqualTo(BugReportIssueType.FeatureRequest)
            assertThat(state.showReproductionSteps).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `text fields update the state`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.ChangeTitle("t"))
            vm.onIntent(BugReportUiIntent.ChangeDescription("d"))
            vm.onIntent(BugReportUiIntent.ChangeProposedSolution("s"))
            vm.onIntent(BugReportUiIntent.ChangeAdditionalInfo("i"))
            val state = expectMostRecentItem()
            assertThat(state.title).isEqualTo("t")
            assertThat(state.description).isEqualTo("d")
            assertThat(state.proposedSolution).isEqualTo("s")
            assertThat(state.additionalInfo).isEqualTo("i")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding steps appends rows with incrementing ids starting after the initial three`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.AddStep)
            vm.onIntent(BugReportUiIntent.AddStep)
            val steps = expectMostRecentItem().reproductionSteps
            assertThat(steps).hasSize(5)
            assertThat(steps.map { it.id }).containsExactly(0L, 1L, 2L, 3L, 4L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing a step out of bounds is a no-op`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.ChangeStep(index = 99, value = "nope"))
            val steps = expectMostRecentItem().reproductionSteps
            assertThat(steps.all { it.value.isEmpty() }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removing a step while more than one exists hides it rather than dropping it`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.RemoveStep(0))
            val steps = expectMostRecentItem().reproductionSteps
            assertThat(steps).hasSize(3)
            assertThat(steps[0].visible).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `finishing a hidden step's exit animation drops it from the list`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.RemoveStep(0)) // hides step id 0
            vm.onIntent(BugReportUiIntent.FinishStepExit(stepId = 0L))
            val steps = expectMostRecentItem().reproductionSteps
            assertThat(steps.map { it.id }).containsExactly(1L, 2L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `attachments are de-duplicated on selection and removable`() = runTest {
        val a = mockk<Uri>()
        val b = mockk<Uri>()
        val vm = newViewModel()
        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.SelectAttachments(listOf(a, b)))
            vm.onIntent(BugReportUiIntent.SelectAttachments(listOf(a)))
            assertThat(expectMostRecentItem().selectedAttachmentUris).containsExactly(a, b).inOrder()

            vm.onIntent(BugReportUiIntent.RemoveAttachment(a))
            assertThat(expectMostRecentItem().selectedAttachmentUris).containsExactly(b)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submitting an incomplete report does nothing`() = runTest {
        val vm = newViewModel()
        vm.effects.test {
            vm.onIntent(BugReportUiIntent.SubmitReport) // title/description empty

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a successful bug submission emits an email composer effect with a bug-tagged title`() = runTest {
        val reportUri = mockk<Uri>()
        coEvery { zipGenerator.generate(any()) } returns GeneratedBugReport(
            uri = reportUri,
            fileName = "minus-bug.zip",
            markdown = "# Bug report",
        )
        val vm = newViewModel()
        vm.fillSubmittableBug()

        vm.effects.test {
            vm.onIntent(BugReportUiIntent.SubmitReport)

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(BugReportUiEffect.OpenEmailComposer::class.java)
            effect as BugReportUiEffect.OpenEmailComposer
            assertThat(effect.uri).isEqualTo(reportUri)
            assertThat(effect.fileName).isEqualTo("minus-bug.zip")
            assertThat(effect.title).isEqualTo("[Bug]: Crash on save")
            assertThat(effect.body).isEqualTo("# Bug report")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a successful feature-request submission tags the title with Feature`() = runTest {
        coEvery { zipGenerator.generate(any()) } returns GeneratedBugReport(
            uri = mockk(),
            fileName = "minus-feature.zip",
            markdown = "# Feature",
        )
        val vm = newViewModel()
        vm.onIntent(BugReportUiIntent.SelectIssueType(BugReportIssueType.FeatureRequest))
        vm.onIntent(BugReportUiIntent.ChangeTitle("Dark mode toggle"))
        vm.onIntent(BugReportUiIntent.ChangeDescription("Please add it"))
        vm.onIntent(BugReportUiIntent.ChangeProposedSolution("A switch in settings"))

        vm.effects.test {
            vm.onIntent(BugReportUiIntent.SubmitReport)

            val effect = awaitItem() as BugReportUiEffect.OpenEmailComposer
            assertThat(effect.title).isEqualTo("[Feature]: Dark mode toggle")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failing report generation emits a ShowError effect`() = runTest {
        coEvery { zipGenerator.generate(any()) } throws IllegalStateException("disk full")
        val vm = newViewModel()
        vm.fillSubmittableBug()

        vm.effects.test {
            vm.onIntent(BugReportUiIntent.SubmitReport)

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(BugReportUiEffect.ShowError::class.java)
            assertThat((effect as BugReportUiEffect.ShowError).message).isEqualTo("disk full")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the generating flag is cleared again after a submission finishes`() = runTest {
        coEvery { zipGenerator.generate(any()) } returns GeneratedBugReport(mockk(), "f.zip", "md")
        val vm = newViewModel()
        vm.fillSubmittableBug()

        vm.uiState.test {
            vm.onIntent(BugReportUiIntent.SubmitReport)
            assertThat(expectMostRecentItem().isGeneratingReport).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
