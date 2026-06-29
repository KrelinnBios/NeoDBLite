package com.krelinnbios.neodblite.ui.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.krelinnbios.neodblite.ui.component.ItemGridCard
import com.krelinnbios.neodblite.ui.component.ItemRow
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.vm.DiscoverViewModel
import com.krelinnbios.neodblite.ui.vm.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverPage(
    discoverVM: DiscoverViewModel,
    searchVM: SearchViewModel,
    onOpenItem: (ItemBrief) -> Unit
) {
    val category by discoverVM.category.collectAsState()
    val state by discoverVM.state.collectAsState()
    val refreshing by discoverVM.refreshing.collectAsState()

    val query by searchVM.query.collectAsState()
    val searchCategory by searchVM.category.collectAsState()
    val searchState by searchVM.state.collectAsState()
    val loadingMore by searchVM.loadingMore.collectAsState()
    val searchRefreshing by searchVM.refreshing.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val isSearching = query.isNotBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            category = searchCategory,
            onQueryChange = searchVM::onQueryChange,
            onCategoryChange = searchVM::selectCategory,
            onSubmit = {
                keyboard?.hide()
                searchVM.submit()
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        if (isSearching) {
            SearchResultContent(
                state = searchState,
                refreshing = searchRefreshing,
                loadingMore = loadingMore,
                onRefresh = searchVM::refresh,
                onRetry = searchVM::submit,
                onLoadMore = searchVM::loadMore,
                onOpenItem = onOpenItem,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Category.entries) { cat ->
                    FilterChip(
                        selected = cat == category,
                        onClick = { discoverVM.selectCategory(cat) },
                        label = { Text(cat.label) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val s = state) {
                    is UiState.Loading -> LoadingBox()
                    is UiState.Error -> ErrorBox(s.message, onRetry = { discoverVM.load() })
                    is UiState.Success -> {
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { discoverVM.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (s.data.isEmpty()) {
                                EmptyBox("暂无内容")
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    gridItems(s.data) { item ->
                                        ItemGridCard(item = item, onClick = { onOpenItem(item) })
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

@Composable
private fun SearchBar(
    query: String,
    category: Category?,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = category?.label ?: "全部"

    Surface(
        modifier = modifier.fillMaxWidth().height(54.dp),
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
                        .height(46.dp)
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
                        contentDescription = "选择搜索范围",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SearchCategoryItem(
                        label = "全部",
                        selected = category == null,
                        onClick = {
                            expanded = false
                            onCategoryChange(null)
                        }
                    )
                    Category.entries.forEach { cat ->
                        SearchCategoryItem(
                            label = cat.label,
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
                    .height(28.dp)
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
                                text = "输入名字搜索",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            IconButton(onClick = onSubmit) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.primary
                )
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
        onClick = onClick
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
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (state) {
            null -> EmptyBox("按搜索键开始搜索")
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(state.message, onRetry = onRetry)
            is UiState.Success -> {
                val results = state.data
                if (results.isEmpty()) {
                    EmptyBox("没有找到相关条目")
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
                                ItemRow(item = item, onClick = { onOpenItem(item) })
                            }
                            if (loadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
