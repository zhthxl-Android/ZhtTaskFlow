package com.example.zhttaskflow.base.ui.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants

/**
 * 详情页默认骨架布局：封面图占位 + 标题行 + 正文多行（与列表骨架扫光/色板一致）。
 *
 * 业务可在 [TaskFlowSkeletonDetailScreen] 的 [content] 中替换为自定义占位组合。
 */
@Composable
fun TaskFlowSkeletonDetailTemplate(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.PageHorizontalPadding),
    ) {
        TaskFlowSkeletonRect(
            height = TaskFlowUiConstants.SkeletonDetailHeroHeight,
            cornerRadius = TaskFlowUiConstants.SkeletonDetailBlockCornerRadius,
        )
        TaskFlowSkeletonLine(
            height = TaskFlowUiConstants.SkeletonTitleLineHeight,
            width = TaskFlowUiConstants.SkeletonDetailTitleLineWidth,
        )
        TaskFlowSkeletonMultiline(lineCount = TaskFlowUiConstants.SkeletonDetailPrimaryMultilineCount)
        Spacer(modifier = Modifier.height(TaskFlowUiConstants.ListVerticalSpacing))
        TaskFlowSkeletonMultiline(
            lineCount = TaskFlowUiConstants.SkeletonDetailSecondaryMultilineCount,
            lastLineWidthFraction = TaskFlowUiConstants.SkeletonDetailSecondaryMultilineLastLineWidthFraction,
        )
    }
}

/**
 * 详情页全屏骨架（含 [TaskFlowSkeletonContainer] 扫光动画）。
 *
 * @param contentPadding 骨架区域内边距；置于 [com.example.zhttaskflow.base.ui.StateBox] 内时通常为 [PaddingValues.Zero]（由 StateBox 统一 padding）
 * @param content 自定义占位布局，默认 [TaskFlowSkeletonDetailTemplate]
 */
@Composable
fun TaskFlowSkeletonDetailScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { TaskFlowSkeletonDetailTemplate() },
) {
    TaskFlowSkeletonContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            content()
        }
    }
}

/**
 * 供 [com.example.zhttaskflow.base.ui.StateBox] 使用的详情骨架 loading 工厂（替换默认转圈）。
 */
@Composable
fun rememberTaskFlowDetailSkeletonLoading(
    contentPadding: PaddingValues = PaddingValues(),
): @Composable (Modifier) -> Unit {
    return remember(contentPadding) {
        { loadingModifier ->
            TaskFlowSkeletonDetailScreen(
                contentPadding = contentPadding,
                modifier = loadingModifier,
            )
        }
    }
}
