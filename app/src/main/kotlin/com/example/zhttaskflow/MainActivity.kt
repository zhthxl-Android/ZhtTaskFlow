package com.example.zhttaskflow

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

/** 壳 Activity：挂载任务列表页，手动组装 ViewModel（无 DI）。 */
class MainActivity : ComponentActivity() {
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
