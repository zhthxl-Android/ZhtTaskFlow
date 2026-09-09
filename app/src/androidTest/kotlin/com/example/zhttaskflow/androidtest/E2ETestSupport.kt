package com.example.zhttaskflow.androidtest

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.example.zhttaskflow.MainActivity
import com.example.zhttaskflow.nav.route.ArticleNavRoutes

/** 仪表化 E2E 通用等待与路由门禁操作。 */
object E2ETestSupport {

    const val TAB_ARTICLE: String = "资讯"
    const val TAB_TASK: String = "任务"
    const val TAB_LOG: String = "日志"

    const val LOGIN_DIALOG_TITLE: String = "登录提示"
    const val LOGIN_CONFIRM: String = "去登录"
    const val LOGIN_DISMISS: String = "暂不登录"

    const val PERMISSION_DIALOG_TITLE: String = "权限申请"
    const val PERMISSION_CONFIRM: String = "去授权"

    const val TASK_LIST_TITLE: String = "任务列表"
    const val TASK_DETAIL_TITLE: String = "任务详情"
    const val DEMO_TASK_TITLE: String = "示例任务"

    const val LOG_VIEWER_TITLE: String = "日志"
    const val LOG_EMPTY: String = "暂无日志"
    const val LOG_CLEAR: String = "清空日志"
    const val LOG_CLEAR_CONFIRM_TITLE: String = "清空本地日志？"
    const val LOG_CONFIRM: String = "确定"
    const val LOG_CANCEL: String = "取消"
    const val LOG_FILTER_CRASH: String = "崩溃"
    const val LOG_FILTER_ANALYTICS: String = "埋点"
    const val LOG_ENTRY_DETAIL: String = "详情"

    const val DEFAULT_WAIT_MS: Long = 20_000L

    @OptIn(ExperimentalTestApi::class)
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForBottomTabs(
        timeoutMillis: Long = DEFAULT_WAIT_MS,
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithText(TAB_ARTICLE, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.selectMainTab(
        label: String,
    ) {
        onNodeWithText(label, useUnmergedTree = true).performClick()
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.isTextOnScreen(
        text: String,
    ): Boolean {
        return onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
    }

    @OptIn(ExperimentalTestApi::class)
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.waitForText(
        text: String,
        timeoutMillis: Long = DEFAULT_WAIT_MS,
        substring: Boolean = false,
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        onNodeWithText(text, substring = substring, useUnmergedTree = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.completeRouteGatesIfShown(
        timeoutMillis: Long = DEFAULT_WAIT_MS,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            when {
                isTextOnScreen(PERMISSION_DIALOG_TITLE) -> {
                    onNodeWithText(PERMISSION_CONFIRM, useUnmergedTree = true).performClick()
                    waitForIdle()
                }
                isTextOnScreen(LOGIN_DIALOG_TITLE) -> {
                    onNodeWithText(LOGIN_CONFIRM, useUnmergedTree = true).performClick()
                    waitForIdle()
                }
                else -> return
            }
        }
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.dismissLoginGateIfShown() {
        if (isTextOnScreen(LOGIN_DIALOG_TITLE)) {
            onNodeWithText(LOGIN_DISMISS, useUnmergedTree = true).performClick()
            waitForIdle()
        }
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.launchDeepLink(
        internalTargetRoute: String,
    ) {
        val encodedTarget = Uri.encode(internalTargetRoute)
        val deepLinkUri = "taskflow://nav/route?target=$encodedTarget"
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
        waitForIdle()
    }

    fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.pressSystemBack() {
        activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        waitForIdle()
    }

    fun articleDetailDeepLinkTarget(articleId: String, detailUrl: String): String {
        return ArticleNavRoutes.detailPath(articleId, detailUrl)
    }
}
