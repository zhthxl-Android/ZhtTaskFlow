package com.example.zhttaskflow.base.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

/**
 * 在 Compose 中生命周期安全地收集 [StateFlow] 状态。
 */
@Composable
fun <T> StateFlow<T>.collectUiStateWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
): State<T> = collectAsStateWithLifecycle(minActiveState = minActiveState)
