package com.krelinnbios.neodblite.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Visibility
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.friendlyMessage
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch

/**
 * 长按条目时的快速标记面板：底部弹层内复用 [MarkEditor]，自带「读取已有标记 → 保存/删除」逻辑，
 * 无需先进入详情页。[item] 必须带 uuid，否则直接关闭。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickMarkSheet(item: ItemBrief, onDismiss: () -> Unit) {
    val uuid = item.uuid
    if (uuid.isNullOrBlank()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val repo = App.container.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val category = Category.fromApi(item.category ?: item.type)

    var mark by remember(uuid) { mutableStateOf<MarkSchema?>(null) }
    var loaded by remember(uuid) { mutableStateOf(false) }
    LaunchedEffect(uuid) {
        repo.mark(uuid).onSuccess { mark = it }
        loaded = true
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = item.bestTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        )
        if (!loaded) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) { LoadingBox() }
        } else {
            MarkEditor(
                category = category,
                existing = mark?.let {
                    MarkDraft(
                        shelf = ShelfType.fromApi(it.shelfType) ?: ShelfType.WISHLIST,
                        grade = it.ratingGrade ?: 0,
                        comment = it.commentText.orEmpty(),
                        visibility = Visibility.fromApi(it.visibility),
                        tags = it.tags,
                        shareToFediverse = false
                    )
                },
                hasExisting = mark != null,
                onSave = { draft ->
                    scope.launch {
                        repo.postMark(
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
                            .onSuccess { Toast.makeText(context, strings.saved, Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                    }
                    onDismiss()
                },
                onDelete = {
                    scope.launch {
                        repo.deleteMark(uuid)
                            .onSuccess { Toast.makeText(context, strings.markDeleted, Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                    }
                    onDismiss()
                }
            )
        }
    }
}
