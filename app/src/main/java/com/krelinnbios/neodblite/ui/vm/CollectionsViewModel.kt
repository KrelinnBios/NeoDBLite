package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.Collection
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.friendlyMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CollectionsContent(
    val created: List<Collection>,
    val liked: List<Collection>
)

/** 我的合集列表。 */
class CollectionsViewModel : ViewModel() {
    private val repo = App.container.repository

    private val _state = MutableStateFlow<UiState<CollectionsContent>>(UiState.Loading)
    val state: StateFlow<UiState<CollectionsContent>> = _state.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private var userHandle = ""
    private var createdPage = 1
    private var createdPages = 1
    private var likedPage = 1
    private var likedPages = 1
    private val created = mutableListOf<Collection>()
    private val liked = mutableListOf<Collection>()

    fun load(handle: String) {
        userHandle = handle
        createdPage = 1
        createdPages = 1
        likedPage = 1
        likedPages = 1
        created.clear()
        liked.clear()
        _loadingMore.value = false
        _state.value = UiState.Loading
        viewModelScope.launch {
            val createdRequest = async { repo.myCollections(1) }
            val likedRequest = async { repo.likedCollections(handle, 1) }
            val createdResult = createdRequest.await()
            val likedResult = likedRequest.await()
            val createdPageData = createdResult.getOrNull()
            val likedPageData = likedResult.getOrNull()

            if (createdPageData == null && likedPageData == null) {
                val error = createdResult.exceptionOrNull() ?: likedResult.exceptionOrNull()
                _state.value = UiState.Error(error?.friendlyMessage() ?: "网络异常，请稍后重试")
                return@launch
            }

            createdPageData?.let {
                createdPages = it.pages
                created.addAll(it.data)
            }
            likedPageData?.let {
                likedPages = it.pages
                liked.addAll(it.data)
            }
            publish()
        }
    }

    fun loadMore(showLiked: Boolean) {
        if (_loadingMore.value) return
        val currentPage = if (showLiked) likedPage else createdPage
        val totalPages = if (showLiked) likedPages else createdPages
        if (currentPage >= totalPages) return
        _loadingMore.value = true
        viewModelScope.launch {
            try {
                val result = if (showLiked) {
                    repo.likedCollections(userHandle, currentPage + 1)
                } else {
                    repo.myCollections(currentPage + 1)
                }
                result.onSuccess {
                    if (showLiked) {
                        likedPage += 1
                        likedPages = it.pages
                        liked.addAll(it.data)
                    } else {
                        createdPage += 1
                        createdPages = it.pages
                        created.addAll(it.data)
                    }
                }
                publish()
            } finally {
                _loadingMore.value = false
            }
        }
    }

    private fun publish() {
        _state.value = UiState.Success(
            CollectionsContent(created = created.toList(), liked = liked.toList())
        )
    }
}
