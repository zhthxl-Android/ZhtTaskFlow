package com.example.zhttaskflow.base.extension

/**
 * 字符串通用工具，比如判空、格式化、正则校验等通用能力
 */


/**
 * 字符串为空或仅空白时返回 null，便于链式处理。
 */
fun String?.orNullIfBlank(): String? = if (this.isNullOrBlank()) null else this

/**
 * 安全截取子串，避免越界。
 */
fun String.safeSubstring(maxLength: Int): String {
    if (maxLength <= 0) return ""
    return if (length <= maxLength) this else substring(0, maxLength)
}
