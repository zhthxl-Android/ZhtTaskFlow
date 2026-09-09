package com.example.zhttaskflow.feature.log.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.example.zhttaskflow.base.ui.UiConstants
import com.example.zhttaskflow.feature.log.R

/**
 * 单条本地日志卡片：摘要常显，详情在展开后展示（内容由 ViewModel 懒加载）。
 *
 * 点击埋点由调用方通过 [modifier] 注入（如 [listItemClickWithLog]）。
 */
@Composable
internal fun LogEntryCard(
    entry: LogEntryUi,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(UiConstants.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
        ) {
            Text(
                text = stringResource(id = R.string.log_str_entry_time, entry.timestampText),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_type, entry.typeLabel),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_page_id, entry.pageId),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_action_id, entry.actionId),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_summary, entry.summary),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
            )
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = UiConstants.ListVerticalSpacing),
                    verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
                ) {
                    Text(
                        text = stringResource(id = R.string.log_str_entry_detail),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = entry.detailText
                            ?: stringResource(id = R.string.log_str_detail_loading),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
