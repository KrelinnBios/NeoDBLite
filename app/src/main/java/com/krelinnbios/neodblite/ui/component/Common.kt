package com.krelinnbios.neodblite.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.krelinnbios.neodblite.data.model.Category
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.MarkSchema
import com.krelinnbios.neodblite.ui.i18n.LocalAppStrings
import com.krelinnbios.neodblite.ui.theme.ratingStar
import com.krelinnbios.neodblite.util.Format

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text(LocalAppStrings.current.retry) }
            }
        }
    }
}

@Composable
fun EmptyBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** 封面图，固定圆角与占位底色。 */
@Composable
fun CoverImage(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (!url.isNullOrBlank()) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 只读评分星（NeoDB 评分 0~10，对应 5 星，半星粒度）。 */
@Composable
fun RatingStars(rating: Double?, modifier: Modifier = Modifier) {
    val grade = ((rating ?: 0.0)).coerceIn(0.0, 10.0)
    val fullStars = (grade / 2).toInt()
    val hasHalf = (grade - fullStars * 2) >= 1.0
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val icon = when {
                index < fullStars -> Icons.Filled.Star
                index == fullStars && hasHalf -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ratingStar,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 发现页网格卡片：封面优先 + 标题 + 评分星。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemGridCard(
    item: ItemBrief,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        CoverImage(
            url = item.coverImageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.bestTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        // 有评分才显示星，未评分留空避免误读为 0 分。
        item.rating?.takeIf { it > 0.0 }?.let {
            RatingStars(rating = it)
        }
    }
}

/**
 * 书架行：封面 + 标题，然后依次是「我的评分（星）」「我写的短评」，最底为全站评分（数字/10）。
 */
@Composable
fun MarkRow(
    mark: MarkSchema,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val item = mark.item ?: return
    val strings = LocalAppStrings.current
    val myGrade = mark.ratingGrade?.takeIf { it > 0 }
    val myComment = mark.commentText?.takeIf { it.isNotBlank() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        CoverImage(
            url = item.coverImageUrl,
            modifier = Modifier.width(60.dp).height(84.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.bestTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (myGrade != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(rating = myGrade.toDouble())
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${strings.myRating} $myGrade/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (myComment != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = myComment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 有评分时，在全站评分上方显示我的评分日期。
            val myDate = if (myGrade != null) {
                mark.createdTime?.takeIf { it.length >= 10 }?.substring(0, 10)
            } else null
            if (myDate != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = myDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
            val cat = Category.fromApi(item.category ?: item.type)
                ?.let { strings.categoryLabel(it) }.orEmpty()
            val site = if (item.rating == null || item.rating <= 0.0) strings.noRating
            else "${Format.ratingText(item.rating)}/10"
            Text(
                text = if (cat.isNotBlank()) "$cat · $site" else site,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onEdit != null) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = strings.editMark,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
/** 列表/网格中通用的条目行：封面 + 标题 + 副标题 + 评分。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    item: ItemBrief,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        CoverImage(
            url = item.coverImageUrl,
            modifier = Modifier.width(60.dp).height(84.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.bestTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = Format.subtitle(item)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val cat = Category.fromApi(item.category ?: item.type)?.let { LocalAppStrings.current.categoryLabel(it) }.orEmpty()
                if (cat.isNotBlank()) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                RatingStars(rating = item.rating)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (item.rating == null || item.rating <= 0.0) LocalAppStrings.current.noRating else Format.ratingText(item.rating),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                trailing()
            }
        }
    }
}
