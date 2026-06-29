package com.krelinnbios.neodblite.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.util.AppDownloadProgress
import com.krelinnbios.neodblite.util.AppUpdateInfo
import com.krelinnbios.neodblite.util.AppUpdateManager
import kotlinx.coroutines.launch

private enum class UpdateDownloadState {
    READY,
    DOWNLOADING,
    INSTALLER_OPENED,
    FAILED
}

@Composable
fun AppUpdateDialog(info: AppUpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    var state by remember(info.versionName) { mutableStateOf(UpdateDownloadState.READY) }
    var failureMessage by remember(info.versionName) { mutableStateOf("") }
    var downloadProgress by remember(info.versionName) { mutableStateOf<AppDownloadProgress?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (state != UpdateDownloadState.DOWNLOADING) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("${strings.newVersionPrefix}${info.versionName}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state) {
                    UpdateDownloadState.DOWNLOADING -> {
                        val p = downloadProgress
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (p == null) strings.preparingDownload
                                else {
                                    val src = if (p.sourceCount > 1) {
                                        "${p.sourceLabel}（第 ${p.sourceIndex}/${p.sourceCount} 个源）"
                                    } else p.sourceLabel
                                    "${strings.downloadingAndVerifying}\n${strings.sourcePrefix}$src"
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                            if (p != null && p.totalBytes > 0L) {
                                val frac = (p.downloadedBytes.toFloat() / p.totalBytes)
                                    .coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = String.format(
                                        "%.1f / %.1f MB（%d%%）",
                                        p.downloadedBytes / 1048576.0,
                                        p.totalBytes / 1048576.0,
                                        (frac * 100).toInt()
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    UpdateDownloadState.INSTALLER_OPENED -> {
                        Text(strings.installerOpened)
                    }

                    UpdateDownloadState.FAILED -> {
                        Text(
                            text = "${strings.updateFailed}${failureMessage}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    UpdateDownloadState.READY -> {
                        Text(
                            text = buildString {
                                appendLine("${strings.targetVersionPrefix}v${info.versionName}")
                                appendLine()
                                append(info.releaseNotes.ifBlank { strings.newVersionReleased })
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = state != UpdateDownloadState.DOWNLOADING,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    onClick = {
                        AppUpdateManager.openReleasesPage(context)
                        onDismiss()
                    }
                ) {
                    Text(strings.manualDownload, maxLines = 1, softWrap = false)
                }
                TextButton(
                    enabled = state != UpdateDownloadState.DOWNLOADING,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    onClick = onDismiss
                ) {
                    Text(strings.later, maxLines = 1, softWrap = false)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    modifier = Modifier.widthIn(min = 150.dp),
                    enabled = state != UpdateDownloadState.DOWNLOADING,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    onClick = {
                        scope.launch {
                            state = UpdateDownloadState.DOWNLOADING
                            failureMessage = ""
                            downloadProgress = null
                            AppUpdateManager.downloadAndOpenInstaller(
                                context,
                                info,
                                onProgress = { downloadProgress = it }
                            )
                                .onSuccess {
                                    state = UpdateDownloadState.INSTALLER_OPENED
                                }
                                .onFailure { error ->
                                    failureMessage = error.message ?: strings.unknownError
                                    state = UpdateDownloadState.FAILED
                                }
                        }
                    }
                ) {
                    Text(
                        text = when (state) {
                            UpdateDownloadState.DOWNLOADING -> strings.downloading
                            UpdateDownloadState.INSTALLER_OPENED -> strings.reopenInstaller
                            UpdateDownloadState.FAILED -> strings.retryAutoUpdate
                            UpdateDownloadState.READY -> strings.downloadAndInstall
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    )
}

@Composable
fun AppUpdateFailureDialog(reason: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(strings.checkUpdateFailed) },
        text = { Text(strings.openDownloadPageHint) },
        confirmButton = {
            Button(
                onClick = {
                    AppUpdateManager.openReleasesPage(context)
                    onDismiss()
                }
            ) {
                Text(strings.openDownloadPage)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
