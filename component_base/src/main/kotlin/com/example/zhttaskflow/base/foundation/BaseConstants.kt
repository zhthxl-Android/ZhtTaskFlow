package com.example.zhttaskflow.base.foundation

/**
 * 全局常量基类：各模块可继承并扩展模块级常量，避免魔法值散落。
 */
abstract class BaseConstants {
    companion object {
        const val EMPTY_STRING: String = ""
        const val NETWORK_TIMEOUT_SECONDS: Long = 30L
    }
}
