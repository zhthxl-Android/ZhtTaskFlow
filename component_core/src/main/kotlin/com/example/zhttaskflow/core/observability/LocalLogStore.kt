package com.example.zhttaskflow.core.observability

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


/**
 * 自研可观测本地日志仓：按天滚动 JSON Lines 文件，保留 7 天，完全离线。
 */
object LocalLogStore {

    //根目录名 `taskflow_observability`，存在 `context.filesDir` 下
    private const val ROOT_DIR_NAME: String = "taskflow_observability"

    //日志子目录 `logs`
    private const val LOGS_DIR_NAME: String = "logs"

    //文件类型
    private const val JSONL_SUFFIX = ".jsonl"

    //最近一次崩溃单独存 `last_crash.jsonl`
    private const val LAST_CRASH_FILE_NAME: String = "last_crash$JSONL_SUFFIX"

    //日志保留 7 天
    private const val RETENTION_DAYS: Int = 7

    //日志文件名格式 `yyyy-MM-dd.jsonl`
    private const val DATE_PATTERN: String = "yyyy-MM-dd"

    //ISO 时间戳格式
    private const val TIMESTAMP_PATTERN: String = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    //索引文件后缀
    private const val INDEX_SUFFIX: String = ".idx"
    //索引文件内容 分割符
    private const val INDEX_FIELD_SEPARATOR: String = "\t"

    /**
     * 日志类型的枚举类
     * 用于定义不同类型的日志及其对应的显示名称
     */
    enum class LogType(
        val wireName: String,//写入文件的英文名
        val displayName: String//中文展示名
    ) {
        // 埋点日志类型
        ANALYTICS(
            "analytics",
            "埋点"
        ),

        // 性能日志类型
        PERFORMANCE(
            "performance",
            "性能"
        ),

        // 崩溃日志类型
        CRASH(
            "crash",
            "崩溃"
        ),
    }

    /**
     * 上层调用时的通道枚举，和 `LogType` 一一对应，
     * 用于 `recordFromObservabilityEmit()` 做转换。
     * */
    enum class ObservabilityChannel {
        ANALYTICS,
        PERFORMANCE,
        CRASH,
    }

    /**
     * 日志记录的数据类（一条日志）
     * 包含日志记录的各个字段，如时间戳、日志类型、页面 ID、动作 ID、事件名、附加参数等
     */
    data class LogRecord(
        val timestampEpochMs: Long,         //时间戳（毫秒）
        val logType: LogType,               //日志类型
        val pageId: String,                 //所属页面 ID
        val actionId: String,               //动作 ID（按钮点击等）
        val event: String,                  //事件名
        val params: Map<String, String>,    //附加参数（键值对）
        val stackTrace: String? = null,     //崩溃堆栈（可选）
        val deviceInfo: String? = null,     //设备信息（可选）
        val anomaly: Boolean = false,       //是否异常标记
    ) {
        //日志记录的稳定 ID，用于唯一标识日志记录
        //时间戳_类型_事件_动作
        fun stableId(): String = "${timestampEpochMs}_${logType.wireName}_${event}_${actionId}"
    }

    /**
     * 日志查询过滤条件的数据类
     * 包含日志类型、页面 ID、时间戳、最大条数、是否只查询异常日志等
     */
    data class QueryFilter(
        val logType: LogType? = null,//按日志类型过滤，null=不过滤
        val pageId: String? = null,//按页面 ID 过滤，null=不过滤
        val sinceEpochMs: Long? = null,//按时间戳过滤，null=不限制
        val maxEntries: Int = 2_000,//最大条数
        val anomaliesOnly: Boolean = false,//是否只查询异常日志
    )

    /**
     * 日志分页查询过滤条件的数据类
     * 包含日志类型、页面 ID、时间戳、是否只查询异常日志、页码和每页条数等
     */
    data class PagedQueryFilter(
        val logType: LogType? = null,// 按日志类型过滤，null=不过滤
        val pageId: String? = null,// 按页面ID过滤，null=不过滤
        val sinceEpochMs: Long? = null,// 只返回该时间戳之后的日志，null=不限制
        val anomaliesOnly: Boolean = false,// true=只看异常日志
        val page: Int = 0,// 页码，从0开始
        val pageSize: Int = 50,// 每页条数，默认50
    )

    /**
     * 日志分页查询结果的数据类
     * 包含日志记录列表和是否有更多日志标记
     */
    data class PagedQueryResult(
        val records: List<LogRecord>,//当前页的日志列表
        val hasMore: Boolean,//是否还有下一页
    )

