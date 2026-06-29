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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.krelinnbios.neodblite.BuildConfig
import com.krelinnbios.neodblite.data.model.NeoUser
import com.krelinnbios.neodblite.ui.component.AppUpdateDialog
import com.krelinnbios.neodblite.ui.theme.AppTheme
import com.krelinnbios.neodblite.util.AppUpdateCheckResult
import com.krelinnbios.neodblite.util.AppUpdateInfo
import com.krelinnbios.neodblite.util.AppUpdateManager
import kotlinx.coroutines.launch

@Composable
fun SettingsPage(
    user: NeoUser?,
    host: String,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        AccountHeader(
            user = user,
            host = host,
            onLogout = { showLogoutConfirm = true }
        )
        Divider()
        ThemeSection(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange
        )
        Divider()
        SettingRow(
            title = "检查更新",
            subtitle = if (checking) "检查中…" else "当前版本 v${BuildConfig.VERSION_NAME}",
            onClick = {
                if (checking) return@SettingRow
                checking = true
                scope.launch {
                    when (val result = AppUpdateManager.checkForUpdate()) {
                        AppUpdateCheckResult.NoUpdate ->
                            Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                        is AppUpdateCheckResult.UpdateAvailable -> updateInfo = result.info
                        is AppUpdateCheckResult.Failed ->
                            Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                    }
                    checking = false
                }
            }
        )
        SettingRow(
            title = "Releases 页面",
            subtitle = "在浏览器中查看历史版本",
            onClick = { AppUpdateManager.openReleasesPage(context) }
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "NeoDB Lite · 非官方 NeoDB 客户端",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }

    updateInfo?.let { info ->
        AppUpdateDialog(info = info, onDismiss = { updateInfo = null })
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录") },
            text = { Text("将清除本地保存的登录令牌，需要重新授权才能继续使用。") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AccountHeader(
    user: NeoUser?,
    host: String,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatar = user?.avatar
        if (!avatar.isNullOrBlank()) {
            AsyncImage(
                model = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.bestName ?: "未登录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = user?.externalAcct?.takeIf { it.isNotBlank() } ?: host,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onLogout) {
            Text("退出")
        }
    }
}

@Composable
private fun ThemeSection(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = "主题颜色",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppTheme.entries) { theme ->
                FilterChip(
                    selected = theme == currentTheme,
                    onClick = { onThemeChange(theme) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(theme.scheme.primary)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(theme.label)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
