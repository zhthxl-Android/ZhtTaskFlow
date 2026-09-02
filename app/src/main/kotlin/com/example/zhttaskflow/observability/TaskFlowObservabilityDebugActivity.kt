package com.example.zhttaskflow.observability

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.zhttaskflow.R
import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.nav.theme.TaskFlowTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 自研可观测本地日志调试页：查询最近条目、导出分享、查看上次崩溃摘要。
 *
 * 启动：`adb shell am start -n com.example.zhttaskflow/.observability.TaskFlowObservabilityDebugActivity`
 */
class TaskFlowObservabilityDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskFlowTheme {
                TaskFlowObservabilityDebugScreen(
                    onExport = { exportLogs() },
                    onClose = { finish() },
                )
            }
        }
    }

    private fun exportLogs() {
        val exportFile = TaskFlowLocalLogStore.exportRecentLogs(this)
        val authority = "${packageName}.observability.fileprovider"
        val uri = FileProvider.getUriForFile(this, authority, exportFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_str_observability_share_title)))
    }
}

@Composable
private fun TaskFlowObservabilityDebugScreen(
    onExport: () -> Unit,
    onClose: () -> Unit,
) {
    var records by remember { mutableStateOf<List<TaskFlowLocalLogStore.LogRecord>>(emptyList()) }
    val lastCrash = remember { TaskFlowLocalLogStore.peekLastCrash() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        records = withContext(Dispatchers.IO) {
            TaskFlowLocalLogStore.query(
                TaskFlowLocalLogStore.QueryFilter(maxEntries = 200),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.app_str_observability_debug_title),
            style = MaterialTheme.typography.titleLarge,
        )
        lastCrash?.let { crash ->
            Text(
                text = stringResource(
                    id = R.string.app_str_observability_last_crash_format,
                    crash.event,
                    crash.pageId,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.app_str_observability_export))
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.app_str_observability_close))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(records, key = { record ->
                "${record.timestampEpochMs}_${record.event}_${record.actionId}"
            }) { record ->
                Text(
                    text = buildString {
                        append(record.logType.displayName)
                        append(" · ")
                        append(record.event)
                        if (record.anomaly) {
                            append(" [异常]")
                        }
                        append("\n")
                        append("pageId=")
                        append(record.pageId)
                        append(" actionId=")
                        append(record.actionId)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
