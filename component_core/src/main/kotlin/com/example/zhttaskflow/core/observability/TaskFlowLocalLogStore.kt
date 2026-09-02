package com.example.zhttaskflow.core.observability

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.FileInputStream
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
    private const val INDEX_SUFFIX: String = ".idx"
    private const val INDEX_FIELD_SEPARATOR: String = "\t"

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

    /**
     * 分页查询条件（页码从 0 开始）。
     */
    data class PagedQueryFilter(
        val logType: LogType? = null,
        val pageId: String? = null,
        val sinceEpochMs: Long? = null,
        val anomaliesOnly: Boolean = false,
        val page: Int = 0,
        val pageSize: Int = 50,
    )

    data class PagedQueryResult(
        val records: List<LogRecord>,
        val hasMore: Boolean,
    )

    private data class LogIndexEntry(
        val logFile: File,
        val byteOffset: Long,
        val timestampEpochMs: Long,
        val logType: LogType,
        val anomaly: Boolean,
        val pageId: String,
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
            val startOffset = dayFile.length()
            FileOutputStream(dayFile, true).use { output ->
                output.write(line.toByteArray(StandardCharsets.UTF_8))
                output.write('\n'.code)
            }
            appendIndexEntry(dayFile, startOffset, record)
            if (record.logType == LogType.CRASH) {
                writeLastCrashFile(context, line)
                lastCrashRecord = record
            }
        }
    }

    /**
     * 查询日志，按时间倒序，最多 [QueryFilter.maxEntries] 条（基于索引，不全量解析 JSONL）。
     */
    fun query(filter: QueryFilter = QueryFilter()): List<LogRecord> {
        val pageSize = filter.maxEntries.coerceAtLeast(1)
        return queryPaged(
            PagedQueryFilter(
                logType = filter.logType,
                pageId = filter.pageId,
                sinceEpochMs = filter.sinceEpochMs,
                anomaliesOnly = filter.anomaliesOnly,
                page = 0,
                pageSize = pageSize,
            ),
        ).records
    }

    /**
     * 按页查询日志：先读侧车索引筛选排序，再按字节偏移只解析当前页对应行。
     */
    fun queryPaged(filter: PagedQueryFilter): PagedQueryResult {
        val context = appContextRef.get() ?: return PagedQueryResult(emptyList(), hasMore = false)
        val page = filter.page.coerceAtLeast(0)
        val pageSize = filter.pageSize.coerceIn(1, 500)
        val indexEntries = loadFilteredIndexEntries(context, filter)
        val fromIndex = page * pageSize
        if (fromIndex >= indexEntries.size) {
            return PagedQueryResult(emptyList(), hasMore = false)
        }
        val toIndex = minOf(fromIndex + pageSize, indexEntries.size)
        val slice = indexEntries.subList(fromIndex, toIndex)
        val records = slice.mapNotNull { entry -> readRecordAt(entry.logFile, entry.byteOffset) }
        val hasMore = toIndex < indexEntries.size
        return PagedQueryResult(records = records, hasMore = hasMore)
    }

    /**
     * 清空本地全部日志文件（含 last_crash）。
     */
    fun clearAllLogs() {
        val context = appContextRef.get() ?: return
        listLogFiles(context).forEach { file ->
            file.delete()
            indexFileFor(file).delete()
        }
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
                indexFileFor(file).delete()
            }
        }
    }

    private fun indexFileFor(logFile: File): File {
        return File(logFile.parentFile, logFile.name + INDEX_SUFFIX)
    }

    private fun appendIndexEntry(logFile: File, byteOffset: Long, record: LogRecord) {
        val indexFile = indexFileFor(logFile)
        indexFile.parentFile?.mkdirs()
        FileOutputStream(indexFile, true).use { output ->
            output.write(formatIndexLine(byteOffset, record).toByteArray(StandardCharsets.UTF_8))
            output.write('\n'.code)
        }
    }

    private fun formatIndexLine(byteOffset: Long, record: LogRecord): String {
        val safePageId = record.pageId.replace(INDEX_FIELD_SEPARATOR, " ")
        return buildString {
            append(byteOffset)
            append(INDEX_FIELD_SEPARATOR)
            append(record.timestampEpochMs)
            append(INDEX_FIELD_SEPARATOR)
            append(record.logType.wireName)
            append(INDEX_FIELD_SEPARATOR)
            append(if (record.anomaly) "1" else "0")
            append(INDEX_FIELD_SEPARATOR)
            append(safePageId)
        }
    }

    private fun parseIndexLine(logFile: File, line: String): LogIndexEntry? {
        if (line.isBlank()) {
            return null
        }
        val parts = line.split(INDEX_FIELD_SEPARATOR, limit = 5)
        if (parts.size < 5) {
            return null
        }
        val offset = parts[0].toLongOrNull() ?: return null
        val timestamp = parts[1].toLongOrNull() ?: return null
        val typeWire = parts[2]
        val logType = LogType.entries.firstOrNull { it.wireName == typeWire } ?: LogType.ANALYTICS
        val anomaly = parts[3] == "1"
        val pageId = parts[4]
        return LogIndexEntry(
            logFile = logFile,
            byteOffset = offset,
            timestampEpochMs = timestamp,
            logType = logType,
            anomaly = anomaly,
            pageId = pageId,
        )
    }

    private fun ensureIndexForLogFile(logFile: File) {
        val indexFile = indexFileFor(logFile)
        if (!logFile.exists()) {
            indexFile.delete()
            return
        }
        if (indexFile.exists() && indexFile.lastModified() >= logFile.lastModified()) {
            return
        }
        rebuildIndex(logFile)
    }

    private fun rebuildIndex(logFile: File) {
        val indexFile = indexFileFor(logFile)
        if (!logFile.exists()) {
            indexFile.delete()
            return
        }
        val bytes = logFile.readBytes()
        indexFile.parentFile?.mkdirs()
        FileOutputStream(indexFile, false).use { output ->
            var lineStart = 0
            while (lineStart < bytes.size) {
                var lineEnd = lineStart
                while (lineEnd < bytes.size && bytes[lineEnd] != '\n'.code.toByte()) {
                    lineEnd++
                }
                val lineBytes = bytes.copyOfRange(lineStart, lineEnd)
                val line = lineBytes.toString(StandardCharsets.UTF_8)
                val nextStart = if (lineEnd < bytes.size) lineEnd + 1 else bytes.size
                if (line.isNotBlank()) {
                    val record = decodeRecord(line)
                    if (record != null) {
                        output.write(formatIndexLine(lineStart.toLong(), record).toByteArray(StandardCharsets.UTF_8))
                        output.write('\n'.code)
                    }
                }
                lineStart = nextStart
            }
        }
    }

    private fun loadFilteredIndexEntries(
        context: Context,
        filter: PagedQueryFilter,
    ): List<LogIndexEntry> {
        val entries = mutableListOf<LogIndexEntry>()
        listLogFiles(context).forEach { logFile ->
            ensureIndexForLogFile(logFile)
            val indexFile = indexFileFor(logFile)
            if (!indexFile.exists()) {
                return@forEach
            }
            indexFile.forEachLine { line ->
                val entry = parseIndexLine(logFile, line) ?: return@forEachLine
                if (matchesIndexFilter(entry, filter)) {
                    entries.add(entry)
                }
            }
        }
        return entries.sortedByDescending { entry -> entry.timestampEpochMs }
    }

    private fun matchesIndexFilter(entry: LogIndexEntry, filter: PagedQueryFilter): Boolean {
        if (filter.logType != null && entry.logType != filter.logType) {
            return false
        }
        if (!filter.pageId.isNullOrBlank() && entry.pageId != filter.pageId) {
            return false
        }
        if (filter.sinceEpochMs != null && entry.timestampEpochMs < filter.sinceEpochMs) {
            return false
        }
        if (filter.anomaliesOnly && !entry.anomaly) {
            return false
        }
        return true
    }

    private fun readRecordAt(logFile: File, byteOffset: Long): LogRecord? {
        if (!logFile.exists()) {
            return null
        }
        return runCatching {
            FileInputStream(logFile).use { input ->
                input.channel.position(byteOffset)
                input.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val line = reader.readLine() ?: return null
                    decodeRecord(line)
                }
            }
        }.getOrNull()
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
