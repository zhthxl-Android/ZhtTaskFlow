package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.zhttaskflow.base.R

/**
 * 全屏半透明阻塞加载层：遮罩拦截点击，居中展示圆角卡片与 [CircularProgressIndicator]。
 */
@Composable
fun TaskFlowBlockingLoadingOverlay(
    visible: Boolean,
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (!visible) {
        return
    }
    val displayMessage = message?.takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.base_str_loading_default)
    val scrimInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = TaskFlowUiConstants.LoadingScrimAlpha))
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(TaskFlowUiConstants.LoadingDialogCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = TaskFlowUiConstants.LoadingDialogElevation,
            ),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = TaskFlowUiConstants.LoadingDialogContentPaddingHorizontal,
                    vertical = TaskFlowUiConstants.LoadingDialogContentPaddingVertical,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.LoadingDialogContentSpacing),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(TaskFlowUiConstants.LoadingDialogProgressSize),
                    strokeWidth = TaskFlowUiConstants.LoadingDialogProgressStrokeWidth,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = displayMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = TaskFlowUiConstants.CompactBodyTextSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
