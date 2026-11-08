package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.category
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.PagedTagItems
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.TagItem
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.friendlyMessage
import com.krelinnbios.neodblite.ui.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
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
    private var category: Category? = null
    private var shelfType: ShelfType? = null
    private var page = 0
    private var pages = 1
    private val accumulated = mutableListOf<MarkSchema>()

    /** 滚动加载/全量加载共用一个任务句柄：搜索时需能取消在途的滚动加载并由全量拉取接管。 */
    private var extendJob: Job? = null

    fun loadOnce(uuid: String, category: Category?, shelfType: ShelfType? = null) {
        if (loadedUuid == uuid && this.category == category && this.shelfType == shelfType && _state.value is UiState.Success) return
        loadedUuid = uuid
        this.category = category
        this.shelfType = shelfType
        reload()
    }

    fun reload() {
        extendJob?.cancel()
        _loadingMore.value = false
        _loadingAll.value = false
        page = 0
        pages = 1
        accumulated.clear()
        _state.value = UiState.Loading
        loadMore()
    }

    fun loadMore() {
        val uuid = loadedUuid ?: return
        if (_loadingMore.value || _loadingAll.value || page >= pages) return
        _loadingMore.value = true
        val selectedCategory = category
        extendJob = viewModelScope.launch {
            try {
                // 当前页可能没有匹配项；继续翻页，找到当前类型或到达末页才更新列表。
                do {
                    val result = repo.tagItems(uuid, page + 1).getOrThrow()
                    val members = result.membersIn(selectedCategory)
                    val marks = loadMarks(members)
                    accumulated.addAll(marks)
                    page++
                    pages = result.pages
                } while (marks.isEmpty() && page < pages)
                _state.value = UiState.Success(accumulated.toList())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UiState.Error(e.friendlyMessage())
            } finally {
                if (isActive) _loadingMore.value = false
            }
        }
    }

    /**
     * 搜索时需要覆盖尚未滚动到的分页，否则本地过滤会漏掉标签下的条目。
     * 剩余分页一次性并发请求（网络层按主机排队限流），总耗时≈最慢一页而非逐页累加；
     * 每页按序合并后立即更新列表，命中结果随加载逐步出现。
     */
    fun loadAll() {
        val uuid = loadedUuid ?: return
        if (_state.value !is UiState.Success || _loadingAll.value || page >= pages) return
        // 滚动加载在途时取消它并由本次接管：其结果尚未提交，不会产生缺口。
        extendJob?.cancel()
        _loadingMore.value = false
        _loadingAll.value = true
        extendJob = viewModelScope.launch {
            try {
                val firstNew = page + 1
                val deferreds = (firstNew..pages).map { p ->
                    async { p to repo.tagItems(uuid, p) }
                }
                for (deferred in deferreds) {
                    if (loadedUuid != uuid) return@launch
                    val (p, result) = deferred.await()
                    val response = result.getOrElse {
                        _state.value = UiState.Error(it.friendlyMessage())
                        return@launch
                    }
                    val marks = loadMarks(response.membersIn(category))
                    page = p
                    pages = response.pages
                    accumulated.addAll(marks)
                    _state.value = UiState.Success(accumulated.toList())
                }
            } finally {
                if (isActive) _loadingAll.value = false
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
        }.awaitAll().filter { mark ->
            shelfType?.let { it.apiValue == mark.shelfType } ?: true
        }
    }
}

internal fun PagedTagItems.membersIn(category: Category?): List<TagItem> =
    if (category == null) data else data.filter { it.category == category }
