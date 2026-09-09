package com.example.zhttaskflow.navigation

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.zhttaskflow.MainActivity
import com.example.zhttaskflow.androidtest.E2ETestSupport
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOGIN_DIALOG_TITLE
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOGIN_DISMISS
import com.example.zhttaskflow.androidtest.E2ETestSupport.TAB_ARTICLE
import com.example.zhttaskflow.androidtest.E2ETestSupport.TAB_LOG
import com.example.zhttaskflow.androidtest.E2ETestSupport.TAB_TASK
import com.example.zhttaskflow.androidtest.E2ETestSupport.TASK_DETAIL_TITLE
import com.example.zhttaskflow.androidtest.E2ETestSupport.TASK_LIST_TITLE
import com.example.zhttaskflow.androidtest.E2ETestSupport.DEMO_TASK_TITLE
import com.example.zhttaskflow.androidtest.E2ETestSupport.articleDetailDeepLinkTarget
import com.example.zhttaskflow.androidtest.E2ETestSupport.completeRouteGatesIfShown
import com.example.zhttaskflow.androidtest.E2ETestSupport.isTextOnScreen
import com.example.zhttaskflow.androidtest.E2ETestSupport.launchDeepLink
import com.example.zhttaskflow.androidtest.E2ETestSupport.pressSystemBack
import com.example.zhttaskflow.androidtest.E2ETestSupport.selectMainTab
import com.example.zhttaskflow.androidtest.E2ETestSupport.waitForBottomTabs
import com.example.zhttaskflow.androidtest.E2ETestSupport.waitForText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 主壳导航冒烟：默认 Tab、底部切换、二级页返回、深链与登录门禁。
 */
@RunWith(AndroidJUnit4::class)
class NavigationE2ETest {

    @get:Rule(order = 0)
    val grantStorageRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES,
    )

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun launch_defaultTabIsArticleList() {
        composeRule.waitForBottomTabs()
        composeRule.onNodeWithText(TAB_ARTICLE, useUnmergedTree = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mainTabs_switchArticleTaskLog() {
        composeRule.waitForBottomTabs()
        composeRule.selectMainTab(TAB_TASK)
        composeRule.waitForText(TASK_LIST_TITLE)
        composeRule.selectMainTab(TAB_LOG)
        composeRule.waitForText(E2ETestSupport.LOG_VIEWER_TITLE)
        composeRule.selectMainTab(TAB_ARTICLE)
        composeRule.onNodeWithText(TAB_ARTICLE, useUnmergedTree = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun taskDetail_navigateBackAfterRouteGates() {
        composeRule.waitForBottomTabs()
        composeRule.selectMainTab(TAB_TASK)
        composeRule.waitForText(TASK_LIST_TITLE)
        composeRule.waitForText(DEMO_TASK_TITLE)
        composeRule.onNodeWithText(DEMO_TASK_TITLE, useUnmergedTree = true).performClick()
        composeRule.completeRouteGatesIfShown()
        composeRule.waitForText(TASK_DETAIL_TITLE)
        composeRule.pressSystemBack()
        composeRule.waitForText(TASK_LIST_TITLE)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun articleDetail_deepLink_triggersLoginGate() {
        composeRule.waitForBottomTabs()
        val target = articleDetailDeepLinkTarget(
            articleId = "e2e-article",
            detailUrl = "https://wanandroid.com/",
        )
        composeRule.launchDeepLink(target)
        composeRule.waitForText(LOGIN_DIALOG_TITLE)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loginRequiredRoute_cancelLogin_staysOnTaskList() {
        composeRule.waitForBottomTabs()
        composeRule.selectMainTab(TAB_TASK)
        composeRule.waitForText(DEMO_TASK_TITLE)
        composeRule.onNodeWithText(DEMO_TASK_TITLE, useUnmergedTree = true).performClick()
        composeRule.waitUntil(E2ETestSupport.DEFAULT_WAIT_MS) {
            composeRule.isTextOnScreen(LOGIN_DIALOG_TITLE) ||
                composeRule.isTextOnScreen(E2ETestSupport.PERMISSION_DIALOG_TITLE)
        }
        if (composeRule.isTextOnScreen(E2ETestSupport.PERMISSION_DIALOG_TITLE)) {
            composeRule.onNodeWithText(E2ETestSupport.PERMISSION_CONFIRM, useUnmergedTree = true)
                .performClick()
            composeRule.waitForText(LOGIN_DIALOG_TITLE)
        }
        composeRule.onNodeWithText(LOGIN_DISMISS, useUnmergedTree = true).performClick()
        composeRule.waitForText(TASK_LIST_TITLE)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun articleDetail_deepLink_navigateBackAfterRouteGates() {
        composeRule.waitForBottomTabs()
        val target = articleDetailDeepLinkTarget(
            articleId = "e2e-article",
            detailUrl = "https://wanandroid.com/",
        )
        composeRule.launchDeepLink(target)
        composeRule.completeRouteGatesIfShown()
        composeRule.waitForText("返回")
        composeRule.pressSystemBack()
        composeRule.onNodeWithText(TAB_ARTICLE, useUnmergedTree = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deepLink_taskDetail_showsLoginGateThenDetail() {
        composeRule.waitForBottomTabs()
        composeRule.launchDeepLink(
            com.example.zhttaskflow.nav.route.TaskNavRoutes.detailPath("demo-1"),
        )
        composeRule.waitForText(LOGIN_DIALOG_TITLE)
        composeRule.completeRouteGatesIfShown()
        composeRule.waitForText(TASK_DETAIL_TITLE)
    }
}
