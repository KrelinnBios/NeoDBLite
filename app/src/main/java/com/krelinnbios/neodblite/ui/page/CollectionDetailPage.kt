package com.krelinnbios.neodblite.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Collection
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.CoverImage
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.ItemRow
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.vm.CollectionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailPage(
    uuid: String,
    detailVM: CollectionDetailViewModel,
    onBack: () -> Unit,
    onOpenItem: (ItemBrief) -> Unit
) {
    val strings = LocalAppStrings.current
    val collection by detailVM.collection.collectAsState()
    val state by detailVM.state.collectAsState()
    val loadingMore by detailVM.loadingMore.collectAsState()

    LaunchedEffect(uuid) { detailVM.loadOnce(uuid) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = collection?.bestTitle ?: strings.collections,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = { detailVM.load(uuid) })
                is UiState.Success -> {
                    val listState = rememberLazyListState()
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = listState.layoutInfo.totalItemsCount
                            total > 0 && last >= total - 3
                        }
                    }
                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) detailVM.loadMore()
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        collection?.let { c ->
                            item { CollectionHeader(c) }
                        }
                        if (s.data.isEmpty()) {
                            item { Box(Modifier.fillMaxWidth().height(160.dp)) { EmptyBox(strings.noContent) } }
                        } else {
                            items(s.data) { member ->
                                val item = member.item
                                if (item != null) {
                                    ItemRow(item = item, onClick = { onOpenItem(item) })
                                }
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
    }
}

@Composable
private fun CollectionHeader(collection: Collection) {
    val strings = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row {
            CoverImage(
                url = collection.coverImageUrl,
                modifier = Modifier.width(96.dp).height(96.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.bestTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                collection.itemCount?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$it ${strings.itemsCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        collection.brief?.takeIf { it.isNotBlank() }?.let { brief ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = brief,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
