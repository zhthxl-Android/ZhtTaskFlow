package com.example.zhttaskflow.nav

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
}
