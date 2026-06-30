package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 某个标签下的全部条目（分页）。 */
class TagItemsViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _state = MutableStateFlow<UiState<List<ItemBrief>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ItemBrief>>> = _state.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private var loadedUuid: String? = null
    private var page = 1
    private var pages = 1
    private val accumulated = mutableListOf<ItemBrief>()

    fun loadOnce(uuid: String) {
        if (loadedUuid == uuid && _state.value is UiState.Success) return
        loadedUuid = uuid
        load(uuid)
    }

    fun load(uuid: String) {
        page = 1
        accumulated.clear()
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.tagItems(uuid, page)
                .onSuccess {
                    pages = it.pages
                    accumulated.addAll(it.data.mapNotNull { member -> member.item })
                    _state.value = UiState.Success(accumulated.toList())
                }
                .onFailure { _state.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun loadMore() {
        val uuid = loadedUuid ?: return
        if (_loadingMore.value || page >= pages) return
        _loadingMore.value = true
        viewModelScope.launch {
            repo.tagItems(uuid, page + 1)
                .onSuccess {
                    page += 1
                    pages = it.pages
                    accumulated.addAll(it.data.mapNotNull { member -> member.item })
                    _state.value = UiState.Success(accumulated.toList())
                }
            _loadingMore.value = false
        }
    }
}
