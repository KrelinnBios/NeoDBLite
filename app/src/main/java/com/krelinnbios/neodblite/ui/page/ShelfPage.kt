package com.krelinnbios.neodblite.ui.page

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Visibility
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.component.MarkDraft
import com.krelinnbios.neodblite.ui.component.MarkEditor
import com.krelinnbios.neodblite.ui.component.MarkRow
import com.krelinnbios.neodblite.ui.component.ShelfCalendar
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.vm.ShelfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfPage(
    shelfVM: ShelfViewModel,
    onOpenItem: (ItemBrief) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val shelfType by shelfVM.shelfType.collectAsState()
    val category by shelfVM.category.collectAsState()
    val state by shelfVM.state.collectAsState()
    val loadingMore by shelfVM.loadingMore.collectAsState()
    val refreshing by shelfVM.refreshing.collectAsState()
    val toast by shelfVM.toast.collectAsState()

    var showCalendar by remember { mutableStateOf(false) }
    var selectedDay by remember(shelfType, category) { mutableStateOf<String?>(null) }
    var editingMark by remember { mutableStateOf<MarkSchema?>(null) }

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            shelfVM.consumeToast()
        }
    }

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
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                IconButton(onClick = { showCalendar = !showCalendar }) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = if (showCalendar) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                    val allMarks = s.data
                    val displayed = if (selectedDay != null) {
                        allMarks.filter { it.createdTime?.startsWith(selectedDay!!) == true }
                    } else allMarks

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
