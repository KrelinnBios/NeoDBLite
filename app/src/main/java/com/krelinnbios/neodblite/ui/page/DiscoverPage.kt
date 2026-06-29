package com.krelinnbios.neodblite.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.ItemGridCard
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.vm.DiscoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverPage(
    discoverVM: DiscoverViewModel,
    onOpenItem: (ItemBrief) -> Unit
) {
    val category by discoverVM.category.collectAsState()
    val state by discoverVM.state.collectAsState()
    val refreshing by discoverVM.refreshing.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
