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
    private val historyStore = App.container.searchHistory

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _history = MutableStateFlow(historyStore.load())
    val history: StateFlow<List<String>> = _history.asStateFlow()

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
        if (value.isBlank()) {
            page = 1
            pages = 1
            accumulated.clear()
            _state.value = null
        }
    }

    fun selectCategory(category: Category?) {
        if (_category.value == category) return
        _category.value = category
        if (_query.value.isNotBlank()) submit()
    }

    /** 点击历史项：填入并立即搜索。 */
    fun searchFor(query: String) {
        _query.value = query
        submit()
    }

    fun removeHistory(query: String) {
        _history.value = historyStore.remove(query)
    }

    fun clearHistory() {
        historyStore.clear()
        _history.value = emptyList()
    }

    fun submit() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        _history.value = historyStore.add(q)
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
