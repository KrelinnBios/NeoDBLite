package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Tag
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShelfViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _shelfType = MutableStateFlow(ShelfType.WISHLIST)
    val shelfType: StateFlow<ShelfType> = _shelfType.asStateFlow()

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<MarkSchema>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MarkSchema>>> = _state.asStateFlow()

    private var page = 1
    private var pages = 1
    private val accumulated = mutableListOf<MarkSchema>()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _userTags = MutableStateFlow<List<Tag>>(emptyList())
    val userTags: StateFlow<List<Tag>> = _userTags.asStateFlow()

    init {
        reload()
        loadTags()
    }

    /** 拉取当前账号的全部标签（用于标签筛选下拉）。 */
    fun loadTags() {
        viewModelScope.launch {
            val all = mutableListOf<Tag>()
            var p = 1
            while (p <= 20) {
                val result = repo.myTags(p).getOrNull() ?: break
                all.addAll(result.data)
                if (result.data.isEmpty() || p >= result.pages) break
                p++
            }
            _userTags.value = all.filter { !it.uuid.isNullOrBlank() && it.bestTitle.isNotBlank() }
                .sortedBy { it.bestTitle }
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            repo.shelf(_shelfType.value, _category.value, 1)
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

    fun selectShelf(type: ShelfType) {
        if (_shelfType.value == type) return
        _shelfType.value = type
        reload()
    }

    fun selectCategory(category: Category?) {
        if (_category.value == category) return
        _category.value = category
        reload()
    }

    fun reload() {
        page = 1
        accumulated.clear()
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.shelf(_shelfType.value, _category.value, page)
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
            repo.shelf(_shelfType.value, _category.value, page + 1)
                .onSuccess {
                    page += 1
                    pages = it.pages
                    accumulated.addAll(it.data)
                    _state.value = UiState.Success(accumulated.toList())
                }
            _loadingMore.value = false
        }
    }

    fun saveMark(uuid: String, request: MarkInRequest) {
        if (uuid.isBlank()) return
        viewModelScope.launch {
            repo.postMark(uuid, request)
                .onSuccess {
                    _toast.value = "已保存"
                    reload()
                }
                .onFailure { _toast.value = it.friendlyMessage() }
        }
    }

    fun deleteMark(uuid: String) {
        if (uuid.isBlank()) return
        viewModelScope.launch {
            repo.deleteMark(uuid)
                .onSuccess {
                    _toast.value = "已删除标记"
                    reload()
                }
                .onFailure { _toast.value = it.friendlyMessage() }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    val canLoadMore: Boolean get() = page < pages
}
