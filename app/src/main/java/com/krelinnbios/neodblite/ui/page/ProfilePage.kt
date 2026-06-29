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
import com.krelinnbios.neodblite.data.model.shelfLabel
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.CoverImage
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
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { ProfileHeader(user = user, host = host) }

            when (val s = state) {
                is UiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) { LoadingBox() }
                }
                is UiState.Error -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        ErrorBox(s.message, onRetry = { profileVM.load() })
                    }
                }
                is UiState.Success -> {
                    item {
                        StatsRow(counts = s.data.counts, onOpenShelf = onOpenShelf)
                    }
                    if (s.data.recent.isNotEmpty()) {
                        item {
                            Text(
                                text = "最近标记",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(s.data.recent) { item ->
                                    RecentCover(item = item, onClick = { onOpenItem(item) })
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: NeoUser, host: String) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val avatar = user.avatar
            if (!avatar.isNullOrBlank()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Spacer(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.bestName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = user.externalAcct?.takeIf { it.isNotBlank() } ?: host,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val url = user.url
        if (!url.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { Browser.open(context, url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("在浏览器中打开主页")
            }
        }
    }
}

@Composable
private fun StatsRow(
    counts: Map<ShelfType, Int>,
    onOpenShelf: (ShelfType) -> Unit
) {
    val order = listOf(ShelfType.WISHLIST, ShelfType.PROGRESS, ShelfType.COMPLETE)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        order.forEach { type ->
            Surface(
                modifier = Modifier.weight(1f).clickable { onOpenShelf(type) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = (counts[type] ?: 0).toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = shelfLabel(type, null),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentCover(item: ItemBrief, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(92.dp).clickable(onClick = onClick)
    ) {
        CoverImage(url = item.coverImageUrl, modifier = Modifier.width(92.dp).height(130.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.bestTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