    /**
     * 日志索引条目的数据类（内部用）
     * 包含日志文件、字节偏移量、时间戳、日志类型、是否异常标记和页面 ID
     * 索引条目，对应 `.idx` 文件里的一行，记录日志文件、字节偏移、时间戳等，
     * 查询时先扫这个，再按需读 JSONL。
     */
    private data class LogIndexEntry(
        val logFile: File,//所属正文文件
        val byteOffset: Long,//在正文中的字节偏移
        val timestampEpochMs: Long,//时间戳
        val logType: LogType,//日志类型
        val anomaly: Boolean,//是否异常
        val pageId: String,//页面 ID
    )

    //原子引用容器，管理上下文引用，
    // 只能使用Application Context，禁止使用其他context
    private val appContextRef = AtomicReference<Context?>(null)

    //单线程守护线程池，管理异步执行环境
    private val ioExecutor: ExecutorService = ThreadPoolExecutor(
        1,                                      //核心线程数
        1,                                  //最大线程数（保持单线程）
        0L,                                     // 空闲线程存活时间（单线程无意义，设0）
        TimeUnit.MILLISECONDS,                          // 时间单位
        LinkedBlockingQueue(1024),      // 有界队列，容量1024
        { runnable ->                           // 自定义 ThreadFactory
            Thread(
                runnable,
                "App-LocalLogStore"
            ).apply { isDaemon = true }
        },
        ThreadPoolExecutor.DiscardOldestPolicy()  // 拒绝策略：队列满时丢弃最旧任务
    )

    @Volatile
    private var lastKnownPageId: String? = null

    @Volatile
    private var lastCrashRecord: LogRecord? = null

    fun init(applicationContext: Context) {
        appContextRef.set(applicationContext.applicationContext)
        ioExecutor.execute {
            purgeExpiredLogs()//清理过期日志
            lastCrashRecord = readLastCrashFile()//读取上次崩溃记录到内存
        }
    }

    /**
     * 记录当前用户所在页面，后续埋点如果没传 pageId，就用这个兜底
     * */
    fun updateLastKnownPageId(pageId: String?) {
        if (!pageId.isNullOrBlank()) {
            lastKnownPageId = pageId
        }
    }

    fun lastKnownPageId(): String? = lastKnownPageId

    /**
     * 取内存中缓存的最近一次崩溃记录（启动时已读好）
     * */
    fun peekLastCrash(): LogRecord? = lastCrashRecord

    /**
     * 写入日志文件
     * */
    fun append(record: LogRecord) {
        ioExecutor.execute {
            val context = appContextRef.get() ?: return@execute
            //清理过期日志
            purgeExpiredLogs()
            //ogRecord转JSON 字符串
            val line = encodeRecord(record)
            //获取文件地址://filesDir/taskflow_observability/logs/2026-09-10.jsonl
            val dayFile = logFileForDay(
                context,
                dayKey(record.timestampEpochMs)
            )
            dayFile.parentFile?.mkdirs()
            //获取要写入的起始位置:当前文件末尾字节位置
            val startOffset = dayFile.length()
            //追加写JSON行 + 换行
            FileOutputStream(
                dayFile,
                true
            ).use { output ->
                output.write(line.toByteArray(Charsets.UTF_8))
                //末尾加换行
                output.write('\n'.code)
            }
            //同步写索引行到 .idx 文件
            appendIndexEntry(
                dayFile,
                startOffset,
                record
            )
            //崩溃日志额外落单文件
            if (record.logType == LogType.CRASH) {
                writeLastCrashFile(
                    context,
                    line
                )
                lastCrashRecord = record
            }
        }
    }

