package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** 列表上滑收起顶栏 / 状态栏占位、下滑恢复（供 [ListScaffold] 使用）。 */
@Stable
class CollapsibleTopBarState(
    maxCollapsePx: Float,
) {
    var maxCollapsePx: Float = maxCollapsePx
        internal set(value) {
            field = value.coerceAtLeast(0f)
            offsetPx = offsetPx.coerceIn(-field, 0f)
        }

    private var _offsetPx = mutableFloatStateOf(0f)

    var offsetPx: Float
        get() = _offsetPx.floatValue
        set(value) {
            _offsetPx.floatValue = value.coerceIn(-maxCollapsePx, 0f)
        }

    val collapseProgress: Float
        get() = if (maxCollapsePx <= 0f) {
            0f
        } else {
            (-offsetPx / maxCollapsePx).coerceIn(0f, 1f)
        }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (maxCollapsePx <= 0f) return Offset.Zero
            val delta = available.y
            if (delta == 0f) return Offset.Zero
            val previous = offsetPx
            offsetPx = previous + delta
            return Offset(0f, offsetPx - previous)
        }
    }
}

@Composable
fun rememberCollapsibleTopBarState(
    enabled: Boolean,
    topBarContentHeight: Dp,
    includeStatusBarInset: Boolean = true,
): CollapsibleTopBarState {
    val density = LocalDensity.current
    val statusBarTopPx = if (includeStatusBarInset) {
        WindowInsets.statusBars.getTop(density).toFloat()
    } else {
        0f
    }
    val topBarPx = with(density) { topBarContentHeight.toPx() }
    val maxCollapsePx = if (enabled) {
        statusBarTopPx + topBarPx
    } else {
        0f
    }
    return remember(enabled, includeStatusBarInset) {
        CollapsibleTopBarState(maxCollapsePx = maxCollapsePx)
    }.also { state ->
        state.maxCollapsePx = maxCollapsePx
        if (!enabled) {
            state.offsetPx = 0f
        }
    }
}

internal fun CollapsibleTopBarState.visibleHeaderHeightPx(): Float {
    return (maxCollapsePx + offsetPx).coerceAtLeast(0f)
}

internal fun CollapsibleTopBarState.headerOffsetPx(): Int = offsetPx.roundToInt()
