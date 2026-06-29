package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 个人主页数据：各书架数量统计 + 最近「看过」的条目预览。仅用 /api/me/shelf 的 count 与首页数据，不臆造端点。 */
class ProfileViewModel : ViewModel() {
    private val repo = App.container.repository

    data class ProfileStats(
        val counts: Map<ShelfType, Int>,
        val recent: List<ItemBrief>
    )

    private val _state = MutableStateFlow<UiState<ProfileStats>>(UiState.Loading)
    val state: StateFlow<UiState<ProfileStats>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        load()
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
        val counts = linkedMapOf<ShelfType, Int>()
        var recent: List<ItemBrief> = emptyList()
        var lastError: Throwable? = null

        for (type in listOf(ShelfType.WISHLIST, ShelfType.PROGRESS, ShelfType.COMPLETE)) {
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
            _state.value = UiState.Success(ProfileStats(counts, recent))
        }
    }
}