    /**
     * 查询日志，按时间倒序，最多 [QueryFilter.maxEntries] 条（基于索引，不全量解析 JSONL）。
     * @param filter 查询过滤条件,默认为空表示不过滤
     * @return 日志记录列表
     */
    fun query(filter: QueryFilter = QueryFilter()): List<LogRecord> {
        //确保至少 1 条
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
        val context = appContextRef.get() ?: return PagedQueryResult(
            emptyList(),
            hasMore = false
        )
        //页码不能为负，最小是 0
        val page = filter.page.coerceAtLeast(0)
        //页大小限制在 1~500 之间。小于 1 变 1，大于 500 变 500
        val pageSize = filter.pageSize.coerceIn(
            1,
            500
        )
        //一次性把满足过滤条件的全部条目加载出来
        val indexEntries = loadFilteredIndexEntries(
            context,
            filter
        )
        //计算当前页起始下标
        val fromIndex = page * pageSize
        //起始位置超过总条数，当前页无数据
        if (fromIndex >= indexEntries.size) {
            return PagedQueryResult(
                emptyList(),
                hasMore = false
            )
        }
        //计算当前页结束下标
        //如果结束下标超过总条数，则取总条数
        val toIndex = minOf(
            fromIndex + pageSize,
            indexEntries.size
        )
        //根据起始和结束下标截取当前页的条目
        val slice = indexEntries.subList(
            fromIndex,
            toIndex
        )
        //根据索引文件中的字节偏移量，逐行读取日志记录
        //丢弃null数据
        val records = slice.mapNotNull { entry ->
            readRecordAt(
                entry.logFile,
                entry.byteOffset
            )
        }
        val hasMore = toIndex < indexEntries.size
        return PagedQueryResult(
            records = records,
            hasMore = hasMore
        )
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
        File(
            rootDir(context),
            LAST_CRASH_FILE_NAME
        ).delete()
        lastCrashRecord = null
    }

    fun exportRecentLogs(
        context: Context,
        maxEntries: Int = 2_000
    ): File {
        return exportRecentLogs(
            context,
            QueryFilter(maxEntries = maxEntries),
            maxEntries
        )
    }

    /**
     * 导出日志（与列表筛选条件一致）
     * @param filter 日志筛选条件
     * @param maxEntries 导出条数限制
     * @return 导出后的 JSONL 文件
     */
    fun exportRecentLogs(
        context: Context,
        filter: QueryFilter,
        maxEntries: Int = 2_000
    ): File {
        //如果 maxEntries 小于 1，就强制变成 1
        val capped = maxEntries.coerceAtLeast(1)
        val records = query(filter.copy(maxEntries = capped))
        val exportDir = File(
            context.cacheDir,
            "observability_export"
        ).apply { mkdirs() }
        val exportFile = File(
            exportDir,
            "taskflow_observability_export_${System.currentTimeMillis()}$JSONL_SUFFIX"
        )
        OutputStreamWriter(
            FileOutputStream(exportFile),
            Charsets.UTF_8
        ).use { writer ->
            records.forEach { record ->
                writer.write(encodeRecord(record))
                //末尾加换行
                writer.write("\n")
            }
        }
        return exportFile
    }

    /**
     * 可观测性日志统一入口
     * @param channel 日志来源通道，决定最终 `LogType`（埋点 / 性能 / 崩溃）
     * @param eventOrMetric 事件名或指标名，如 `"button_click"`、`"frame_drop"`、`"native_crash"`
     * @param pageId 所属页面 ID；为 null 或空白时回退到 `lastKnownPageId`
     * @param actionId 动作 ID（按钮点击、滑动等），可为空
     * @param params 附加参数键值对；value 允许 null，内部会过滤掉 null
     * @param stackTrace 崩溃堆栈，仅 CRASH 通道通常有值，默认 null
     * @param deviceInfo 设备信息 JSON 串；为 null 时用 `defaultDeviceInfo()` 兜底
     * @param anomaly 是否异常标记，用于把 "非崩溃但可疑" 的日志单独标红，默认 false
     */
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
        //把外部枚举翻译成内部 `LogType`
        val logType = when (channel) {
            ObservabilityChannel.ANALYTICS -> LogType.ANALYTICS
            ObservabilityChannel.PERFORMANCE -> LogType.PERFORMANCE
            ObservabilityChannel.CRASH -> LogType.CRASH
        }
        //过滤value 为 null 的条目
        val normalizedParams = params
            ?.mapNotNull { (key, value) -> value?.let { key to it } }
            ?.toMap()
            .orEmpty()
        //没传（null / 空串 / 纯空格）就用 lastKnownPageId
        val resolvedPageId = pageId?.takeIf { it.isNotBlank() }
            ?: lastKnownPageId.orEmpty()
        //actionId保证不为null
        val resolvedActionId = actionId?.takeIf { it.isNotBlank() }.orEmpty()
        //写入日志文件
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

    /**
     * 把毫秒时间戳转成日期字符串:2026-09-10T14:30:45.123+0800（系统语言）
     * */
    fun formatTimestamp(epochMs: Long): String {
        return SimpleDateFormat(
            TIMESTAMP_PATTERN,
            Locale.US
        ).format(Date(epochMs))
    }

