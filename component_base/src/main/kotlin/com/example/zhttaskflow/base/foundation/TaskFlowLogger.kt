package com.example.zhttaskflow.base.foundation

import android.util.Log

/**
 * 全局日志门面，统一 Tag 前缀，供各层记录诊断信息。
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
