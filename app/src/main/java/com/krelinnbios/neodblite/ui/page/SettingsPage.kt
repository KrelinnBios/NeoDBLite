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
import com.krelinnbios.neodblite.ui.i18n.AppLanguage
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
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
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
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
        LanguageSection(
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange
        )
        Divider()
        ThemeSection(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange
        )
        Divider()
        SettingRow(
            title = strings.checkUpdate,
            subtitle = if (checking) strings.checking else "${strings.currentVersionPrefix}${BuildConfig.VERSION_NAME}",
            onClick = {
                if (checking) return@SettingRow
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
        )
        SettingRow(
            title = strings.releasesPage,
            subtitle = strings.releasesSubtitle,
            onClick = { AppUpdateManager.openReleasesPage(context) }
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = strings.appTagline,
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
            title = { Text(strings.logoutTitle) },
            text = { Text(strings.logoutMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
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
                text = user?.bestName ?: LocalAppStrings.current.notLoggedIn,
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
            Text(LocalAppStrings.current.logout)
        }
    }
}


@Composable
private fun LanguageSection(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = strings.language,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppLanguage.entries) { language ->
                FilterChip(
                    selected = language == currentLanguage,
                    onClick = { onLanguageChange(language) },
                    label = { Text(strings.languageLabel(language)) }
                )
            }
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
            text = LocalAppStrings.current.themeColor,
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