    private fun defaultDeviceInfo(): String {
        return buildString {
            append("brand=${Build.BRAND}")
            append(";model=${Build.MODEL}")
            append(";sdk=${Build.VERSION.SDK_INT}")
            append(";release=${Build.VERSION.RELEASE}")
        }
    }

    /**
     * 把毫秒时间戳转成日期字符串:2026-09-10（美式英语）
     * */
    private fun dayKey(epochMs: Long): String {
        val formatter = SimpleDateFormat(
            DATE_PATTERN,
            Locale.US
        )
        return formatter.format(Date(epochMs))
    }

    /**
     * 根据日期创建jsonl文件
     * */
    private fun logFileForDay(
        context: Context,
        day: String
    ): File {
        return File(
            File(
                rootDir(context),
                LOGS_DIR_NAME
            ),
            "$day$JSONL_SUFFIX"
        )
    }

    private fun rootDir(context: Context): File {
        return File(
            context.filesDir,
            ROOT_DIR_NAME
        )
    }

    /**
     * 返回所有 .jsonl 格式的日志文件列表
     * 如果目录不存在或没有符合条件的文件，则返回空列表。
     * */
    private fun listLogFiles(context: Context): List<File> {
        val logsDir = File(
            rootDir(context),
            LOGS_DIR_NAME
        )
        if (!logsDir.exists()) {
            return emptyList()
        }
        return logsDir.listFiles { file -> file.isFile && file.name.endsWith(JSONL_SUFFIX) }
            .orEmpty()
            .toList()
    }

    /**
     * 清理过期日志
     * */
    private fun purgeExpiredLogs() {
        val context = appContextRef.get() ?: return
        //当前时间减去设定好的过期时间
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
        //保留截止日
        val cutoffDay = dayKey(cutoff)
        listLogFiles(context).forEach { file ->
            val day = file.name.removeSuffix(JSONL_SUFFIX)
            //如果日志文件日期小于保留截止日，则删除
            if (day < cutoffDay) {
                file.delete()
                //删除对应的索引文件
                indexFileFor(file).delete()
            }
        }
    }

    /**
     * xxx.jsonl.idx
     * 命名规则是 `日志文件名 + .idx
     * */
    private fun indexFileFor(logFile: File): File {
        return File(
            logFile.parentFile,
            logFile.name + INDEX_SUFFIX
        )
    }

    /**
     * 生成索引（.idx）文件
     * @param logFile 日志文件地址
     * @param byteOffset 日志文件起始偏移量
     * @param record 日志记录
     * */
    private fun appendIndexEntry(
        logFile: File,
        byteOffset: Long,
        record: LogRecord
    ) {
        //2026-09-10.jsonl.idx
        val indexFile = indexFileFor(logFile)
        indexFile.parentFile?.mkdirs()
        FileOutputStream(
            indexFile,
            true
        ).use { output ->
            output.write(
                formatIndexLine(
                    byteOffset,
                    record
                ).toByteArray(Charsets.UTF_8)
            )
            output.write('\n'.code)
        }
    }

    /**
     * 将日志对象解码成索引字符串
     * 索引字符串是一个 纯文本分隔行,例如：1024 1757472000000 info 0 HomePage
     * @param byteOffset 日志文件起始偏移量
     * @param record 日志记录
     * */
    private fun formatIndexLine(
        byteOffset: Long,
        record: LogRecord
    ): String {
        //如果 `pageId` 里碰巧包含了分隔符（比如 ` `），会导致解析时列错位。
        // 所以把分隔符替换成空格，做清洗
        val safePageId = record.pageId.replace(
            INDEX_FIELD_SEPARATOR,
            " "
        )
        return buildString {
            append(byteOffset)//日志文件起始偏移量
            append(INDEX_FIELD_SEPARATOR)//分隔符
            append(record.timestampEpochMs)
            append(INDEX_FIELD_SEPARATOR)
            append(record.logType.wireName)
            append(INDEX_FIELD_SEPARATOR)
            append(if (record.anomaly) "1" else "0")
            append(INDEX_FIELD_SEPARATOR)
            append(safePageId)
        }
    }

