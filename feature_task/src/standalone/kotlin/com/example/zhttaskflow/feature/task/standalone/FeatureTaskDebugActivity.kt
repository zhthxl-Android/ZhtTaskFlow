package com.example.zhttaskflow.feature.task.standalone

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.task.data.TaskMockDataSource
import com.example.zhttaskflow.feature.task.data.TaskRepositoryImpl
import com.example.zhttaskflow.feature.task.ui.TaskListScreen
import com.example.zhttaskflow.feature.task.ui.TaskViewModel
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/** Feature 独立调试入口：展示任务列表页。 */
class FeatureTaskDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskFlowTheme {
                val viewModel = remember {
                    TaskViewModel(
                        TaskRepositoryImpl(TaskMockDataSource()),
                    )
                }
                TaskListScreen(
                    viewModel = viewModel,
                    onNavigateToTaskDetail = { taskId ->
                        Toast.makeText(
                            this,
                            "跳转详情: $taskId",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }
        }
    }
}
