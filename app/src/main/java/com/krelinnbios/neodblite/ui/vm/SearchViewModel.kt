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

class SearchViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** null 表示全部类目。 */
    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<ItemBrief>>?>(null)
    val state: StateFlow<UiState<List<ItemBrief>>?> = _state.asStateFlow()

    private var page = 1
    private var pages = 1
    private val accumulated = mutableListOf<ItemBrief>()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** 下拉刷新：重跑当前关键词第一页，保留旧结果直到成功。 */
    fun refresh() {
        val q = _query.value.trim()
        if (q.isBlank() || _refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            repo.search(q, _category.value, 1)
                .onSuccess {
                    page = 1
                    pages = it.pages
                    accumulated.clear()
                    accumulated.addAll(it.data)
                    _state.value = UiState.Success(accumulated.toList())
                }
                .onFailure { if (_state.value !is UiState.Success) _state.value = UiState.Error(it.friendlyMessage()) }
            _refreshing.value = false
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun selectCategory(category: Category?) {
        if (_category.value == category) return
        _category.value = category
        if (_query.value.isNotBlank()) submit()
    }

    fun submit() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        page = 1
        accumulated.clear()
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.search(q, _category.value, page)
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
        val q = _query.value.trim().ifBlank { return }
        _loadingMore.value = true
        viewModelScope.launch {
            repo.search(q, _category.value, page + 1)
                .onSuccess {
                    page += 1
                    pages = it.pages
                    accumulated.addAll(it.data)
                    _state.value = UiState.Success(accumulated.toList())
                }
            _loadingMore.value = false
        }
    }

    val canLoadMore: Boolean get() = page < pages
}
