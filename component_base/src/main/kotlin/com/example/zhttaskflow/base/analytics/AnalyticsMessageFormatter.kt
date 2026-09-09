package com.example.zhttaskflow.base.analytics

/**
 * 交互日志单行格式（与历史交互埋点字符串一致）。
 */
internal object AnalyticsMessageFormatter {

    fun formatInteraction(
        action: String,
        operationId: String,
        pageId: String? = null,
        params: Map<String, String?>? = null,
        detail: String? = null,
    ): String {
        val parts = buildList {
            add("action=$action")
            if (!pageId.isNullOrBlank()) {
                add("pageId=$pageId")
            }
            add("actionId=$operationId")
            val snapshot = formatParamsSnapshot(params = params, detail = detail)
            if (snapshot.isNotBlank()) {
                add("params=$snapshot")
            }
        }
        return parts.joinToString(separator = " ")
    }

    private fun formatParamsSnapshot(
        params: Map<String, String?>?,
        detail: String?,
    ): String {
        val fromMap = params
            ?.entries
            ?.mapNotNull { (key, value) ->
                value?.let { safeValue -> "$key=$safeValue" }
            }
            ?.joinToString(separator = ",")
        return when {
            !fromMap.isNullOrBlank() && !detail.isNullOrBlank() -> "$fromMap,$detail"
            !fromMap.isNullOrBlank() -> fromMap
            !detail.isNullOrBlank() -> detail
            else -> ""
        }
    }
}
