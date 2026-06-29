package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.CommunityEntry
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _item = MutableStateFlow<UiState<ItemBrief>>(UiState.Loading)
    val item: StateFlow<UiState<ItemBrief>> = _item.asStateFlow()

    private val _mark = MutableStateFlow<MarkSchema?>(null)
    val mark: StateFlow<MarkSchema?> = _mark.asStateFlow()

    private val _community = MutableStateFlow<UiState<List<CommunityEntry>>?>(null)
    val community: StateFlow<UiState<List<CommunityEntry>>?> = _community.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private var loadedPath: String? = null

    fun loadOnce(path: String) {
        if (loadedPath == path && _item.value is UiState.Success) return
        loadedPath = path
        load(path)
    }

    fun load(path: String) {
        _item.value = UiState.Loading
        viewModelScope.launch {
            repo.item(path)
                .onSuccess { item ->
                    _item.value = UiState.Success(item)
                    item.uuid?.let { loadMark(it) }
                    loadCommunity(item)
                }
                .onFailure { _item.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    private fun loadMark(uuid: String) {
        viewModelScope.launch {
            repo.mark(uuid).onSuccess { _mark.value = it }
        }
    }
    private fun loadCommunity(item: ItemBrief) {
        _community.value = UiState.Loading
        viewModelScope.launch {
            repo.itemCommunity(item, App.container.authStore.cachedHost)
                .onSuccess { _community.value = UiState.Success(it) }
                .onFailure { _community.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun saveMark(uuid: String, request: MarkInRequest) {
        _saving.value = true
        viewModelScope.launch {
            repo.postMark(uuid, request)
                .onSuccess {
                    _toast.value = "已保存"
                    repo.mark(uuid).onSuccess { _mark.value = it }
                }
                .onFailure { _toast.value = it.friendlyMessage() }
            _saving.value = false
        }
    }

    fun deleteMark(uuid: String) {
        _saving.value = true
        viewModelScope.launch {
            repo.deleteMark(uuid)
                .onSuccess {
                    _mark.value = null
                    _toast.value = "已删除标记"
                }
                .onFailure { _toast.value = it.friendlyMessage() }
            _saving.value = false
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
