package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.Collection
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 我的合集列表。 */
class CollectionsViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _state = MutableStateFlow<UiState<List<Collection>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Collection>>> = _state.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private var page = 1
    private var pages = 1
    private val accumulated = mutableListOf<Collection>()

    init {
        load()
    }

    fun load() {
        page = 1
        accumulated.clear()
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.myCollections(page)
                .onSuccess {
                    pages = it.pages
                    accumulated.addAll(it.data)
                    _state.value = UiState.Success(accumulated.toList())
                }
                .onFailure { _state.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun loadMore() {
        if (_loadingMore.value || page >= pages) return
        _loadingMore.value = true
        viewModelScope.launch {
            repo.myCollections(page + 1)
                .onSuccess {
                    page += 1
                    pages = it.pages
                    accumulated.addAll(it.data)
                    _state.value = UiState.Success(accumulated.toList())
                }
            _loadingMore.value = false
        }
    }
}
