package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.TagItem
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 某个标签下的全部条目（分页）。 */
class TagItemsViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _state = MutableStateFlow<UiState<List<MarkSchema>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MarkSchema>>> = _state.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _loadingAll = MutableStateFlow(false)
    val loadingAll: StateFlow<Boolean> = _loadingAll.asStateFlow()

    private var loadedUuid: String? = null
    private var page = 1
    private var pages = 1
    private val accumulated = mutableListOf<MarkSchema>()

    fun loadOnce(uuid: String) {
        if (loadedUuid == uuid && _state.value is UiState.Success) return
        loadedUuid = uuid
        load(uuid)
    }

    fun load(uuid: String) {
        loadedUuid = uuid
        page = 1
        accumulated.clear()
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.tagItems(uuid, page)
                .onSuccess {
                    pages = it.pages
                    accumulated.addAll(loadMarks(it.data))
                    _state.value = UiState.Success(accumulated.toList())
                }
                .onFailure { _state.value = UiState.Error(it.friendlyMessage()) }
        }
    }

    fun loadMore() {
        val uuid = loadedUuid ?: return
        if (_loadingMore.value || _loadingAll.value || page >= pages) return
        _loadingMore.value = true
        viewModelScope.launch {
            repo.tagItems(uuid, page + 1)
                .onSuccess {
                    page += 1
                    pages = it.pages
                    accumulated.addAll(loadMarks(it.data))
                    _state.value = UiState.Success(accumulated.toList())
                }
            _loadingMore.value = false
        }
    }

    /** 搜索时需要覆盖尚未滚动到的分页，否则本地过滤会漏掉标签下的条目。 */
    fun loadAll() {
        val uuid = loadedUuid ?: return
        if (_loadingMore.value || _loadingAll.value || page >= pages) return
        _loadingAll.value = true
        viewModelScope.launch {
            try {
                while (page < pages) {
                    val nextPage = page + 1
                    val result = repo.tagItems(uuid, nextPage)
                    if (result.isFailure) break
                    val response = result.getOrThrow()
                    page = nextPage
                    pages = response.pages
                    accumulated.addAll(loadMarks(response.data))
                    _state.value = UiState.Success(accumulated.toList())
                }
            } finally {
                _loadingAll.value = false
            }
        }
    }

    /** 标签接口只返回条目；补取个人标记后与普通书架共用完整行样式。 */
    private suspend fun loadMarks(members: List<TagItem>): List<MarkSchema> = coroutineScope {
        members.mapNotNull { it.item }.map { item ->
            async {
                val uuid = item.uuid?.takeIf { it.isNotBlank() }
                val mark = uuid?.let { repo.mark(it).getOrNull() }
                mark?.copy(item = mark.item ?: item) ?: MarkSchema(item = item)
            }
        }.awaitAll()
    }
}
