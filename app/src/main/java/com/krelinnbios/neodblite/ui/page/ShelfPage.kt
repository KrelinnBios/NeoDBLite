package com.krelinnbios.neodblite.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.component.MarkRow
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.vm.ShelfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfPage(
    shelfVM: ShelfViewModel,
    onOpenItem: (ItemBrief) -> Unit
) {
    val strings = LocalAppStrings.current
    val shelfType by shelfVM.shelfType.collectAsState()
    val category by shelfVM.category.collectAsState()
    val state by shelfVM.state.collectAsState()
    val loadingMore by shelfVM.loadingMore.collectAsState()
    val refreshing by shelfVM.refreshing.collectAsState()

    val shelves = ShelfType.entries

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = shelves.indexOf(shelfType)) {
            shelves.forEach { type ->
                Tab(
                    selected = type == shelfType,
                    onClick = { shelfVM.selectShelf(type) },
                    text = { Text(strings.shelfLabel(type, category)) }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = category == null,
                    onClick = { shelfVM.selectCategory(null) },
                    label = { Text(strings.all) }
                )
            }
            items(Category.entries) { cat ->
                FilterChip(
                    selected = cat == category,
                    onClick = { shelfVM.selectCategory(cat) },
                    label = { Text(strings.categoryLabel(cat)) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = { shelfVM.reload() })
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
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
                                items(s.data) { mark ->
                                    val item = mark.item
                                    if (item != null) {
                                        MarkRow(mark = mark, onClick = { onOpenItem(item) })
                                    }
                                }
                                if (loadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(strings.loadingMore, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
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
