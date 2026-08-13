package com.krelinnbios.neodblite.ui.component

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicLong

object AppToast {
    private const val LENGTH_SHORT = 2_000L
    private val nextId = AtomicLong(0L)
    private val _events = MutableSharedFlow<AppToastEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events = _events.asSharedFlow()

    fun show(message: String, durationMillis: Long = LENGTH_SHORT) {
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) return
        _events.tryEmit(
            AppToastEvent(
                id = nextId.incrementAndGet(),
                message = cleanMessage,
                durationMillis = durationMillis.coerceAtLeast(800L)
            )
        )
    }
}

data class AppToastEvent(
    val id: Long,
    val message: String,
    val durationMillis: Long
)

@Composable
private fun AppToastPill(message: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = message,
                modifier = Modifier.padding(start = 10.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppToastHost(modifier: Modifier = Modifier) {
    var displayedEvent by remember { mutableStateOf<AppToastEvent?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppToast.events.collectLatest { event ->
            displayedEvent = event
            visible = true
            delay(event.durationMillis)
            visible = false
            delay(180L)
            if (displayedEvent?.id == event.id) displayedEvent = null
        }
    }

    val event = displayedEvent
    if (visible && event != null) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // Toast 需要浮在应用内其它 Dialog 之上，但不应再给页面增加一层遮罩。
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window?.setDimAmount(0f)
            }
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .zIndex(220f),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(200)) +
                        slideInVertically(initialOffsetY = { 30 }),
                    exit = fadeOut(animationSpec = tween(500)),
                    modifier = Modifier
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
                ) {
                    AppToastPill(message = event.message)
                }
            }
        }
    }
}
