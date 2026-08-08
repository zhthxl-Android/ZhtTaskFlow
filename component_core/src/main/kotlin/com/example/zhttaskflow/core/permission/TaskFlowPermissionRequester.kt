package com.example.zhttaskflow.core.permission

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 权限请求封装：在 Activity 中手动创建并持有，无 DI。
 */
class TaskFlowPermissionRequester(
    private val activity: ComponentActivity,
) {
    private var onResult: ((Boolean) -> Unit)? = null

    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onResult?.invoke(granted)
            onResult = null
        }

    /**
     * 请求单个权限。
     */
    fun request(permission: String, onResult: (Boolean) -> Unit) {
        if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }
        this.onResult = onResult
        launcher.launch(permission)
    }
}
