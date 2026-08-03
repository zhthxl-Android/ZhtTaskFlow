package com.example.zhttaskflow.feature.article.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

class FeatureArticleDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskFlowTheme {
            }
        }
    }
}
