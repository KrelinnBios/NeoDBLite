package com.krelinnbios.neodblite.ui.page

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.krelinnbios.neodblite.data.model.CommunityEntry
import com.krelinnbios.neodblite.data.model.CommunityEntryType
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkInRequest
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Visibility
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.component.CoverImage
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.component.MarkDraft
import com.krelinnbios.neodblite.ui.component.MarkEditor
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
        else -> host.removePrefix("www.").ifBlank { "External link" }
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
    onBack: () -> Unit,
    onSearchTag: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val itemState by detailVM.item.collectAsState()
    val mark by detailVM.mark.collectAsState()
    val community by detailVM.community.collectAsState()
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
                title = { Text(strings.detail, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
                    mark = mark,
                    community = community,
                    onMark = { showSheet = true },
                    onSearchTag = onSearchTag
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    item: ItemBrief,
    mark: MarkSchema?,
    community: UiState<List<CommunityEntry>>?,
    onMark: () -> Unit,
    onSearchTag: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val markSummary = mark?.let {
        val cat = Category.fromApi(item.category ?: item.type)
        val shelf = ShelfType.fromApi(it.shelfType)
        buildString {
            if (shelf != null) append(strings.shelfLabel(shelf, cat))
            it.ratingGrade?.takeIf { g -> g > 0 }?.let { g -> append(" · ${strings.myRating} $g/10") }
        }.takeIf { s -> s.isNotBlank() }
    }
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
                        text = (if (item.rating == null || item.rating <= 0.0) strings.noRating else Format.ratingText(item.rating)) +
                            (item.ratingCount?.let { " ($it${strings.peopleCountSuffix})" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onMark, modifier = Modifier.fillMaxWidth()) {
            Text(if (markSummary.isNullOrBlank()) strings.mark else strings.editMark)
        }
        if (!markSummary.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = markSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val tags = item.tags.orEmpty().filter { it.isNotBlank() }
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    AssistChip(
                        onClick = { onSearchTag(tag) },
                        label = { Text(tag, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        if (!item.brief.isNullOrBlank()) {
            Spacer(Modifier.height(20.dp))
            Text(strings.intro, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
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
            Text(strings.sources, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sources.forEach { url ->
                    AssistChip(
                        onClick = { Browser.open(context, url) },
                        label = { Text("↗ ${sourceLabel(url)}") }
                    )
                }
            }
        }

        val browserUrl = remember(item) { itemWebUrl(item) }

        MyMarkCard(item = item, mark = mark, browserUrl = browserUrl)

        CommunitySection(community = community, browserUrl = browserUrl)
        if (!browserUrl.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { Browser.open(context, browserUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.openInBrowser)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}


/** 来源与社区内容之间：展示我自己的评分与短评，排版与社区卡片一致。 */
@Composable
private fun MyMarkCard(item: ItemBrief, mark: MarkSchema?, browserUrl: String?) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    if (mark == null) return
    val grade = mark.ratingGrade?.takeIf { it > 0 }
    val comment = mark.commentText?.takeIf { it.isNotBlank() }
    if (grade == null && comment == null) return

    val cat = Category.fromApi(item.category ?: item.type)
    val shelf = ShelfType.fromApi(mark.shelfType)
    Spacer(Modifier.height(20.dp))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                browserUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    Modifier.clickable { Browser.open(context, url) }
                } ?: Modifier
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.myRating,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (shelf != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.shelfLabel(shelf, cat),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (grade != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(rating = grade.toDouble())
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$grade/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (comment != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CommunitySection(
    community: UiState<List<CommunityEntry>>?,
    browserUrl: String?
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    Spacer(Modifier.height(20.dp))
    Text(strings.communityContent, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
    when (community) {
        null, is UiState.Loading -> Text(
            text = strings.loading,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        is UiState.Error -> Text(
            text = strings.communityLoadFailed,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        is UiState.Success -> {
            val entries = community.data
            if (entries.isEmpty()) {
                Text(
                    text = strings.noCommunityContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEach { entry ->
                    CommunityEntryCard(entry = entry)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    if (!browserUrl.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { Browser.open(context, browserUrl) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.openCommunityInBrowser)
        }
    }
}

@Composable
private fun CommunityEntryCard(entry: CommunityEntry) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                entry.url?.takeIf { it.isNotBlank() }?.let { url ->
                    Modifier.clickable { Browser.open(context, url) }
                } ?: Modifier
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (entry.type) {
                            CommunityEntryType.COMMENT -> strings.comments
                            CommunityEntryType.REVIEW -> strings.reviews
                            CommunityEntryType.NOTE -> strings.notes
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val meta = listOf(entry.author, entry.action).filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                entry.date?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            entry.rating?.takeIf { it > 0.0 }?.let {
                Spacer(Modifier.height(6.dp))
                RatingStars(rating = it)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
