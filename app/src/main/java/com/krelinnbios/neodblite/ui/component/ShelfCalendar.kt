package com.krelinnbios.neodblite.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krelinnbios.neodblite.data.model.MarkSchema
import java.util.Calendar

/**
 * 书架标记热力图（月历）。基于已加载的 [marks] 的 created_time 统计每天的标记数，
 * 有标记的日子按数量深浅高亮；点某天回调 [onSelectDay]（再点同一天取消，返回 null）。
 * 数据仅覆盖已加载的标记，往更早月份翻看需先在列表里多滚动加载。
 */
@Composable
fun ShelfCalendar(
    marks: List<MarkSchema>,
    selectedDay: String?,
    onSelectDay: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = remember(marks) {
        marks.mapNotNull { it.createdTime?.takeIf { t -> t.length >= 10 }?.substring(0, 10) }
            .groupingBy { it }
            .eachCount()
    }
    val initialYm = remember(marks) {
        marks.mapNotNull { it.createdTime?.takeIf { t -> t.length >= 7 }?.substring(0, 7) }
            .maxOrNull() ?: currentYm()
    }
    var ymOverride by remember { mutableStateOf<String?>(null) }
    val ym = ymOverride ?: initialYm

    val year = ym.substring(0, 4).toIntOrNull() ?: 2026
    val month0 = (ym.substring(5, 7).toIntOrNull() ?: 1) - 1

    val cal = remember(ym) {
        Calendar.getInstance().apply { clear(); set(year, month0, 1) }
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val lead = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 周一为一周起点
    val cells: List<Int?> = buildList {
        repeat(lead) { add(null) }
        for (d in 1..daysInMonth) add(d)
        while (size % 7 != 0) add(null)
    }
    val maxCount = counts.filterKeys { it.startsWith(ym) }.values.maxOrNull() ?: 0
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        var showPicker by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { ymOverride = shiftMonth(ym, -1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = ym,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { showPicker = true }
                )
                DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
                    var pickYear by remember { mutableStateOf(year) }
                    Column(modifier = Modifier.padding(8.dp).width(220.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { pickYear-- }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                            }
                            Text(
                                text = pickYear.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { pickYear++ }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // 固定 3×4 网格。此前用 LazyVerticalGrid 时，Popup 内的条目复用会让
                        // 切换年份后残留旧的选中高亮；12 个月静态排布即可，无需懒加载。
                        (1..12).chunked(4).forEachIndexed { rowIndex, rowMonths ->
                            if (rowIndex > 0) Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowMonths.forEach { m ->
                                    val monthStr = "%02d".format(m)
                                    val selected = ym == "%04d-%02d".format(pickYear, m)
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable {
                                            ymOverride = "%04d-%02d".format(pickYear, m)
                                            showPicker = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = monthStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            IconButton(onClick = { ymOverride = shiftMonth(ym, 1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Text(
                    text = w,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        val key = "%04d-%02d-%02d".format(year, month0 + 1, day)
                        val cnt = counts[key] ?: 0
                        val intensity = if (cnt == 0) 0f
                        else (0.3f + 0.7f * cnt.toFloat() / maxCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val selected = key == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (cnt == 0) Color.Transparent else primary.copy(alpha = intensity))
                                .then(
                                    if (selected) Modifier.border(2.dp, primary, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .clickable(enabled = cnt > 0) {
                                    onSelectDay(if (selected) null else key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (intensity > 0.55f) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun currentYm(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
}

private fun shiftMonth(ym: String, delta: Int): String {
    val y = ym.substring(0, 4).toIntOrNull() ?: return ym
    val m = (ym.substring(5, 7).toIntOrNull() ?: return ym) - 1
    val c = Calendar.getInstance().apply { clear(); set(y, m, 1); add(Calendar.MONTH, delta) }
    return "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
}
