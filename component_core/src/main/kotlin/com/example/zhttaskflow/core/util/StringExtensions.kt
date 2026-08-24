package com.example.zhttaskflow.core.util

/**
 * null 转为 `""`；非 null 时原样返回（不裁剪空白，裁剪请用 [blankToEmpty]）。
 */
fun String?.orEmpty(): String = this ?: ""

/**
 * null、空串、仅空白字符统一转为 `""`；有有效文本时返回 trim 后的内容。
 */
fun String?.blankToEmpty(): String {
    val trimmed = this?.trim()
    return if (trimmed.isNullOrEmpty()) {
        ""
    } else {
        trimmed
    }
}

/**
 * null、空串、仅空白统一转为 null；有有效文本时返回 trim 后的字符串。
 */
fun String?.nullIfBlank(): String? {
    val trimmed = this?.trim()
    return trimmed?.takeIf { it.isNotEmpty() }
}

/** 非 null 且包含非空白字符。 */
fun String?.isNotNullOrBlank(): Boolean = !this.isNullOrBlank()

/** 非 null 且长度大于 0（空白字符也算有内容）。 */
fun String?.isNotNullOrEmpty(): Boolean = !this.isNullOrEmpty()
