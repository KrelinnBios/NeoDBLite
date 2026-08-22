package com.krelinnbios.neodblite.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * 自适应页签：先测量各页签文案的理想宽度，若按等宽（maxWidth/数量）排布最宽的标签
 * 也能放下，就用固定 [TabRow]（中文等短标签均匀铺满、更美观）；否则回退
 * [ScrollableTabRow]，避免英文/日文等较长标签在等宽下被挤换行。
 *
 * 修复：暗黑模式下显式指定选中/未选中颜色与容器色，避免系统默认色在深色主题下对比度不足。
 * 短中文（想要/进行中等）保持等宽 TabRow铺满，长英文自动回退为可横向滑动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AdaptiveTabRow(
    tabs: List<T>,
    selectedIndex: Int,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    if (tabs.isEmpty()) return
    val safeIndex = selectedIndex.coerceIn(0, tabs.size - 1)

    SubcomposeLayout { constraints ->
        val maxWidth = constraints.maxWidth

        // 第一遍仅测量文案宽度（含页签水平内边距），不进入布局。
        val labelWidths = subcompose("measure") {
            tabs.forEach { tab ->
                Text(
                    text = label(tab),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }.map { it.measure(Constraints()).width }

        // 固定等宽下每个页签分到 maxWidth/数量，最宽的能放下才不会换行/截断。
        val widest = labelWidths.maxOrNull() ?: 0
        val fits = widest * tabs.size <= maxWidth

        val tabSlot: @Composable () -> Unit = {
            tabs.forEachIndexed { index, tab ->
                val selected = index == safeIndex
                Tab(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = label(tab),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        val placeable = subcompose("content") {
            if (fits) {
                TabRow(
                    selectedTabIndex = safeIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) { tabSlot() }
            } else {
                ScrollableTabRow(
                    selectedTabIndex = safeIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp
                ) { tabSlot() }
            }
        }.first().measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}