    /**
     * 日志索引行解析
     * @param logFile 日志文件
     * @param line 索引文件行
     * @return 索引文件行解析结果
     * */
    private fun parseIndexLine(
        logFile: File,
        line: String
    ): LogIndexEntry? {
        if (line.isBlank()) {
            return null
        }
        //按照分割符最多切成5段
        val parts = line.split(
            INDEX_FIELD_SEPARATOR,
            limit = 5
        )
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

    /**
     * 索引文件安全检查
     * 确保索引文件存在和数据时效性
     * */
    private fun ensureIndexForLogFile(logFile: File) {
        //获取 jsonl 对应的 idx 文件
        val indexFile = indexFileFor(logFile)
        if (!logFile.exists()) {
            indexFile.delete()
            return
        }
        //如果 idx 文件存在，且最后修改时间大于等于 jsonl 文件最后修改时间，则不需要重建索引
        if (indexFile.exists() && indexFile.lastModified() >= logFile.lastModified()) {
            return
        }
        //重建索引文件
        rebuildIndex(logFile)
    }

    /**
     * 日志文件索引重建
     * 把日志文件从头到尾扫一遍，给每条能成功解码的记录，在索引文件里记下 "它在原文件第几个字节开始"。
     * @param logFile 日志文件(jsonl文件)
     * */
    private fun rebuildIndex(logFile: File) {
        //推导索引文件
        val indexFile = indexFileFor(logFile)
        if (!logFile.exists()) {
            indexFile.delete()
            return
        }
        val bytes = logFile.readBytes()
        indexFile.parentFile?.mkdirs()
        FileOutputStream(
            indexFile,
            false
        ).use { output ->
            //当前行在 `bytes` 中的起始字节下标
            var lineStart = 0
            //从 `lineStart` 开始往后找，直到文件末尾
            while (lineStart < bytes.size) {
                //当前行在 `bytes` 中的结束字节下标
                var lineEnd = lineStart
                //从 `lineStart` 开始往后找，直到找到文件末尾或换行符
                while (lineEnd < bytes.size && bytes[lineEnd] != '\n'.code.toByte()) {
                    lineEnd++
                }
                //截取出当前行的字节（不包含换行符）
                val lineBytes = bytes.copyOfRange(
                    lineStart,
                    lineEnd
                )
                //按 UTF-8 解码成字符串
                val line = lineBytes.toString(Charsets.UTF_8)
                //如果当前行末尾是换行符（`lineEnd < bytes.size`），
                // 下一行从 `lineEnd + 1` 开始（跳过 `\n`）
                //如果已经到文件末尾（最后一行没有换行符），
                // 下一行起点就是 `bytes.size`
                val nextStart = if (lineEnd < bytes.size) lineEnd + 1 else bytes.size
                if (line.isNotBlank()) {
                    //将字符串解码成日志对象
                    val record = decodeRecord(line)
                    if (record != null) {
                        //写入文件
                        output.write(
                            formatIndexLine(
                                lineStart.toLong(),
                                record
                            ).toByteArray(Charsets.UTF_8)
                        )
                        output.write('\n'.code)
                    }
                }
                //下一行从 `nextStart` 开始
                lineStart = nextStart
            }
        }
    }

    /**
     * 扫描所有日志文件的索引，按条件过滤，返回符合条件的索引条目列表。
     * @param context 上下文
     * @param filter 过滤条件
     * @return 符合条件的索引条目列表
     * */
    private fun loadFilteredIndexEntries(
        context: Context,
        filter: PagedQueryFilter,
    ): List<LogIndexEntry> {
        val entries = mutableListOf<LogIndexEntry>()
        //遍历所有.jsonl 格式的日志文件
        listLogFiles(context).forEach { logFile ->
            ensureIndexForLogFile(logFile)
            //拿到索引文件
            val indexFile = indexFileFor(logFile)
            if (!indexFile.exists()) {
                return@forEach
            }
            //逐行读索引文件
            indexFile.forEachLine { line ->
                //解析索引行
                val entry = parseIndexLine(
                    logFile,
                    line
                ) ?: return@forEachLine
                //根据过滤条件过滤索引条目
                if (matchesIndexFilter(
                        entry,
                        filter
                    )
                ) {
                    entries.add(entry)
                }
            }
        }
        //按时间戳降序排序,最新的日志排最前面
        return entries.sortedByDescending { entry -> entry.timestampEpochMs }
    }

    /**
     * 根据索引条件过滤索引条目
     * @param entry 索引条目
     * @param filter 过滤条件
     * @return 是否匹配
     * */
    private fun matchesIndexFilter(
        entry: LogIndexEntry,
        filter: PagedQueryFilter
    ): Boolean {
        //如果过滤条件指定了日志类型，且当前索引条目的日志类型不匹配，则返回 false
        if (filter.logType != null && entry.logType != filter.logType) {
            return false
        }
        //如果过滤条件指定了页面 ID，且当前索引条目的页面 ID 不匹配，则返回 false
        if (!filter.pageId.isNullOrBlank() && entry.pageId != filter.pageId) {
            return false
        }
        //如果过滤条件指定了时间范围，且当前索引条目的时间不在范围内，则返回 false
        if (filter.sinceEpochMs != null && entry.timestampEpochMs < filter.sinceEpochMs) {
            return false
        }
        //如果过滤条件指定了只看异常日志，且当前非异常日志，则返回false
        if (filter.anomaliesOnly && !entry.anomaly) {
            return false
        }
        return true
    }

    /**
     * 读取日志文件中指定字节偏移量处的日志记录
     * @param logFile 日志文件
     * @param byteOffset 字节偏移量
     * @return 日志记录
     * */
    private fun readRecordAt(
        logFile: File,
        byteOffset: Long
    ): LogRecord? {
        if (!logFile.exists()) {
            return null
        }
        return runCatching {
            FileInputStream(logFile).use { input ->
                input.channel.position(byteOffset)
                input.bufferedReader(Charsets.UTF_8).use { reader ->
                    val line = reader.readLine() ?: return null
                    decodeRecord(line)
                }
            }
        }.getOrNull()
    }

    /**
     * 生成崩溃文件
     * */
    private fun writeLastCrashFile(
        context: Context,
        line: String
    ) {
        val file = File(
            rootDir(context),
            LAST_CRASH_FILE_NAME
        )
        file.parentFile?.mkdirs()
        //覆盖写，不是追加。
        //所以这个文件永远只保留最近一次崩溃的那一行 JSON
        file.writeText(
            line,
            Charsets.UTF_8
        )
    }

    /**
     * 读取崩溃文件
     * */
    private fun readLastCrashFile(): LogRecord? {
        val context = appContextRef.get() ?: return null
        //filesDir/taskflow_observability/last_crash.jsonl
        val file = File(
            rootDir(context),
            LAST_CRASH_FILE_NAME
        )
        if (!file.exists()) {
            return null
        }
        val line = file.useLines(Charsets.UTF_8) { it.firstOrNull() } ?: return null
        return decodeRecord(line)
    }

    /**
     * LogRecord编码成JSON字符串
     * */
    fun encodeRecord(record: LogRecord): String {
        val json = JSONObject()
        //数值型毫秒时间戳，排序 / 过滤
        json.put(
            "timestamp",
            record.timestampEpochMs
        )
        //ISO可读字符串，可读性
        json.put(
            "timestampIso",
            formatIso(record.timestampEpochMs)
        )
        json.put(
            "logType",
            record.logType.wireName
        )
        json.put(
            "logTypeLabel",
            record.logType.displayName
        )
        json.put(
            "pageId",
            record.pageId
        )
        json.put(
            "actionId",
            record.actionId
        )
        json.put(
            "event",
            record.event
        )
        json.put(
            "anomaly",
            record.anomaly
        )
        val paramsJson = JSONObject()
        record.params.forEach { (key, value) ->
            paramsJson.put(
                key,
                value
            )
        }
        json.put(
            "params",
            paramsJson
        )
        if (!record.stackTrace.isNullOrBlank()) {
            json.put(
                "stackTrace",
                record.stackTrace
            )
        }
        if (!record.deviceInfo.isNullOrBlank()) {
            json.put(
                "deviceInfo",
                record.deviceInfo
            )
        }
        return json.toString()
    }


    /**
     * JSON 解码为对象
     * 将字符串解码成日志对象
     * */
    private fun decodeRecord(line: String): LogRecord? {
        if (line.isBlank()) {
            return null
        }
        return runCatching {
            val json = JSONObject(line)
            //json中字符串类型
            val typeWire = json.optString("logType")
            //匹配枚举类型
            val logType =
                LogType.entries.firstOrNull { it.wireName == typeWire } ?: LogType.ANALYTICS
            val paramsJson = json.optJSONObject("params") ?: JSONObject()

            val params = buildMap {
                //params 的 value 全部转成 String
                paramsJson.keys().forEach { key ->
                    put(
                        key,
                        paramsJson.optString(key)
                    )
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
        return SimpleDateFormat(
            TIMESTAMP_PATTERN,
            Locale.US
        ).format(Date(epochMs))
    }
}
