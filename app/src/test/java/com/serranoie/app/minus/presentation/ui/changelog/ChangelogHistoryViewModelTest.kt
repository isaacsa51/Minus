package com.serranoie.app.minus.presentation.ui.changelog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.ChangelogRepository
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import io.mockk.every
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
class ChangelogHistoryViewModelTest {

    private val changelogRepository: ChangelogRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `releases emits the repository's release list once collected`() = runTest {
        val releases = listOf(mockk<VersionRelease>(), mockk<VersionRelease>())
        every { changelogRepository.getAllReleases() } returns releases

        ChangelogHistoryViewModel(changelogRepository).releases.test {
            assertThat(expectMostRecentItem()).isEqualTo(releases)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `releases starts from an empty list`() = runTest {
        every { changelogRepository.getAllReleases() } returns emptyList()

        val vm = ChangelogHistoryViewModel(changelogRepository)

        assertThat(vm.releases.value).isEmpty()
    }
}
