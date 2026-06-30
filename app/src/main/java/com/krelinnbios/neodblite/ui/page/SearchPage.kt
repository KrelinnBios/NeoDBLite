package com.krelinnbios.neodblite.ui.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.ItemRow
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.component.QuickMarkSheet
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.vm.SearchViewModel

/**
 * 独立搜索界面：顶部为返回键 + 搜索框（含范围选择与清空叉号，回车提交搜索），
 * 未搜索时下方展示历史搜索记录，搜索后展示结果列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    searchVM: SearchViewModel,
    onOpenItem: (ItemBrief) -> Unit
) {
    val strings = LocalAppStrings.current
    val query by searchVM.query.collectAsState()
    val searchCategory by searchVM.category.collectAsState()
    val searchState by searchVM.state.collectAsState()
    val loadingMore by searchVM.loadingMore.collectAsState()
    val refreshing by searchVM.refreshing.collectAsState()
    val history by searchVM.history.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    var quickMarkItem by remember { mutableStateOf<ItemBrief?>(null) }

    val focusRequester = remember { FocusRequester() }
    // 进入界面且尚无关键词时自动聚焦并弹出键盘。
    LaunchedEffect(Unit) {
        if (query.isBlank()) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchField(
                query = query,
                category = searchCategory,
                focusRequester = focusRequester,
                onQueryChange = searchVM::onQueryChange,
                onCategoryChange = searchVM::selectCategory,
                onClear = { searchVM.onQueryChange(""); focusRequester.requestFocus() },
                onSubmit = {
                    keyboard?.hide()
                    searchVM.submit()
                },
                modifier = Modifier.weight(1f)
            )
        }

        // state 非空表示已发起搜索（加载/结果/错误），否则展示历史记录。
        if (searchState != null) {
            SearchResultContent(
                state = searchState,
                refreshing = refreshing,
                loadingMore = loadingMore,
                onRefresh = searchVM::refresh,
                onRetry = searchVM::submit,
                onLoadMore = searchVM::loadMore,
                onOpenItem = onOpenItem,
                onQuickMark = { quickMarkItem = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            RecentSearches(
                history = history,
                onPick = {
                    keyboard?.hide()
                    searchVM.searchFor(it)
                },
                onRemove = searchVM::removeHistory,
                onClearAll = searchVM::clearHistory,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    quickMarkItem?.let { item ->
        QuickMarkSheet(item = item, onDismiss = { quickMarkItem = null })
    }
}

@Composable
private fun SearchField(
    query: String,
    category: Category?,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalAppStrings.current
    val label = category?.let { strings.categoryLabel(it) } ?: strings.all

    Surface(
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clickable { expanded = true }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = strings.searchScope,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .widthIn(max = 220.dp)
                ) {
                    SearchCategoryItem(
                        label = strings.all,
                        selected = category == null,
                        onClick = {
                            expanded = false
                            onCategoryChange(null)
                        }
                    )
                    Category.entries.forEach { cat ->
                        SearchCategoryItem(
                            label = strings.categoryLabel(cat),
                            selected = cat == category,
                            onClick = {
                                expanded = false
                                onCategoryChange(cat)
                            }
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isBlank()) {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .focusRequester(focusRequester)
            )
            // 搜索图标无用（回车即搜），统一改成清空叉号；无内容时占位隐藏。
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.clearInput,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(
    history: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    if (history.isEmpty()) {
        EmptyBox(strings.searchToStart, modifier = modifier)
        return
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.recentSearches,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClearAll) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = strings.clearInput,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(history) { q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(q) }
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = q,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
                    )
                    IconButton(onClick = { onRemove(q) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.clearInput,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCategoryItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultContent(
    state: UiState<List<ItemBrief>>?,
    refreshing: Boolean,
    loadingMore: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenItem: (ItemBrief) -> Unit,
    onQuickMark: (ItemBrief) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (state) {
            null -> EmptyBox(LocalAppStrings.current.searchToStart)
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(state.message, onRetry = onRetry)
            is UiState.Success -> {
                val results = state.data
                if (results.isEmpty()) {
                    EmptyBox(LocalAppStrings.current.noSearchResults)
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
                        if (shouldLoadMore) onLoadMore()
                    }

                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(results) { item ->
                                ItemRow(
                                    item = item,
                                    onClick = { onOpenItem(item) },
                                    onLongClick = { onQuickMark(item) }
                                )
                            }
                            if (loadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(LocalAppStrings.current.loadingMore, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
