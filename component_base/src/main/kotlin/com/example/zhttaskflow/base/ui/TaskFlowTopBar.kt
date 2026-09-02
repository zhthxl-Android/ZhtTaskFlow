package com.example.zhttaskflow.base.ui



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.RowScope

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.Dp



/**

 * 顶栏标题在栏内的对齐方式。

 */

enum class TaskFlowTopBarTitlePosition {

    /** 一级页：标题在中间区域左对齐（与 [leading] 同行）。 */

    Start,



    /** 二级页：标题在整条顶栏水平居中（对齐原 CenterAlignedTopAppBar）。 */

    Center,

}



/**

 * 判断顶部栏是否应参与布局（与 [TaskFlowTopBar] 内部 early-return 规则一致）。

 */

internal fun taskFlowTopBarShouldCompose(

    title: String? = null,

    hasLeadingSlot: Boolean = false,

    hasCenterSlot: Boolean = false,

    hasTrailingSlot: Boolean = false,

): Boolean {

    return hasLeadingSlot ||

        hasCenterSlot ||

        !title.isNullOrBlank() ||

        hasTrailingSlot

}



/**

 * 全项目通用三插槽顶部栏：左 / 中 / 右可空；全空时不占位。

 *

 * @param barHeight 顶栏内容区高度（不含状态栏 inset）

 * @param titlePosition [TaskFlowTopBarTitlePosition.Center] 用于二级详情页标题居中

 */

@Composable

fun TaskFlowTopBar(

    modifier: Modifier = Modifier,

    title: String? = null,

    leading: @Composable (() -> Unit)? = null,

    center: @Composable (() -> Unit)? = null,

    trailing: (@Composable RowScope.() -> Unit)? = null,

    barHeight: Dp = TaskFlowUiConstants.TopBarHeight,

    titlePosition: TaskFlowTopBarTitlePosition = TaskFlowTopBarTitlePosition.Start,

    applyStatusBarsPadding: Boolean = true,
    collapseProgress: Float = 1f,
) {
    val hasLeadingSlot = leading != null

    val hasCenterSlot = center != null

    val hasTitleInCenter = !title.isNullOrBlank()

    val hasCenterContent = hasCenterSlot || hasTitleInCenter

    val hasTrailingSlot = trailing != null



    if (!taskFlowTopBarShouldCompose(

            title = title,

            hasLeadingSlot = hasLeadingSlot,

            hasCenterSlot = hasCenterSlot,

            hasTrailingSlot = hasTrailingSlot,

        )

    ) {

        return

    }



    val barModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            alpha = collapseProgress.coerceIn(0f, 1f)
        }
        .then(
            if (applyStatusBarsPadding) {

                Modifier.statusBarsPadding()

            } else {

                Modifier

            },

        )

        .height(barHeight)

        .padding(horizontal = TaskFlowUiConstants.PageHorizontalPadding)



    when (titlePosition) {

        TaskFlowTopBarTitlePosition.Center -> {

            TaskFlowTopBarCenteredLayout(

                barModifier = barModifier,

                title = title,

                hasLeadingSlot = hasLeadingSlot,

                hasCenterSlot = hasCenterSlot,

                hasTitleInCenter = hasTitleInCenter,

                hasCenterContent = hasCenterContent,

                hasTrailingSlot = hasTrailingSlot,

                leading = leading,

                center = center,

                trailing = trailing,

            )

        }

        TaskFlowTopBarTitlePosition.Start -> {

            TaskFlowTopBarStartLayout(

                barModifier = barModifier,

                title = title,

                hasLeadingSlot = hasLeadingSlot,

                hasCenterSlot = hasCenterSlot,

                hasTitleInCenter = hasTitleInCenter,

                hasCenterContent = hasCenterContent,

                hasTrailingSlot = hasTrailingSlot,

                leading = leading,

                center = center,

                trailing = trailing,

            )

        }

    }

}



@Composable

private fun TaskFlowTopBarStartLayout(

    barModifier: Modifier,

    title: String?,

    hasLeadingSlot: Boolean,

    hasCenterSlot: Boolean,

    hasTitleInCenter: Boolean,

    hasCenterContent: Boolean,

    hasTrailingSlot: Boolean,

    leading: @Composable (() -> Unit)?,

    center: @Composable (() -> Unit)?,

    trailing: (@Composable RowScope.() -> Unit)?,

) {

    Row(

        modifier = barModifier,

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),

    ) {

        if (hasLeadingSlot) {

            Box(contentAlignment = Alignment.CenterStart) {

                leading?.invoke()

            }

        }

        if (hasCenterContent || hasTrailingSlot) {

            Box(

                modifier = Modifier.weight(1f),

                contentAlignment = Alignment.CenterStart,

            ) {

                when {

                    hasCenterSlot -> center?.invoke()

                    hasTitleInCenter -> {

                        TaskFlowTopBarTitleText(text = title.orEmpty())

                    }

                }

            }

        }

        if (hasTrailingSlot) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trailing?.invoke(this)
            }
        }

    }

}



@Composable

private fun TaskFlowTopBarCenteredLayout(

    barModifier: Modifier,

    title: String?,

    hasLeadingSlot: Boolean,

    hasCenterSlot: Boolean,

    hasTitleInCenter: Boolean,

    hasCenterContent: Boolean,

    hasTrailingSlot: Boolean,

    leading: @Composable (() -> Unit)?,

    center: @Composable (() -> Unit)?,

    trailing: (@Composable RowScope.() -> Unit)?,

) {

    Box(modifier = barModifier) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween,

        ) {

            Box(contentAlignment = Alignment.CenterStart) {

                if (hasLeadingSlot) {

                    leading?.invoke()

                }

            }

            Row(

                horizontalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),

                verticalAlignment = Alignment.CenterVertically,

            ) {

                if (hasTrailingSlot) {

                    trailing?.invoke(this)

                }

            }

        }

        when {

            hasCenterSlot -> {

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .align(Alignment.Center),

                    contentAlignment = Alignment.Center,

                ) {

                    center?.invoke()

                }

            }

            hasTitleInCenter -> {

                TaskFlowTopBarTitleText(

                    text = title.orEmpty(),

                    modifier = Modifier

                        .fillMaxWidth()

                        .align(Alignment.Center),

                    textAlign = TextAlign.Center,

                )

            }

            hasCenterContent -> Unit

        }

    }

}



@Composable

private fun TaskFlowTopBarTitleText(

    text: String,

    modifier: Modifier = Modifier,

    textAlign: TextAlign? = null,

) {

    Text(

        text = text,

        modifier = modifier,

        style = MaterialTheme.typography.titleLarge,

        color = MaterialTheme.colorScheme.onSurface,

        maxLines = 1,

        overflow = TextOverflow.Ellipsis,

        textAlign = textAlign,

    )

}


