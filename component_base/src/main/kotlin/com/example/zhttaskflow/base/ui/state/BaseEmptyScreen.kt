package com.example.zhttaskflow.base.ui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.R

/**
 * 空数据占位页。
 */
@Composable
fun BaseEmptyScreen(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.base_str_empty),
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}
