package com.example.zhttaskflow.log

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zhttaskflow.MainActivity
import com.example.zhttaskflow.androidtest.E2ETestSupport
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_CANCEL
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_CLEAR
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_CLEAR_CONFIRM_TITLE
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_CONFIRM
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_EMPTY
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_ENTRY_DETAIL
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_FILTER_ANALYTICS
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_FILTER_CRASH
import com.example.zhttaskflow.androidtest.E2ETestSupport.LOG_VIEWER_TITLE
import com.example.zhttaskflow.androidtest.E2ETestSupport.TAB_LOG
import com.example.zhttaskflow.androidtest.E2ETestSupport.selectMainTab
import com.example.zhttaskflow.androidtest.E2ETestSupport.waitForBottomTabs
import com.example.zhttaskflow.androidtest.E2ETestSupport.waitForText
import com.example.zhttaskflow.core.observability.LocalLogStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 日志 Tab 冒烟：列表加载、类型筛选、展开详情、清空二次确认。
 */
@RunWith(AndroidJUnit4::class)
class LogScreenE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedLocalLogs() {
        composeRule.activityRule.scenario.onActivity {
            appendLog(
                logType = LocalLogStore.LogType.ANALYTICS,
                event = "e2e_analytics_event",
                actionId = "e2e_analytics",
            )
            appendLog(
                logType = LocalLogStore.LogType.CRASH,
                event = "e2e_crash_event",
                actionId = "e2e_crash",
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun logList_loadsSeededEntries() {
        openLogTab()
        composeRule.waitForText("e2e_analytics_event", substring = true)
        composeRule.waitForText("e2e_crash_event", substring = true)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun logFilter_crash_showsOnlyCrashEntry() {
        openLogTab()
        composeRule.waitForText("e2e_crash_event", substring = true)
        composeRule.onNodeWithText(LOG_FILTER_CRASH, useUnmergedTree = true).performClick()
        composeRule.waitForText("e2e_crash_event", substring = true)
        composeRule.onAllNodesWithText("e2e_analytics_event", substring = true, useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithText(LOG_FILTER_ANALYTICS, useUnmergedTree = true).performClick()
        composeRule.waitForText("e2e_analytics_event", substring = true)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun logEntry_expandShowsDetailSection() {
        openLogTab()
        composeRule.waitForText("e2e_crash_event", substring = true)
        composeRule.onNodeWithText("e2e_crash_event", substring = true, useUnmergedTree = true)
            .performClick()
        composeRule.waitForText(LOG_ENTRY_DETAIL)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clearLogs_confirmDialog_cancelThenConfirm() {
        openLogTab()
        composeRule.waitForText("e2e_crash_event", substring = true)
        composeRule.onNodeWithText(LOG_CLEAR, useUnmergedTree = true).performClick()
        composeRule.waitForText(LOG_CLEAR_CONFIRM_TITLE)
        composeRule.onNodeWithText(LOG_CANCEL, useUnmergedTree = true).performClick()
        composeRule.onAllNodesWithText(LOG_CLEAR_CONFIRM_TITLE, useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithText(LOG_CLEAR, useUnmergedTree = true).performClick()
        composeRule.waitForText(LOG_CLEAR_CONFIRM_TITLE)
        composeRule.onNodeWithText(LOG_CONFIRM, useUnmergedTree = true).performClick()
        composeRule.waitForText(LOG_EMPTY)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun openLogTab() {
        composeRule.waitForBottomTabs()
        composeRule.selectMainTab(TAB_LOG)
        composeRule.waitForText(LOG_VIEWER_TITLE)
    }

    private fun appendLog(
        logType: LocalLogStore.LogType,
        event: String,
        actionId: String,
    ) {
        val record = LocalLogStore.LogRecord(
            timestampEpochMs = System.currentTimeMillis(),
            logType = logType,
            pageId = "E2E",
            actionId = actionId,
            event = event,
            params = emptyMap(),
        )
        LocalLogStore.append(record)
        Thread.sleep(800)
    }
}
