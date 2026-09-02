package com.example.zhttaskflow.base.ui.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants

/**
 * 错误态全屏展示，支持重试回调。
 *
 * @param message 错误说明
 * @param onRetry 点击重试
 */
@Composable
fun BaseErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TaskFlowUiConstants.StateScreenContentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = TaskFlowUiConstants.StateScreenActionTopSpacing),
        ) {
            Text(text = stringResource(id = R.string.base_str_retry))
        }
    }
}
