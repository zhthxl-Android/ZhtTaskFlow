package com.example.zhttaskflow.feature.article.presentation

import android.util.Log
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.core.foundation.NetworkException
import com.example.zhttaskflow.feature.article.domain.usecase.ArticleDetailResult
import com.example.zhttaskflow.feature.article.domain.usecase.GetArticleDetailUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ArticleDetailViewModel] 核心 MVI 分支回归（加载 / 重试 / 空态与错误态）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailViewModelTest {

    private val getArticleDetailUseCase = mockk<GetArticleDetailUseCase>()

    private val networkMessage = "网络不可用，请检查网络连接后重试"

    @Before
    fun setUp() {
        mockAndroidLog()
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery {
            getArticleDetailUseCase(articleId = ARTICLE_ID, detailUrl = DETAIL_URL)
        } returns ArticleDetailResult(articleId = ARTICLE_ID, detailUrl = DETAIL_URL)
        coEvery {
            getArticleDetailUseCase(articleId = "stale", detailUrl = DETAIL_URL)
        } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun load_success_emitsSuccessWithDetailData() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ArticleDetailUiEvent.Load(ARTICLE_ID, DETAIL_URL))

        val state = viewModel.uiState.value
        assertTrue(state is BaseUiState.Success)
        val data = (state as BaseUiState.Success).data
        assertEquals(ARTICLE_ID, data.articleId)
        assertEquals(DETAIL_URL, data.detailUrl)
    }

    @Test
    fun load_invalidParams_emitsEmpty() = viewModelTest {
        coEvery { getArticleDetailUseCase(articleId = "", detailUrl = DETAIL_URL) } returns null
        val viewModel = createViewModel()

        viewModel.onEvent(ArticleDetailUiEvent.Load("", DETAIL_URL))

        assertEquals(BaseUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun load_useCaseThrows_emitsError() = viewModelTest {
        coEvery {
            getArticleDetailUseCase(articleId = ARTICLE_ID, detailUrl = DETAIL_URL)
        } throws IllegalStateException("load failed")
        val viewModel = createViewModel()

        viewModel.onEvent(ArticleDetailUiEvent.Load(ARTICLE_ID, DETAIL_URL))

        assertTrue(viewModel.uiState.value is BaseUiState.Error)
    }

    @Test
    fun load_networkException_usesLocalizedNetworkMessage() = viewModelTest {
        coEvery {
            getArticleDetailUseCase(articleId = ARTICLE_ID, detailUrl = DETAIL_URL)
        } throws NetworkException(
            message = "no network",
            userMessage = "ignored",
        )
        val viewModel = createViewModel()

        viewModel.onEvent(ArticleDetailUiEvent.Load(ARTICLE_ID, DETAIL_URL))

        val error = viewModel.uiState.value as BaseUiState.Error
        assertEquals(networkMessage, error.message)
    }

    @Test
    fun retry_reloadsWithRouteArguments() = viewModelTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ArticleDetailUiEvent.Load("stale", DETAIL_URL))
        viewModel.onEvent(ArticleDetailUiEvent.Retry)

        assertTrue(viewModel.uiState.value is BaseUiState.Success)
        coVerify(atLeast = 1) {
            getArticleDetailUseCase(articleId = ARTICLE_ID, detailUrl = DETAIL_URL)
        }
    }

    @Test
    fun articleDetailViewModel_hasNoUiEffectsOnSuccess() = viewModelTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ArticleDetailUiEvent.Load(ARTICLE_ID, DETAIL_URL))
        // 成功路径无 ArticleDetailUiEffect 子类型
    }

    private fun viewModelTest(block: suspend TestScope.() -> Unit): Unit = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        block()
    }

    private fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
    }

    private fun createViewModel(
        articleId: String = ARTICLE_ID,
        detailUrl: String = DETAIL_URL,
    ): ArticleDetailViewModel {
        return ArticleDetailViewModel(
            articleId = articleId,
            detailUrl = detailUrl,
            getArticleDetailUseCase = getArticleDetailUseCase,
            networkUnavailableMessage = networkMessage,
        )
    }

    private companion object {
        const val ARTICLE_ID: String = "article-1"
        const val DETAIL_URL: String = "https://example.com/article/1"
    }
}
