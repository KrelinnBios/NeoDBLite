package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Tag
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.global.MarkEventBus
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _tagsLoadFailed = MutableStateFlow(false)
    val tagsLoadFailed: StateFlow<Boolean> = _tagsLoadFailed.asStateFlow()

    private val _categoryCounts = MutableStateFlow<Map<Category, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<Category, Int>> = _categoryCounts.asStateFlow()

    private var loadingTags = false

    init {
        reload()
        loadCategoryCounts()
        loadTags()
    }

    /** 拉取当前账号的全部标签（用于标签筛选下拉）。失败时保留旧列表并置失败标记，供下拉提示与重试。 */
    fun loadTags() {
        if (loadingTags) return
        loadingTags = true
        viewModelScope.launch {
            val all = mutableListOf<Tag>()
            var p = 1
            var failed = false
            while (p <= 20) {
                val result = repo.myTags(p).getOrNull()
                if (result == null) {
                    failed = true
                    break
                }
                all.addAll(result.data)
                if (result.data.isEmpty() || p >= result.pages) break
                p++
            }
            if (!failed) {
                _userTags.value = all.filter { !it.uuid.isNullOrBlank() && it.bestTitle.isNotBlank() }
                    .sortedBy { it.bestTitle }
            }
            _tagsLoadFailed.value = failed
            loadingTags = false
        }
    }

    private fun loadCategoryCounts() {
        val type = _shelfType.value
        viewModelScope.launch {
            val counts = Category.entries
                .map { category ->
                    async { category to repo.shelf(type, category, 1).getOrNull()?.count }
                }
                .awaitAll()
                .mapNotNull { (category, count) -> count?.let { category to it } }
                .toMap()
            if (_shelfType.value == type) _categoryCounts.value = counts
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        loadTags()
        loadCategoryCounts()
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
        loadCategoryCounts()
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
                    loadTags()
                    loadCategoryCounts()
                    MarkEventBus.markDirty()
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
                    loadTags()
                    loadCategoryCounts()
                    MarkEventBus.markDirty()
                }
                .onFailure { _toast.value = it.friendlyMessage() }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    val canLoadMore: Boolean get() = page < pages
}
