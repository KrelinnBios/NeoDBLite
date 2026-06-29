package com.krelinnbios.neodblite.ui.page

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Visibility
import com.krelinnbios.neodblite.data.model.shelfLabel
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.CoverImage
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.component.RatingStars
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.vm.DetailViewModel
import com.krelinnbios.neodblite.util.Browser
import com.krelinnbios.neodblite.util.Format

/** 外部来源站点的可读名称。 */
private fun sourceLabel(url: String): String {
    val host = url.removePrefix("https://").removePrefix("http://").substringBefore('/').lowercase()
    return when {
        "douban" in host -> "豆瓣"
        "imdb" in host -> "IMDb"
        "themoviedb" in host || host == "tmdb.org" -> "TMDB"
        "bgm.tv" in host || "bangumi" in host -> "Bangumi"
        "goodreads" in host -> "Goodreads"
        "spotify" in host -> "Spotify"
        "music.apple" in host -> "Apple Music"
        "bandcamp" in host -> "Bandcamp"
        "igdb" in host -> "IGDB"
        "steam" in host -> "Steam"
        "google" in host && "books" in url -> "Google Books"
        else -> host.removePrefix("www.").ifBlank { "外部链接" }
    }
}

/** 条目在网页端的完整地址：优先用绝对的 id，否则用当前实例 host 拼相对 url。 */
private fun itemWebUrl(item: ItemBrief): String? {
    item.id?.takeIf { it.startsWith("http", ignoreCase = true) }?.let { return it }
    val relative = item.url?.takeIf { it.isNotBlank() } ?: return null
    val host = App.container.authStore.cachedHost
    return "https://$host/" + relative.removePrefix("/")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailPage(
    path: String,
    detailVM: DetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val itemState by detailVM.item.collectAsState()
    val mark by detailVM.mark.collectAsState()
    val toast by detailVM.toast.collectAsState()

    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(path) { detailVM.loadOnce(path) }
    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            detailVM.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = itemState) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.message, onRetry = { detailVM.load(path) })
                is UiState.Success -> DetailContent(
                    item = s.data,
                    markSummary = mark?.let {
                        val cat = Category.fromApi(s.data.category ?: s.data.type)
                        val shelf = ShelfType.fromApi(it.shelfType)
                        buildString {
                            if (shelf != null) append(shelfLabel(shelf, cat))
                            it.ratingGrade?.takeIf { g -> g > 0 }?.let { g -> append(" · 我的评分 $g/10") }
                        }
                    },
                    onMark = { showSheet = true }
                )
            }
        }
    }

    val current = itemState
    if (showSheet && current is UiState.Success && current.data.uuid != null) {
        val uuid = current.data.uuid!!
        val category = Category.fromApi(current.data.category ?: current.data.type)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
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
                    detailVM.saveMark(
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
                    showSheet = false
                },
                onDelete = {
                    detailVM.deleteMark(uuid)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun DetailContent(
    item: ItemBrief,
    markSummary: String?,
    onMark: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row {
            CoverImage(url = item.coverImageUrl, modifier = Modifier.width(110.dp).height(154.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.bestTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = Format.subtitle(item)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(rating = item.rating)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Format.ratingText(item.rating) +
                            (item.ratingCount?.let { "（$it 人）" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onMark, modifier = Modifier.fillMaxWidth()) {
            Text(if (markSummary.isNullOrBlank()) "标记" else "修改标记")
        }
        if (!markSummary.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = markSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (!item.brief.isNullOrBlank()) {
            Spacer(Modifier.height(20.dp))
            Text("简介", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.brief!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val sources = item.externalResources.orEmpty().mapNotNull { it.url }.filter { it.isNotBlank() }
        if (sources.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("来源", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            sources.forEach { url ->
                Text(
                    text = "↗ ${sourceLabel(url)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { Browser.open(context, url) }
                        .padding(vertical = 8.dp)
                )
            }
        }

        val browserUrl = remember(item) { itemWebUrl(item) }
        if (!browserUrl.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { Browser.open(context, browserUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("在浏览器中打开")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** 标记编辑草稿。 */
data class MarkDraft(
    val shelf: ShelfType,
    val grade: Int,
    val comment: String,
    val visibility: Visibility,
    val tags: List<String>,
    val shareToFediverse: Boolean
)

/** 把用户输入的标签文本（空格/逗号/顿号分隔）解析为去重后的标签列表。 */
private fun parseTags(text: String): List<String> =
    text.split(' ', ',', '，', '、', '\n')
        .map { it.trim().removePrefix("#") }
        .filter { it.isNotBlank() }
        .distinct()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkEditor(
    category: Category?,
    existing: MarkDraft?,
    hasExisting: Boolean,
    onSave: (MarkDraft) -> Unit,
    onDelete: () -> Unit
) {
    var shelf by remember { mutableStateOf(existing?.shelf ?: ShelfType.WISHLIST) }
    var grade by remember { mutableIntStateOf(existing?.grade ?: 0) }
    var comment by remember { mutableStateOf(existing?.comment ?: "") }
    var visibility by remember { mutableStateOf(existing?.visibility ?: Visibility.PUBLIC) }
    var tagsText by remember { mutableStateOf(existing?.tags?.joinToString(" ") ?: "") }
    var shareToFediverse by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("状态", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShelfType.entries.forEach { type ->
                FilterChip(
                    selected = type == shelf,
                    onClick = { shelf = type },
                    label = { Text(shelfLabel(type, category)) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (grade > 0) "评分：$grade / 10" else "评分：未评分",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = grade.toFloat(),
            onValueChange = { grade = it.toInt() },
            valueRange = 0f..10f,
            steps = 9
        )

        Spacer(Modifier.height(8.dp))
        Text("可见性", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Visibility.entries.forEach { v ->
                FilterChip(
                    selected = v == visibility,
                    onClick = { visibility = v },
                    label = { Text(v.label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("短评（可选）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            label = { Text("标签（空格或逗号分隔，可选）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "同步到联邦宇宙",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = shareToFediverse, onCheckedChange = { shareToFediverse = it })
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                onSave(
                    MarkDraft(
                        shelf = shelf,
                        grade = grade,
                        comment = comment,
                        visibility = visibility,
                        tags = parseTags(tagsText),
                        shareToFediverse = shareToFediverse
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存标记")
        }
        if (hasExisting) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("删除标记")
            }
        }
    }
}
