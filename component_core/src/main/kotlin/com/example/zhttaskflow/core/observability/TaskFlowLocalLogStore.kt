package com.example.zhttaskflow.core.observability

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 自研可观测本地日志仓：按天滚动 JSON Lines 文件，保留 7 天，完全离线。
 */
object TaskFlowLocalLogStore {

    private const val ROOT_DIR_NAME: String = "taskflow_observability"
    private const val LOGS_DIR_NAME: String = "logs"
    private const val LAST_CRASH_FILE_NAME: String = "last_crash.jsonl"
    private const val RETENTION_DAYS: Int = 7
    private const val DATE_PATTERN: String = "yyyy-MM-dd"
    private const val TIMESTAMP_PATTERN: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    enum class LogType(val wireName: String, val displayName: String) {
        ANALYTICS("analytics", "埋点"),
        PERFORMANCE("performance", "性能"),
        CRASH("crash", "崩溃"),
    }

    enum class ObservabilityChannel {
        ANALYTICS,
        PERFORMANCE,
        CRASH,
    }

    data class LogRecord(
        val timestampEpochMs: Long,
        val logType: LogType,
        val pageId: String,
        val actionId: String,
        val event: String,
        val params: Map<String, String>,
        val stackTrace: String? = null,
        val deviceInfo: String? = null,
        val anomaly: Boolean = false,
    ) {
        fun stableId(): String = "${timestampEpochMs}_${logType.wireName}_${event}_${actionId}"
    }

    data class QueryFilter(
        val logType: LogType? = null,
        val pageId: String? = null,
        val sinceEpochMs: Long? = null,
        val maxEntries: Int = 2_000,
        val anomaliesOnly: Boolean = false,
    )

