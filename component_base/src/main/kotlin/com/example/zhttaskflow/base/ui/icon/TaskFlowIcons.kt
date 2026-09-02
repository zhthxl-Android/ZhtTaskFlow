package com.example.zhttaskflow.base.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Description
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 全项目唯一 Material 图标入口：业务层禁止直接引用 [Icons] 及 `androidx.compose.material.icons.*`。
 *
 * ## 使用约定
 * - 着色由调用方 [androidx.compose.material3.Icon] 配合 [androidx.compose.material3.MaterialTheme] 完成，浅色/深色自动适配。
 * - 新增图标仅在本对象扩展；后续本地矢量或自定义图标亦由此透出 [ImageVector]。
 *
 * ## 分类
 * - [Tab]：主界面底部导航选中/未选中态
 * - [Nav]：通用导航与操作
 */
object TaskFlowIcons {

    /**
     * 底部 Tab 图标：选中 Filled、未选中 Outlined。
     */
    object Tab {
        val LogSelected: ImageVector = Icons.Filled.Description
        val LogUnselected: ImageVector = Icons.Outlined.Description
        val ArticleSelected: ImageVector = Icons.AutoMirrored.Filled.Article
        val ArticleUnselected: ImageVector = Icons.AutoMirrored.Outlined.Article
        val TaskSelected: ImageVector = Icons.AutoMirrored.Filled.List
        val TaskUnselected: ImageVector = Icons.AutoMirrored.Outlined.List
    }

    /**
     * 通用导航与顶栏操作图标。
     */
    object Nav {
        val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    }

    /** Snackbar 预设类型图标。 */
    object Snackbar {
        val Success: ImageVector = Icons.Filled.CheckCircle
        val Error: ImageVector = Icons.Filled.Error
        val Info: ImageVector = Icons.Filled.Info
        val Dismiss: ImageVector = Icons.Filled.Close
    }
}
