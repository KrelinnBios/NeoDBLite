package com.krelinnbios.neodblite.ui.page

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.vm.AuthViewModel

@Composable
fun LoginPage(authVM: AuthViewModel) {
    val strings = LocalAppStrings.current
    val message by authVM.loginMessage.collectAsState()
    var host by rememberSaveable { mutableStateOf(authVM.currentHost) }
    var authorizationUrl by rememberSaveable { mutableStateOf<String?>(null) }

    authorizationUrl?.let { url ->
        AuthorizationWebView(
            url = url,
            onBack = { authorizationUrl = null },
            onCode = { code ->
                authorizationUrl = null
                authVM.handleAuthCode(code)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NeoDB Lite",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.loginSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text(strings.instanceHost) },
            placeholder = { Text("neodb.social") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                authVM.beginLogin(host) { url -> authorizationUrl = url }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.login)
        }

        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = message!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = strings.loginHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthorizationWebView(
    url: String,
    onBack: () -> Unit,
    onCode: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var callbackHandled by remember { mutableStateOf(false) }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.login,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean = handleUrl(request.url)

                        @Deprecated("Deprecated in Android WebView API")
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                            handleUrl(Uri.parse(url))

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            if (!url.isNullOrBlank()) handleUrl(Uri.parse(url))
                        }

                        private fun handleUrl(uri: Uri): Boolean {
                            if (uri.scheme == "neodblite" && uri.host == "oauth") {
                                uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let { code ->
                                    if (!callbackHandled) {
                                        callbackHandled = true
                                        onCode(code)
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    }
                    loadUrl(url)
                }
            }
        )
    }
}
