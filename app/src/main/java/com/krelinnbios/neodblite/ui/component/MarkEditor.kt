package com.krelinnbios.neodblite.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Tag
import com.krelinnbios.neodblite.data.model.Visibility
import com.krelinnbios.neodblite.global.App
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import kotlinx.coroutines.launch
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class MarkDraft(
    val shelf: ShelfType,
    val grade: Int,
    val comment: String,
    val visibility: Visibility,
    val tags: List<String>,
    val shareToFediverse: Boolean,
    val createdTime: String? = null
)

private val markTagSeparators = Regex("""[\s,，、]+""")

fun parseMarkTags(text: String): List<String> =
    text.split(markTagSeparators)
        .map { it.trim().removePrefix("#") }
        .filter { it.isNotBlank() }
        .distinct()

private fun markTagRange(value: TextFieldValue): TextRange {
    var start = value.selection.min
    var end = value.selection.max
    while (start > 0 && !markTagSeparators.matches(value.text[start - 1].toString())) start--
    while (end < value.text.length && !markTagSeparators.matches(value.text[end].toString())) end++
    return TextRange(start, end)
}

internal fun markTagQuery(value: TextFieldValue): String {
    val range = markTagRange(value)
    return value.text.substring(range.start, range.end).removePrefix("#")
}

internal fun completeMarkTag(value: TextFieldValue, tag: String): TextFieldValue {
    val range = markTagRange(value)
    val tags = parseMarkTags(value.text.replaceRange(range.start, range.end, tag))
    val text = tags.joinToString(" ", postfix = " ")
    return TextFieldValue(text, selection = TextRange(text.length))
}

internal fun currentMarkDate(now: Date = Date()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now)

internal fun markDateToCreatedTime(text: String): String? {
    val date = text.trim()
    if (!Regex("""[0-9]{4}-[0-9]{2}-[0-9]{2}""").matches(date)) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val position = ParsePosition(0)
    if (format.parse(date, position) == null || position.index != date.length) return null
    // 与书架及日历直接读取 created_time 日期部分的口径保持一致。
    return "${date}T00:00:00Z"
}

internal fun sliderValueToGrade(value: Float): Int =
    value.roundToInt().coerceIn(0, 10)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarkEditor(
    category: Category?,
    existing: MarkDraft?,
    hasExisting: Boolean,
    onSave: (MarkDraft) -> Unit,
    onDelete: () -> Unit,
    saving: Boolean = false
) {
    val strings = LocalAppStrings.current
    val repo = App.container.repository
    val scope = rememberCoroutineScope()

    var shelf by remember(existing) { mutableStateOf(existing?.shelf ?: ShelfType.WISHLIST) }
    var grade by remember(existing) { mutableIntStateOf(existing?.grade ?: 0) }
    var comment by remember(existing) { mutableStateOf(existing?.comment ?: "") }
    var visibility by remember(existing) { mutableStateOf(existing?.visibility ?: Visibility.PUBLIC) }
    var tagsTextValue by remember(existing) {
        mutableStateOf(TextFieldValue(existing?.tags?.joinToString(" ") ?: ""))
    }
    var shareToFediverse by remember(existing) { mutableStateOf(existing?.shareToFediverse ?: false) }
    // 未主动指定日期时省略 created_time，由服务端保留原时间或记录状态变更时间。
    var specifyDate by remember(existing) { mutableStateOf(false) }
    var selectedDate by remember(existing) {
        mutableStateOf(existing?.createdTime?.takeIf { it.length >= 10 }?.take(10) ?: currentMarkDate())
    }
    var dateError by remember(existing) { mutableStateOf(false) }
    var tagFieldFocused by remember { mutableStateOf(false) }

    var allTags by remember { mutableStateOf<List<Tag>>(emptyList()) }

    // 加载用户的所有标签（加载多页）
    LaunchedEffect(Unit) {
        scope.launch {
            val loadedTags = mutableListOf<Tag>()
            var page = 1
            var hasMore = true

            while (hasMore && page <= 10) { // 最多加载10页
                repo.myTags(page).onSuccess { pagedTags ->
                    loadedTags.addAll(pagedTags.data)
                    hasMore = page < pagedTags.pages
                    page++
                }.onFailure {
                    hasMore = false
                }
            }

            allTags = loadedTags.sortedBy { it.bestTitle.lowercase() }
        }
    }

    val filteredTags = remember(allTags, tagsTextValue) {
        val query = markTagQuery(tagsTextValue)
        allTags.filter {
            it.bestTitle.isNotBlank() && it.bestTitle.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(strings.status, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(ShelfType.entries) { type ->
                FilterChip(
                    selected = type == shelf,
                    onClick = { shelf = type },
                    label = { Text(strings.shelfLabel(type, category)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = if (grade > 0) "${strings.rating}: $grade / 10" else "${strings.rating}: ${strings.unrated}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Slider(
            value = grade.toFloat(),
            onValueChange = { grade = sliderValueToGrade(it) },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(32.dp)
        )

        Spacer(Modifier.height(12.dp))
        Text(strings.visibility, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(Visibility.entries) { item ->
                FilterChip(
                    selected = item == visibility,
                    onClick = { visibility = item },
                    label = { Text(strings.visibilityLabel(item)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text(strings.shortCommentOptional) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tagsTextValue,
            onValueChange = { tagsTextValue = it },
            label = { Text(strings.tagsOptional) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { tagFieldFocused = it.isFocused }
        )

        if (tagFieldFocused && filteredTags.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp),
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = Color.Transparent
            ) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredTags.forEach { tag ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    tagsTextValue = completeMarkTag(tagsTextValue, tag.bestTitle)
                                },
                                label = { Text(tag.bestTitle) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                strings.specifyMarkDate,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = specifyDate, onCheckedChange = {
                specifyDate = it
                dateError = false
            })
        }

        if (specifyDate) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = selectedDate,
                onValueChange = {
                    selectedDate = it
                    dateError = false
                },
                label = { Text(strings.markDate) },
                placeholder = { Text("YYYY-MM-DD") },
                isError = dateError,
                supportingText = if (dateError) ({ Text(strings.invalidMarkDate) }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                strings.syncToFediverse,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = shareToFediverse, onCheckedChange = { shareToFediverse = it })
        }

        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !saving,
            onClick = {
                val createdTime = if (specifyDate) markDateToCreatedTime(selectedDate) else null
                if (specifyDate && createdTime == null) {
                    dateError = true
                    return@Button
                }
                onSave(
                    MarkDraft(
                        shelf = shelf,
                        grade = grade,
                        comment = comment,
                        visibility = visibility,
                        tags = parseMarkTags(tagsTextValue.text),
                        shareToFediverse = shareToFediverse,
                        createdTime = createdTime
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.saveMark)
        }
        if (hasExisting) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDelete, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                Text(strings.deleteMark)
            }
        }
    }
}
