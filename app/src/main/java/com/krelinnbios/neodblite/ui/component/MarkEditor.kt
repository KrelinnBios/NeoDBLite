package com.krelinnbios.neodblite.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.data.model.Visibility
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings

data class MarkDraft(
    val shelf: ShelfType,
    val grade: Int,
    val comment: String,
    val visibility: Visibility,
    val tags: List<String>,
    val shareToFediverse: Boolean
)

fun parseMarkTags(text: String): List<String> =
    text.split(Regex("""[\s,，、]+"""))
        .map { it.trim().removePrefix("#") }
        .filter { it.isNotBlank() }
        .distinct()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkEditor(
    category: Category?,
    existing: MarkDraft?,
    hasExisting: Boolean,
    onSave: (MarkDraft) -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    var shelf by remember(existing) { mutableStateOf(existing?.shelf ?: ShelfType.WISHLIST) }
    var grade by remember(existing) { mutableIntStateOf(existing?.grade ?: 0) }
    var comment by remember(existing) { mutableStateOf(existing?.comment ?: "") }
    var visibility by remember(existing) { mutableStateOf(existing?.visibility ?: Visibility.PUBLIC) }
    var tagsText by remember(existing) { mutableStateOf(existing?.tags?.joinToString(" ") ?: "") }
    var shareToFediverse by remember(existing) { mutableStateOf(existing?.shareToFediverse ?: false) }

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

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (grade > 0) "${strings.rating}: $grade / 10" else "${strings.rating}: ${strings.unrated}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = grade.toFloat(),
            onValueChange = { grade = it.toInt() },
            valueRange = 0f..10f,
            steps = 9
        )

        Spacer(Modifier.height(8.dp))
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

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text(strings.shortCommentOptional) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            label = { Text(strings.tagsOptional) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                strings.syncToFediverse,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = shareToFediverse, onCheckedChange = { shareToFediverse = it })
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                onSave(
                    MarkDraft(
                        shelf = shelf,
                        grade = grade,
                        comment = comment,
                        visibility = visibility,
                        tags = parseMarkTags(tagsText),
                        shareToFediverse = shareToFediverse
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.saveMark)
        }
        if (hasExisting) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(strings.deleteMark)
            }
        }
    }
}
