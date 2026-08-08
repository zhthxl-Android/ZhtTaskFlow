package com.example.zhttaskflow.base.foundation

import android.util.Log

/**
 * 轻量日志工具：统一 TAG 前缀，业务阶段可替换为正式日志框架。
 */
object TaskFlowLogger {

    private const val GLOBAL_TAG_PREFIX = "TaskFlow"

    fun d(tag: String, message: String) {
        Log.d("$GLOBAL_TAG_PREFIX/$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$GLOBAL_TAG_PREFIX/$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$GLOBAL_TAG_PREFIX/$tag", message, throwable)
        } else {
            Log.w("$GLOBAL_TAG_PREFIX/$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$GLOBAL_TAG_PREFIX/$tag", message, throwable)
        } else {
            Log.e("$GLOBAL_TAG_PREFIX/$tag", message)
        }
    }
}
