package com.example.zhttaskflow.base.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.R

/**
 * 执行页面返回：先走 [onBackIntercept]，未消费时再 [onNavigateUp]。
 */
fun handlePageBack(
    onNavigateUp: () -> Unit,
    onBackIntercept: (() -> Boolean)?,
) {
    val intercepted = onBackIntercept?.invoke() == true
    if (!intercepted) {
        onNavigateUp()
    }
}

/**
 * 二级页系统返回键 / 手势返回统一拦截。
 */
@Composable
fun PageBackHandler(
    onNavigateUp: () -> Unit,
    onBackIntercept: (() -> Boolean)? = null,
    enabled: Boolean = true,
) {
    val currentNavigateUp = rememberUpdatedState(onNavigateUp)
    val currentIntercept = rememberUpdatedState(onBackIntercept)
    BackHandler(enabled = enabled) {
        handlePageBack(
            onNavigateUp = { currentNavigateUp.value() },
            onBackIntercept = currentIntercept.value,
        )
    }
}

/**
 * 一级 Tab 根页：系统返回退到桌面，避免误触直接退出应用。
 */
@Composable
fun TabRootBackHandler(
    enabled: Boolean = true,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentOnBack = rememberUpdatedState(onBack)
    BackHandler(enabled = enabled) {
        val handler = currentOnBack.value
        if (handler != null) {
            handler()
        } else {
            context.findActivity()?.moveTaskToBack(true)
        }
    }
}

/**
 * Activity 退到桌面（不 finish）。
 */
fun Activity.moveTaskToDesktop() {
    moveTaskToBack(true)
}

/**
 * 未保存内容时拦截返回：弹出全局确认框，确认后执行 [onConfirmExit]。
 */
@Composable
fun rememberUnsavedBackInterceptor(
    hasUnsavedChanges: Boolean,
    onConfirmExit: () -> Unit,
): () -> Boolean {
    val dialogController = rememberDialogController()
    val title = stringResource(id = R.string.base_str_unsaved_exit_title)
    val message = stringResource(id = R.string.base_str_unsaved_exit_message)
    val exitLabel = stringResource(id = R.string.base_str_exit)
    val currentHasUnsaved = rememberUpdatedState(hasUnsavedChanges)
    val currentOnConfirmExit = rememberUpdatedState(onConfirmExit)
    return remember(dialogController, title, message, exitLabel) {
        {
            if (!currentHasUnsaved.value) {
                false
            } else {
                showUnsavedChangesExitConfirmDialog(
                    controller = dialogController,
                    title = title,
                    message = message,
                    confirmText = exitLabel,
                    onConfirmExit = { currentOnConfirmExit.value() },
                )
                true
            }
        }
    }
}

/**
 * 「内容未保存，是否退出」确认弹窗（样式走 [DialogController]）。
 */
fun showUnsavedChangesExitConfirmDialog(
    controller: DialogController,
    onConfirmExit: () -> Unit,
    title: String,
    message: String,
    confirmText: String? = null,
    dismissText: String? = null,
) {
    showConfirmDialog(
        controller = controller,
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirmExit,
    )
}

internal tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
