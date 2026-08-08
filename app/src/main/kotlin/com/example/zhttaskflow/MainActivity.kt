package com.example.zhttaskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/** 壳 Activity：空 Compose 根，无 NavHost / 业务 UI */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //task开发分支 创建
        setContent {
            TaskFlowTheme {
            }
        }
    }
}
