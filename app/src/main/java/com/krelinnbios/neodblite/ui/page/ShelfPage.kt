package com.krelinnbios.neodblite.ui.page

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Tag
import com.krelinnbios.neodblite.data.model.Visibility
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.AdaptiveTabRow
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.ItemRow
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.component.MarkDraft
import com.krelinnbios.neodblite.ui.component.MarkEditor
import com.krelinnbios.neodblite.ui.component.MarkRow
import com.krelinnbios.neodblite.ui.component.ShelfCalendar
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.vm.ShelfViewModel
import com.krelinnbios.neodblite.ui.vm.TagItemsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfPage(
    shelfVM: ShelfViewModel,
    onOpenItem: (ItemBrief) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val tagItemsVM: TagItemsViewModel = viewModel()

    val shelfType by shelfVM.shelfType.collectAsState()
    val category by shelfVM.category.collectAsState()
    val state by shelfVM.state.collectAsState()
    val loadingMore by shelfVM.loadingMore.collectAsState()
    val refreshing by shelfVM.refreshing.collectAsState()
    val toast by shelfVM.toast.collectAsState()
    val userTags by shelfVM.userTags.collectAsState()
    val tagsLoadFailed by shelfVM.tagsLoadFailed.collectAsState()

    var showCalendar by remember { mutableStateOf(false) }
    var selectedDay by remember(shelfType, category) { mutableStateOf<String?>(null) }
    // 选中标签后进入「标签模式」，展示该标签下的全部条目；切换书架状态会退出标签模式。
    var selectedTag by remember(shelfType) { mutableStateOf<Tag?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember(shelfType, category, selectedTag) { mutableStateOf("") }
    var editingMark by remember { mutableStateOf<MarkSchema?>(null) }

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            shelfVM.consumeToast()
        }
    }
    LaunchedEffect(selectedTag) {
        selectedTag?.uuid?.let { tagItemsVM.loadOnce(it) }
    }

    val shelves = ShelfType.entries

    Column(modifier = Modifier.fillMaxSize()) {
        // 自适应页签：标签短就等宽铺满，长（英文/日文）才横向滚动。
        AdaptiveTabRow(
            tabs = shelves,
            selectedIndex = shelves.indexOf(shelfType),
            label = { strings.shelfLabel(it, category) },
            onSelect = { shelfVM.selectShelf(it) }
        )

        // 工具栏：四个按钮均匀分布，顺序为 日历 / 类型 / 标签 / 搜索。
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showCalendar = !showCalendar }) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = if (showCalendar) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                var catExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { catExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.Category,
                        contentDescription = null,
                        tint = if (category != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false },
                    // widthIn 在外、width(IntrinsicSize.Max) 在内：面板贴合最宽条目文本，同时不超上限。
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    CompactMenuItem(
                        label = strings.all,
                        color = if (category == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        onClick = { shelfVM.selectCategory(null); catExpanded = false }
                    )
                    Category.entries.forEach { cat ->
                        CompactMenuItem(
                            label = strings.categoryLabel(cat),
                            color = if (cat == category) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            onClick = { shelfVM.selectCategory(cat); catExpanded = false }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                var tagsExpanded by remember { mutableStateOf(false) }
                // 打开下拉时顺带重新拉取，避免启动时一次静默失败导致列表一直为空。
                IconButton(onClick = { shelfVM.loadTags(); tagsExpanded = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = if (selectedTag != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = tagsExpanded,
                    onDismissRequest = { tagsExpanded = false },
                    // widthIn 在外、width(IntrinsicSize.Max) 在内：面板贴合最宽条目文本，同时不超上限。
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    CompactMenuItem(
                        label = strings.all,
                        color = if (selectedTag == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        onClick = { selectedTag = null; tagsExpanded = false }
                    )
                    if (userTags.isEmpty()) {
                        CompactMenuItem(
                            label = if (tagsLoadFailed) strings.networkError else strings.noContent,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            onClick = {
                                if (tagsLoadFailed) shelfVM.loadTags() else tagsExpanded = false
                            }
                        )
                    }
                    userTags.forEach { tag ->
                        CompactMenuItem(
                            label = tag.bestTitle,
                            color = if (tag.uuid == selectedTag?.uuid) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            onClick = { selectedTag = tag; tagsExpanded = false }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = if (showSearch) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.clearInput)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val query = searchQuery.trim()
            if (selectedTag != null) {
                TagItemsContent(
                    tagItemsVM = tagItemsVM,
                    query = query,
                    onOpenItem = onOpenItem,
                    strings = strings
                )
            } else {
                when (val s = state) {
                    is UiState.Loading -> LoadingBox()
                    is UiState.Error -> ErrorBox(s.message, onRetry = { shelfVM.reload() })
                    is UiState.Success -> {
                        val allMarks = s.data
                        val displayed = allMarks
                            .let { list ->
                                val selectedDate = selectedDay
                                if (selectedDate != null) {
                                    list.filter { it.createdTime?.startsWith(selectedDate) == true }
                                } else list
                            }
                            .let { list ->
                                if (query.isNotBlank()) {
                                    list.filter { it.item?.bestTitle?.contains(query, ignoreCase = true) == true }
                                } else list
                            }

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (showCalendar) {
                                ShelfCalendar(
                                    marks = allMarks,
                                    selectedDay = selectedDay,
                                    onSelectDay = { selectedDay = it }
                                )
                            }

                            if (displayed.isEmpty()) {
                                EmptyBox(strings.noContent)
                            } else {
                                val listState = rememberLazyListState()
                                val shouldLoadMore by remember {
                                    derivedStateOf {
                                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                        val total = listState.layoutInfo.totalItemsCount
                                        total > 0 && last >= total - 3
                                    }
                                }
                                LaunchedEffect(shouldLoadMore) {
                                    if (shouldLoadMore) shelfVM.loadMore()
                                }

                                PullToRefreshBox(
                                    isRefreshing = refreshing,
                                    onRefresh = { shelfVM.refresh() },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                        items(displayed) { mark ->
                                            val item = mark.item
                                            if (item != null) {
                                                val editAction: (() -> Unit)? = if (item.uuid.isNullOrBlank()) {
                                                    null
                                                } else {
                                                    { editingMark = mark }
                                                }
                                                MarkRow(
                                                    mark = mark,
                                                    onClick = { onOpenItem(item) },
                                                    onEdit = editAction
                                                )
                                            }
                                        }
                                        if (loadingMore) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        strings.loadingMore,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingMark?.let { mark ->
        val item = mark.item
        val uuid = item?.uuid
        if (item != null && !uuid.isNullOrBlank()) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { editingMark = null },
                sheetState = sheetState
            ) {
                MarkEditor(
                    category = Category.fromApi(item.category ?: item.type),
                    existing = MarkDraft(
                        shelf = ShelfType.fromApi(mark.shelfType) ?: shelfType,
                        grade = mark.ratingGrade ?: 0,
                        comment = mark.commentText.orEmpty(),
                        visibility = Visibility.fromApi(mark.visibility),
                        tags = mark.tags,
                        shareToFediverse = false
                    ),
                    hasExisting = true,
                    onSave = { draft ->
                        shelfVM.saveMark(
                            uuid,
                            MarkInRequest(
                                shelfType = draft.shelf.apiValue,
                                visibility = draft.visibility.apiValue,
                                commentText = draft.comment.ifBlank { null },
                                ratingGrade = draft.grade.takeIf { it > 0 },
                                tags = draft.tags,
                                postToFediverse = draft.shareToFediverse
                            )
                        )
                        editingMark = null
                    },
                    onDelete = {
                        shelfVM.deleteMark(uuid)
                        editingMark = null
                    }
                )
            }
        }
    }
}

@Composable
private fun TagItemsContent(
    tagItemsVM: TagItemsViewModel,
    query: String,
    onOpenItem: (ItemBrief) -> Unit,
    strings: com.krelinnbios.neodblite.ui.i18n.AppStrings
) {
    val state by tagItemsVM.state.collectAsState()
    val loadingMore by tagItemsVM.loadingMore.collectAsState()

    when (val s = state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(s.message)
        is UiState.Success -> {
            val displayed = if (query.isNotBlank()) {
                s.data.filter { it.bestTitle.contains(query, ignoreCase = true) }
            } else s.data

            if (displayed.isEmpty()) {
                EmptyBox(strings.noContent)
            } else {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = listState.layoutInfo.totalItemsCount
                        total > 0 && last >= total - 3
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) tagItemsVM.loadMore()
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(displayed) { item ->
                        ItemRow(item = item, onClick = { onOpenItem(item) })
                    }
                    if (loadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(strings.loadingMore, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 下拉菜单的紧凑条目：宽度跟随文本内容，避开 DropdownMenuItem 自带的 112dp 最小宽度，
 * 配合菜单上的 width(IntrinsicSize.Max) 让面板贴合最宽条目。
 */
@Composable
private fun CompactMenuItem(
    label: String,
    color: Color,
    onClick: () -> Unit,
    style: TextStyle? = null
) {
    Text(
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = style ?: MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
