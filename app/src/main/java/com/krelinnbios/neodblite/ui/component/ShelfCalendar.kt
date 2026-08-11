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
    // 仅保留有已加载标记的月份，月份导航和选择器都只在这些月份间移动。
    val monthsWithData = remember(counts) {
        counts.keys.map { it.substring(0, 7) }.distinct().sorted()
    }
    val initialYm = remember(monthsWithData) {
        monthsWithData.lastOrNull() ?: currentYm()
    }
    var ymOverride by remember { mutableStateOf<String?>(null) }
    // 切换书架/类目后，旧月份可能不再有数据，此时自动回到当前数据的最新月份。
    val ym = ymOverride?.takeIf { it in monthsWithData } ?: initialYm
    val monthIndex = monthsWithData.indexOf(ym)
    val previousYm = monthsWithData.getOrNull(monthIndex - 1)
    val nextYm = monthsWithData.getOrNull(monthIndex + 1)

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
            IconButton(
                enabled = previousYm != null,
                onClick = {
                    ymOverride = previousYm
                    if (selectedDay != null) onSelectDay(null)
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = ym,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(enabled = monthsWithData.isNotEmpty()) { showPicker = true }
                )
                val yearsWithData = remember(monthsWithData) {
                    monthsWithData.map { it.substring(0, 4).toInt() }.distinct()
                }
                // 选择器只浏览有标记的年份。打开弹窗时重置为当前月份所在年份。
                var pickYear by remember(showPicker) { mutableStateOf(year) }
                // 锚点盒子与弹窗内容同宽（220dp + 左右 8dp 内边距）并在标题区居中，
                // 弹窗贴锚点左缘展开即水平居中；盒子无点击处理，不影响标题的点按。
                // 注意不要把 DropdownMenu 放进 BoxWithConstraints：其内容是子组合，
                // 弹窗挂在子组合下会不随状态重组（年份切换/点选全部失效）。
                Box(modifier = Modifier.width(236.dp).height(1.dp)) {
                    DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
                        Column(modifier = Modifier.padding(8.dp).width(220.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val pickYearIndex = yearsWithData.indexOf(pickYear)
                                val previousYear = yearsWithData.getOrNull(pickYearIndex - 1)
                                val nextYear = yearsWithData.getOrNull(pickYearIndex + 1)
                                IconButton(
                                    enabled = previousYear != null,
                                    onClick = { previousYear?.let { pickYear = it } }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                                }
                                Text(
                                    text = pickYear.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    enabled = nextYear != null,
                                    onClick = { nextYear?.let { pickYear = it } }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            val markedMonths = monthsWithData
                                .filter { it.startsWith("%04d-".format(pickYear)) }
                                .map { it.substring(5, 7).toInt() }
                            markedMonths.chunked(4).forEachIndexed { rowIndex, rowMonths ->
                                androidx.compose.runtime.key(pickYear, rowIndex) {
                                if (rowIndex > 0) Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowMonths.forEach { m ->
                                        val ymStr = "%04d-%02d".format(pickYear, m)
                                        val selected = ym == ymStr
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable {
                                                ymOverride = ymStr
                                                if (selectedDay != null) onSelectDay(null)
                                                showPicker = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "%02d".format(m),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textAlign = TextAlign.Center,
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    repeat(4 - rowMonths.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            }
            IconButton(
                enabled = nextYm != null,
                onClick = {
                    ymOverride = nextYm
                    if (selectedDay != null) onSelectDay(null)
                }
            ) {
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
