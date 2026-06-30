package com.krelinnbios.neodblite.ui.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.ItemGridCard
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.component.QuickMarkSheet
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.vm.DiscoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverPage(
    discoverVM: DiscoverViewModel,
    onOpenSearch: () -> Unit,
    onOpenItem: (ItemBrief) -> Unit
) {
    val strings = LocalAppStrings.current
    val category by discoverVM.category.collectAsState()
    val state by discoverVM.state.collectAsState()
    val refreshing by discoverVM.refreshing.collectAsState()
    var quickMarkItem by remember { mutableStateOf<ItemBrief?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 点击进入独立搜索界面；这里只是入口，不接收输入，故无搜索/清空图标。
        SearchEntry(
            onClick = onOpenSearch,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Category.entries) { cat ->
                FilterChip(
                    selected = cat == category,
                    onClick = { discoverVM.selectCategory(cat) },
                    label = { Text(strings.categoryLabel(cat)) }
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
                            EmptyBox(strings.noContent)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                gridItems(s.data) { item ->
                                    ItemGridCard(
                                        item = item,
                                        onClick = { onOpenItem(item) },
                                        onLongClick = { quickMarkItem = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    quickMarkItem?.let { item ->
        QuickMarkSheet(item = item, onDismiss = { quickMarkItem = null })
    }
}

/** 主页搜索入口：外观与搜索框一致，点击跳转到独立搜索界面。 */
@Composable
private fun SearchEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.searchPlaceholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