    private val appContextRef = AtomicReference<Context?>(null)
    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "TaskFlow-LocalLogStore").apply { isDaemon = true }
    }

    @Volatile
    private var lastKnownPageId: String? = null

    @Volatile
    private var lastCrashRecord: LogRecord? = null

    fun init(applicationContext: Context) {
        appContextRef.set(applicationContext.applicationContext)
        ioExecutor.execute {
            purgeExpiredLogs()
            lastCrashRecord = readLastCrashFile()
        }
    }

    fun updateLastKnownPageId(pageId: String?) {
        if (!pageId.isNullOrBlank()) {
            lastKnownPageId = pageId
        }
    }

    fun lastKnownPageId(): String? = lastKnownPageId

    fun peekLastCrash(): LogRecord? = lastCrashRecord

    fun append(record: LogRecord) {
        ioExecutor.execute {
            val context = appContextRef.get() ?: return@execute
            purgeExpiredLogs()
            val line = encodeRecord(record)
            val dayFile = logFileForDay(context, dayKey(record.timestampEpochMs))
            dayFile.parentFile?.mkdirs()
            FileOutputStream(dayFile, true).use { output ->
                output.write(line.toByteArray(StandardCharsets.UTF_8))
                output.write('\n'.code)
            }
            if (record.logType == LogType.CRASH) {
                writeLastCrashFile(context, line)
                lastCrashRecord = record
            }
        }
    }

    /**
     * 查询日志，默认按 [LogRecord.timestampEpochMs] 倒序。
     */
    fun query(filter: QueryFilter = QueryFilter()): List<LogRecord> {
        val context = appContextRef.get() ?: return emptyList()
        val results = mutableListOf<LogRecord>()
        listLogFiles(context).forEach { file ->
            file.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                reader.lineSequence().forEach { line ->
                    val record = decodeRecord(line) ?: return@forEach
                    if (matchesFilter(record, filter)) {
                        results.add(record)
                    }
                }
            }
        }
        return results
            .sortedByDescending { record -> record.timestampEpochMs }
            .take(filter.maxEntries)
    }

    /**
     * 清空本地全部日志文件（含 last_crash）。
     */
    fun clearAllLogs() {
        val context = appContextRef.get() ?: return
        listLogFiles(context).forEach { file -> file.delete() }
        File(rootDir(context), LAST_CRASH_FILE_NAME).delete()
        lastCrashRecord = null
    }

    fun exportRecentLogs(context: Context, maxEntries: Int = 2_000): File {
        val records = query(QueryFilter(maxEntries = maxEntries))
        val exportDir = File(context.cacheDir, "observability_export").apply { mkdirs() }
        val exportFile = File(exportDir, "taskflow_observability_export_${System.currentTimeMillis()}.jsonl")
        OutputStreamWriter(FileOutputStream(exportFile), StandardCharsets.UTF_8).use { writer ->
            records.forEach { record ->
                writer.write(encodeRecord(record))
                writer.write("\n")
            }
        }
        return exportFile
    }

    fun recordFromObservabilityEmit(
        channel: ObservabilityChannel,
        eventOrMetric: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>?,
        stackTrace: String? = null,
        deviceInfo: String? = null,
        anomaly: Boolean = false,
    ) {
        val logType = when (channel) {
            ObservabilityChannel.ANALYTICS -> LogType.ANALYTICS
            ObservabilityChannel.PERFORMANCE -> LogType.PERFORMANCE
            ObservabilityChannel.CRASH -> LogType.CRASH
        }
        val normalizedParams = params
            ?.mapNotNull { (key, value) -> value?.let { key to it } }
            ?.toMap()
            .orEmpty()
        val resolvedPageId = pageId?.takeIf { it.isNotBlank() }
            ?: lastKnownPageId.orEmpty()
        val resolvedActionId = actionId?.takeIf { it.isNotBlank() }.orEmpty()
        append(
            LogRecord(
                timestampEpochMs = System.currentTimeMillis(),
                logType = logType,
                pageId = resolvedPageId,
                actionId = resolvedActionId,
                event = eventOrMetric,
                params = normalizedParams,
                stackTrace = stackTrace,
                deviceInfo = deviceInfo ?: defaultDeviceInfo(),
                anomaly = anomaly,
            ),
        )
    }

    fun formatTimestamp(epochMs: Long): String {
        return SimpleDateFormat(TIMESTAMP_PATTERN, Locale.getDefault()).format(Date(epochMs))
    }

    private fun matchesFilter(record: LogRecord, filter: QueryFilter): Boolean {
        if (filter.logType != null && record.logType != filter.logType) {
            return false
        }
        if (!filter.pageId.isNullOrBlank() && record.pageId != filter.pageId) {
            return false
        }
        if (filter.sinceEpochMs != null && record.timestampEpochMs < filter.sinceEpochMs) {
            return false
        }
        if (filter.anomaliesOnly && !record.anomaly) {
            return false
        }
        return true
    }

    private fun defaultDeviceInfo(): String {
        return buildString {
            append("brand=${Build.BRAND}")
            append(";model=${Build.MODEL}")
            append(";sdk=${Build.VERSION.SDK_INT}")
            append(";release=${Build.VERSION.RELEASE}")
        }
    }

    private fun dayKey(epochMs: Long): String {
        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)
        return formatter.format(Date(epochMs))
    }

    private fun logFileForDay(context: Context, day: String): File {
        return File(File(rootDir(context), LOGS_DIR_NAME), "$day.jsonl")
    }

    private fun rootDir(context: Context): File {
        return File(context.filesDir, ROOT_DIR_NAME)
    }

    private fun listLogFiles(context: Context): List<File> {
        val logsDir = File(rootDir(context), LOGS_DIR_NAME)
        if (!logsDir.exists()) {
            return emptyList()
        }
        return logsDir.listFiles { file -> file.isFile && file.name.endsWith(".jsonl") }.orEmpty().toList()
    }

    private fun purgeExpiredLogs() {
        val context = appContextRef.get() ?: return
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
        val cutoffDay = dayKey(cutoff)
        listLogFiles(context).forEach { file ->
            val day = file.name.removeSuffix(".jsonl")
            if (day < cutoffDay) {
                file.delete()
            }
        }
    }

    private fun writeLastCrashFile(context: Context, line: String) {
        val file = File(rootDir(context), LAST_CRASH_FILE_NAME)
        file.parentFile?.mkdirs()
        file.writeText(line, StandardCharsets.UTF_8)
    }

    private fun readLastCrashFile(): LogRecord? {
        val context = appContextRef.get() ?: return null
        val file = File(rootDir(context), LAST_CRASH_FILE_NAME)
        if (!file.exists()) {
            return null
        }
        val line = file.readText(StandardCharsets.UTF_8).lineSequence().firstOrNull() ?: return null
        return decodeRecord(line)
    }

    fun encodeRecord(record: LogRecord): String {
        val json = JSONObject()
        json.put("timestamp", record.timestampEpochMs)
        json.put("timestampIso", formatIso(record.timestampEpochMs))
        json.put("logType", record.logType.wireName)
        json.put("logTypeLabel", record.logType.displayName)
        json.put("pageId", record.pageId)
        json.put("actionId", record.actionId)
        json.put("event", record.event)
        json.put("anomaly", record.anomaly)
        val paramsJson = JSONObject()
        record.params.forEach { (key, value) -> paramsJson.put(key, value) }
        json.put("params", paramsJson)
        if (!record.stackTrace.isNullOrBlank()) {
            json.put("stackTrace", record.stackTrace)
        }
        if (!record.deviceInfo.isNullOrBlank()) {
            json.put("deviceInfo", record.deviceInfo)
        }
        return json.toString()
    }

    private fun decodeRecord(line: String): LogRecord? {
        if (line.isBlank()) {
            return null
        }
        return runCatching {
            val json = JSONObject(line)
            val typeWire = json.optString("logType")
            val logType = LogType.entries.firstOrNull { it.wireName == typeWire } ?: LogType.ANALYTICS
            val paramsJson = json.optJSONObject("params") ?: JSONObject()
            val params = buildMap {
                paramsJson.keys().forEach { key ->
                    put(key, paramsJson.optString(key))
                }
            }
            LogRecord(
                timestampEpochMs = json.optLong("timestamp"),
                logType = logType,
                pageId = json.optString("pageId"),
                actionId = json.optString("actionId"),
                event = json.optString("event"),
                params = params,
                stackTrace = json.optString("stackTrace").takeIf { it.isNotBlank() },
                deviceInfo = json.optString("deviceInfo").takeIf { it.isNotBlank() },
                anomaly = json.optBoolean("anomaly"),
            )
        }.getOrNull()
    }

    private fun formatIso(epochMs: Long): String {
        return SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).format(Date(epochMs))
    }
}
