package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.NeoUser
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repo = App.container.repository

    data class ProfileStats(
        val counts: Map<ShelfType, Int>,
        val recent: List<ItemBrief>,
        val bio: String?
    )

    private val _state = MutableStateFlow<UiState<ProfileStats>>(UiState.Loading)
    val state: StateFlow<UiState<ProfileStats>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var boundUser: NeoUser? = null
    private var boundHost: String = ""

    fun bind(user: NeoUser, host: String) {
        val changed = boundUser != user || boundHost != host
        boundUser = user
        boundHost = host
        if (changed || _state.value is UiState.Loading) load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch { fetch() }
    }

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            fetch(keepOnError = true)
            _refreshing.value = false
        }
    }

    private suspend fun fetch(keepOnError: Boolean = false) {
        val user = boundUser
        val host = boundHost
        val bio = user?.let { currentUser ->
            val fromPage = if (host.isNotBlank()) {
                repo.profileBio(currentUser, host).getOrNull()
            } else null
            fromPage?.takeIf { it.isNotBlank() } ?: currentUser.bioText
        }

        val counts = linkedMapOf<ShelfType, Int>()
        var recent: List<ItemBrief> = emptyList()
        var lastError: Throwable? = null

        for (type in ShelfType.entries) {
            repo.shelf(type, null, 1)
                .onSuccess { paged ->
                    counts[type] = paged.count
                    if (type == ShelfType.COMPLETE) {
                        recent = paged.data.mapNotNull { it.item }.take(12)
                    }
                }
                .onFailure { lastError = it }
        }

        if (counts.isEmpty() && lastError != null) {
            if (!keepOnError || _state.value !is UiState.Success) {
                _state.value = UiState.Error(lastError!!.friendlyMessage())
            }
        } else {
            _state.value = UiState.Success(ProfileStats(counts, recent, bio))
        }
    }
}
