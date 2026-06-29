package com.krelinnbios.neodblite.ui.page

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.krelinnbios.neodblite.BuildConfig
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.NeoUser
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.ui.UiState
import com.krelinnbios.neodblite.ui.component.AppUpdateDialog
import com.krelinnbios.neodblite.ui.component.CoverImage
import com.krelinnbios.neodblite.ui.component.EmptyBox
import com.krelinnbios.neodblite.ui.component.ErrorBox
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.i18n.AppLanguage
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.theme.AppTheme
import com.krelinnbios.neodblite.ui.vm.ProfileViewModel
import com.krelinnbios.neodblite.util.AppUpdateCheckResult
import com.krelinnbios.neodblite.util.AppUpdateInfo
import com.krelinnbios.neodblite.util.AppUpdateManager
import com.krelinnbios.neodblite.util.Browser
import kotlinx.coroutines.launch

private const val FEEDBACK_URL = "https://github.com/KrelinnBios/NeoDBLite/issues/new"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    user: NeoUser,
    host: String,
    profileVM: ProfileViewModel,
    onOpenItem: (ItemBrief) -> Unit,
    onOpenShelf: (ShelfType) -> Unit,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    val state by profileVM.state.collectAsState()
    val refreshing by profileVM.refreshing.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { profileVM.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ProfileHeader(user = user, host = host, onOpenSettings = { showSettings = true }) }

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

    if (showSettings) {
        ProfileSettingsDialog(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange,
            onLogout = onLogout,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun ProfileHeader(
    user: NeoUser,
    host: String,
    onOpenSettings: () -> Unit
) {
    val strings = LocalAppStrings.current
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
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = strings.navSettings,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.bio,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.bioText ?: strings.noBio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        url?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { Browser.open(context, it) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.openHomepage)
            }
        }
    }
}

@Composable
private fun ProfileSettingsDialog(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.74f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = strings.navSettings,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                SettingDropdownRow(
                    label = strings.language,
                    valueLabel = strings.languageLabel(currentLanguage)
                ) { dismiss ->
                    AppLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(strings.languageLabel(lang)) },
                            onClick = { onLanguageChange(lang); dismiss() }
                        )
                    }
                }

                SettingDropdownRow(
                    label = strings.themeColor,
                    valueLabel = strings.themeLabel(currentTheme),
                    valueLeading = { ColorDot(currentTheme.scheme.primary) }
                ) { dismiss ->
                    AppTheme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ColorDot(theme.scheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.themeLabel(theme))
                                }
                            },
                            onClick = { onThemeChange(theme); dismiss() }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 6.dp))

                ClickRow(
                    title = strings.checkUpdate,
                    subtitle = if (checking) strings.checking
                    else "${strings.currentVersionPrefix}${BuildConfig.VERSION_NAME}"
                ) {
                    if (checking) return@ClickRow
                    checking = true
                    scope.launch {
                        when (val result = AppUpdateManager.checkForUpdate()) {
                            AppUpdateCheckResult.NoUpdate ->
                                Toast.makeText(context, strings.alreadyLatest, Toast.LENGTH_SHORT).show()
                            is AppUpdateCheckResult.UpdateAvailable -> updateInfo = result.info
                            is AppUpdateCheckResult.Failed ->
                                Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                        }
                        checking = false
                    }
                }

                ClickRow(title = strings.feedback) {
                    Browser.open(context, FEEDBACK_URL)
                }

                Divider(modifier = Modifier.padding(vertical = 6.dp))

                ClickRow(title = strings.logout) { showLogoutConfirm = true }
                ClickRow(title = strings.close) { onDismiss() }
            }
        }
    }

    updateInfo?.let { info ->
        AppUpdateDialog(info = info, onDismiss = { updateInfo = null })
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(strings.logoutTitle) },
            text = { Text(strings.logoutMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onDismiss()
                    onLogout()
                }) { Text(strings.logout) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun SettingDropdownRow(
    label: String,
    valueLabel: String,
    valueLeading: (@Composable () -> Unit)? = null,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                valueLeading?.invoke()
                if (valueLeading != null) Spacer(Modifier.width(6.dp))
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                menuContent { expanded = false }
            }
        }
    }
}

@Composable
private fun ClickRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
