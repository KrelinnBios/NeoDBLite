package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _category = MutableStateFlow(Category.BOOK)
    val category: StateFlow<Category> = _category.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<ItemBrief>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ItemBrief>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        load(Category.BOOK)
    }

    /** 下拉刷新：保留当前列表，仅在成功后整体替换；失败不清空已有内容。 */
    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            repo.trending(_category.value)
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { if (_state.value !is UiState.Success) _state.value = UiState.Error(it.friendlyMessage()) }
            _refreshing.value = false
        }
    }

    fun selectCategory(category: Category) {
        if (_category.value == category && _state.value is UiState.Success) return
        _category.value = category
        load(category)
    }

    fun load(category: Category = _category.value) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.trending(category)
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.friendlyMessage()) }
        }
    }
}
