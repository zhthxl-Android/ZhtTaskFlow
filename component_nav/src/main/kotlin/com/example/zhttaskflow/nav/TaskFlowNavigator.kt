package com.example.zhttaskflow.nav

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * 导航执行器：业务通过封装方法跳转，不直接持有 [NavHostController]。
 */
class TaskFlowNavigator {

    private var navHostController: NavHostController? = null

    /** 由 [TaskFlowNavHost] 绑定控制器 */
    fun bind(controller: NavHostController) {
        navHostController = controller
    }

    /**
     * 跳转到指定路由。
     *
     * @param route 已注册的路由路径（含参数时传入完整 path）
     */
    fun navigate(route: String) {
        navHostController?.navigate(route)
    }

    /**
     * 主界面底部 Tab 切换：等价于路由跳转，并保留各 Tab 返回栈与页面状态。
     *
     * 点击当前 Tab 不重复导航；使用 [launchSingleTop]、[saveState]、[restoreState] 避免重建与滚动丢失。
     */
    fun navigateMainTab(route: String) {
        val controller = navHostController ?: return
        val currentRoute = controller.currentBackStackEntry?.destination?.route
        if (currentRoute == route) {
            return
        }
        val startDestinationId = controller.graph.findStartDestination().id
        controller.navigate(route) {
            popUpTo(startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** 返回上一页 */
    fun navigateUp() {
        navHostController?.popBackStack()
    }
}
