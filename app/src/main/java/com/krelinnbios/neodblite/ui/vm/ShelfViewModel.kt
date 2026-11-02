package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.category
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.PagedTagItems
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Tag
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.global.MarkEventBus
import com.krelinnbios.neodblite.ui.friendlyMessage
import com.krelinnbios.neodblite.ui.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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

    /** 滚动加载/全量加载共用一个任务句柄：搜索时需能取消在途的滚动加载并由全量拉取接管。 */
    private var extendJob: Job? = null

    /** 基础列表被整体替换（如下拉刷新）时自增，供页面在搜索态重新触发全量拉取。 */
    private val _dataEpoch = MutableStateFlow(0)
    val dataEpoch: StateFlow<Int> = _dataEpoch.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _loadingAll = MutableStateFlow(false)
    val loadingAll: StateFlow<Boolean> = _loadingAll.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    data class TagMenuState(val tags: List<Tag> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

    private val _userTags = MutableStateFlow(TagMenuState())
    private var tagProfileUrl: String? = null
    val userTags = _userTags.asStateFlow()

    private val _categoryCounts = MutableStateFlow<Map<Category, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<Category, Int>> = _categoryCounts.asStateFlow()

    private var tagsJob: Job? = null
    private val tagCategoryCounts = mutableMapOf<String, Map<Category, Int>>()

    init {
        reload()
        loadCategoryCounts()
    }

    fun closeTagMenu() {
        tagsJob?.cancel()
        tagsJob = null
        tagProfileUrl = null
    }

    fun invalidateTagCounts() {
        tagCategoryCounts.clear()
        if (tagsJob != null) loadTags()
    }

    fun loadTags(profileUrl: String? = tagProfileUrl) {
        tagsJob?.cancel()
        tagProfileUrl = profileUrl
        val category = _category.value
        _userTags.value = TagMenuState()
        tagsJob = viewModelScope.launch {
            try {
                val pageCounts = async {
                    profileUrl?.let {
                        withTimeoutOrNull(12_000) { repo.tagPageCounts(it, category).getOrNull() }
                    }
                }
                val tags = mutableListOf<Tag>()
                var page = 1
                do {
                    val result = repo.myTags(page).getOrThrow()
                    tags.addAll(result.data)
                    page++
                } while (page <= result.pages)

                val sorted = tags.filter { !it.uuid.isNullOrBlank() && it.bestTitle.isNotBlank() }
                    .sortedBy { it.bestTitle }
                if (category == null) _userTags.value = TagMenuState(sorted)
                val counts = pageCounts.await()
                val visible = mutableMapOf<String, Tag>()
                val pending = mutableListOf<Tag>()
                for (tag in sorted) {
                    val count = counts?.get(tag.bestTitle)
                    if (count != null) {
                        visible[tag.bestTitle] = tag.copy(itemCount = count)
                    } else if (counts == null || tag.visibility != 0 || category == null) {
                        pending.add(tag)
                        if (category == null) visible[tag.bestTitle] = tag
                    }
                }

                var failed = false
                fun publish(loading: Boolean) {
                    _userTags.value = TagMenuState(sorted.mapNotNull { visible[it.bestTitle] }, loading, failed)
                }
                publish(pending.isNotEmpty())
                // 网页已返回的标签立即可点。私密标签或不支持分类页的实例，逐个补齐并展示。
                val limit = Semaphore(4)
                coroutineScope {
                    pending.forEach { tag ->
                        launch {
                            limit.withPermit {
                                try {
                                    tagWithCount(tag, category)?.let { visible[tag.bestTitle] = it }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    failed = true
                                }
                                publish(true)
                            }
                        }
                    }
                }
                publish(false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _userTags.value = _userTags.value.copy(loading = false, failed = true)
            }
        }
    }

    private suspend fun tagWithCount(tag: Tag, category: Category?): Tag? {
        val uuid = requireNotNull(tag.uuid)
        if (category == null) {
            val count = repo.tagItems(uuid, 1).getOrNull()?.count ?: tag.itemCount
            return tag.copy(itemCount = count)
        }
        val counts = tagCategoryCounts[uuid] ?: countTagCategories { page ->
            repo.tagItems(uuid, page).getOrThrow()
        }.also { tagCategoryCounts[uuid] = it }
        return counts[category]?.let { tag.copy(itemCount = it) }
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
        extendJob?.cancel()
        _loadingMore.value = false
        _loadingAll.value = false
        invalidateTagCounts()
        loadCategoryCounts()
        viewModelScope.launch {
            repo.shelf(_shelfType.value, _category.value, 1)
                .onSuccess {
                    page = 1
                    pages = it.pages
                    accumulated.clear()
                    accumulated.addAll(it.data)
                    _state.value = UiState.Success(accumulated.toList())
                    _dataEpoch.value += 1
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
        extendJob?.cancel()
        _loadingMore.value = false
        _loadingAll.value = false
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
        if (_loadingMore.value || _loadingAll.value || page >= pages) return
        _loadingMore.value = true
        extendJob = viewModelScope.launch {
            try {
                repo.shelf(_shelfType.value, _category.value, page + 1)
                    .onSuccess {
                        page += 1
                        pages = it.pages
                        accumulated.addAll(it.data)
                        _state.value = UiState.Success(accumulated.toList())
                    }
            } finally {
                _loadingMore.value = false
            }
        }
    }

    /**
     * 搜索时需要覆盖尚未滚动到的分页，否则本地过滤会漏掉早期收藏。
     * 剩余分页一次性并发请求（网络层按主机排队限流），总耗时≈最慢一页而非逐页累加；
     * 每页按序合并后立即更新列表，命中结果随加载逐步出现。
     */
    fun loadAll() {
        if (_loadingAll.value || page >= pages) return
        // 滚动加载在途时取消它并由本次接管：其结果尚未提交，不会产生缺口。
        extendJob?.cancel()
        _loadingMore.value = false
        _loadingAll.value = true
        val type = _shelfType.value
        val category = _category.value
        extendJob = viewModelScope.launch {
            try {
                val firstNew = page + 1
                val deferreds = (firstNew..pages).map { p ->
                    async { p to repo.shelf(type, category, p) }
                }
                for (deferred in deferreds) {
                    if (_shelfType.value != type || _category.value != category) return@launch
                    val (p, result) = deferred.await()
                    val response = result.getOrElse {
                        // 中途某页失败：保留已到部分，不越过缺失页（后续可重新触发补拉）。
                        _toast.value = it.friendlyMessage()
                        return@launch
                    }
                    page = p
                    pages = response.pages
                    accumulated.addAll(response.data)
                    _state.value = UiState.Success(accumulated.toList())
                }
            } finally {
                _loadingAll.value = false
            }
        }
    }

    fun saveMark(uuid: String, request: MarkInRequest, onSuccess: (() -> Unit)? = null) {
        if (uuid.isBlank()) return
        viewModelScope.launch {
            repo.postMark(uuid, request)
                .onSuccess {
                    _toast.value = "已保存"
                    reload()
                    invalidateTagCounts()
                    loadCategoryCounts()
                    MarkEventBus.markDirty()
                    onSuccess?.invoke()
                }
                .onFailure { _toast.value = it.friendlyMessage() }
        }
    }

    fun deleteMark(uuid: String, onSuccess: (() -> Unit)? = null) {
        if (uuid.isBlank()) return
        viewModelScope.launch {
            repo.deleteMark(uuid)
                .onSuccess {
                    _toast.value = "已删除标记"
                    reload()
                    invalidateTagCounts()
                    loadCategoryCounts()
                    MarkEventBus.markDirty()
                    onSuccess?.invoke()
                }
                .onFailure { _toast.value = it.friendlyMessage() }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    val canLoadMore: Boolean get() = page < pages
}

/** 只保存各类型的数量，整页条目用完即可释放。 */
internal suspend fun countTagCategories(loadPage: suspend (Int) -> PagedTagItems): Map<Category, Int> {
    val counts = mutableMapOf<Category, Int>()
    var page = 1
    do {
        val result = loadPage(page)
        for (member in result.data) {
            val category = member.category ?: continue
            counts[category] = counts.getOrDefault(category, 0) + 1
        }
        page++
    } while (page <= result.pages)
    return counts
}
