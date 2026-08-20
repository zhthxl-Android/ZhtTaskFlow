package com.example.zhttaskflow.nav.route

/**
 * 任务 Feature 全局路由常量（单一事实来源）。
 *
 * 业务 feature_task 仅引用本对象，不在模块内重复定义路由字符串。
 */
object TaskFlowTaskNavRoutes {

    const val LIST: String = "feature_task/list"

    const val DETAIL: String = "feature_task/detail/{taskId}"

    const val ARG_TASK_ID: String = "taskId"

    /**
     * 生成详情/编辑页完整导航 path（用于 [com.example.zhttaskflow.nav.TaskFlowNavigator.navigate]）。
     */
    fun detailPath(taskId: String): String = "feature_task/detail/$taskId"
}
