package com.example.zhttaskflow.feature.task.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

class FeatureTaskDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskFlowTheme {
            }
        }
    }
}
