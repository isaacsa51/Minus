package com.serranoie.app.minus.presentation.ui.changelog

import androidx.lifecycle.ViewModel
import com.serranoie.app.minus.data.repository.ChangelogRepository
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ChangelogHistoryViewModel @Inject constructor(
    changelogRepository: ChangelogRepository,
) : ViewModel() {

    private val _releases = MutableStateFlow<List<VersionRelease>>(emptyList())
    val releases: StateFlow<List<VersionRelease>> = _releases.asStateFlow()

    init {
        _releases.value = changelogRepository.getAllReleases()
    }
}
