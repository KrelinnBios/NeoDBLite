package com.krelinnbios.neodblite.ui.page

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.NeoUser
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.component.CoverImage
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.vm.ProfileViewModel
import com.krelinnbios.neodblite.util.Browser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    user: NeoUser,
    host: String,
    profileVM: ProfileViewModel,
    onOpenItem: (ItemBrief) -> Unit,
    onOpenShelf: (ShelfType) -> Unit
) {
    val state by profileVM.state.collectAsState()
    val refreshing by profileVM.refreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { profileVM.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ProfileHeader(user = user, host = host) }

            when (val s = state) {
                is UiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) { LoadingBox() }
                }
                is UiState.Error -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        ErrorBox(s.message, onRetry = { profileVM.load() })
                    }
                }
                is UiState.Success -> {
                    item {
                        ShelfSummary(
                            counts = s.data.counts,
                            onOpenShelf = onOpenShelf
                        )
                    }
                    item {
                        RecentSection(
                            items = s.data.recent,
                            onOpenItem = onOpenItem
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: NeoUser, host: String) {
    val context = LocalContext.current
    val url = user.url
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val avatar = user.avatar
            if (!avatar.isNullOrBlank()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.bestName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.externalAcct?.takeIf { it.isNotBlank() } ?: host,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        ProfileInfoLine(label = LocalAppStrings.current.instanceHost, value = host)
        user.username?.takeIf { it.isNotBlank() }?.let {
            ProfileInfoLine(label = LocalAppStrings.current.username, value = it)
        }
        user.displayName?.takeIf { it.isNotBlank() && it != user.username }?.let {
            ProfileInfoLine(label = LocalAppStrings.current.displayName, value = it)
        }
        url?.takeIf { it.isNotBlank() }?.let {
            ProfileInfoLine(label = LocalAppStrings.current.homepage, value = compactUrl(it))
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { Browser.open(context, it) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(LocalAppStrings.current.openHomepage)
            }
        }
    }
}

@Composable
private fun ProfileInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShelfSummary(
    counts: Map<ShelfType, Int>,
    onOpenShelf: (ShelfType) -> Unit
) {
    val total = ShelfType.entries.sumOf { counts[it] ?: 0 }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = LocalAppStrings.current.shelfOverview,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = LocalAppStrings.current.totalMarks,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = LocalAppStrings.current.shelfSynced,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val rows = listOf(
            listOf(ShelfType.WISHLIST, ShelfType.PROGRESS),
            listOf(ShelfType.COMPLETE, ShelfType.DROPPED)
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { type ->
                    StatTile(
                        type = type,
                        count = counts[type] ?: 0,
                        onClick = { onOpenShelf(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatTile(
    type: ShelfType,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = LocalAppStrings.current.shelfLabel(type, null),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentSection(
    items: List<ItemBrief>,
    onOpenItem: (ItemBrief) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = LocalAppStrings.current.recentComplete,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp)
        )
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                EmptyBox(LocalAppStrings.current.noCompleteItems)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    RecentCover(item = item, onClick = { onOpenItem(item) })
                }
            }
        }
    }
}

@Composable
private fun RecentCover(item: ItemBrief, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(96.dp).clickable(onClick = onClick)
    ) {
        CoverImage(url = item.coverImageUrl, modifier = Modifier.width(96.dp).height(136.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.bestTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun compactUrl(url: String): String =
    url.removePrefix("https://")
        .removePrefix("http://")
        .trimEnd('/')
