package com.example.zhttaskflow.feature.article.presentation

import android.net.Uri
import android.util.Log
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.article.domain.Article
import com.example.zhttaskflow.feature.article.domain.ArticlePage
import com.example.zhttaskflow.feature.article.domain.ArticlePagingDefaults
import com.example.zhttaskflow.feature.article.domain.usecase.GetArticlePageUseCase
import com.example.zhttaskflow.feature.article.domain.usecase.RefreshArticlePageUseCase
import com.example.zhttaskflow.nav.route.ArticleNavRoutes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ArticleViewModel] 资讯列表核心 MVI 分支：首屏/分页/刷新/副作用。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelTest {

    private val getArticlePageUseCase = mockk<GetArticlePageUseCase>()
    private val refreshArticlePageUseCase = mockk<RefreshArticlePageUseCase>()

    private val pageSize = ArticlePagingDefaults.DEFAULT_PAGE_SIZE

    @Before
    fun setUp() {
        mockAndroidLog()
        mockkStatic(Uri::class)
        every { Uri.encode(any<String>()) } answers { firstArg() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        unmockkStatic(Uri::class)
    }

    @Test
    fun init_loadFirstPageSuccess_emitsSuccess() = viewModelTest {
        val page = samplePage(page = 1, hasMore = true)
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns page
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value as BaseUiState.Success
        assertEquals(1, state.data.articles.size)
        assertTrue(state.data.hasMore)
        assertEquals(1, state.data.currentPage)
    }

    @Test
    fun init_loadFirstPageFailure_emitsError() = viewModelTest {
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } throws IllegalStateException("network down")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is BaseUiState.Error)
    }

    @Test
    fun init_emptyFirstPage_emitsEmpty() = viewModelTest {
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns ArticlePage(emptyList(), page = 1, pageSize = pageSize, hasMore = false)
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(BaseUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun loadMore_success_appendsArticles() = viewModelTest {
        val first = samplePage(page = 1, hasMore = true, idSuffix = "a")
        val second = samplePage(page = 2, hasMore = false, idSuffix = "b")
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns first
        coEvery { getArticlePageUseCase(page = 2, pageSize = pageSize) } returns second
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ArticleUiEvent.LoadMore)
        advanceUntilIdle()
        val state = viewModel.uiState.value as BaseUiState.Success
        assertEquals(2, state.data.articles.size)
        assertFalse(state.data.hasMore)
        assertFalse(state.data.isLoadingMore)
    }

    @Test
    fun loadMore_whenNoMore_doesNotCallUseCaseAgain() = viewModelTest {
        val first = samplePage(page = 1, hasMore = false)
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns first
        coEvery { getArticlePageUseCase(page = 2, pageSize = pageSize) } returns samplePage(page = 2)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ArticleUiEvent.LoadMore)
        advanceUntilIdle()
        val state = viewModel.uiState.value as BaseUiState.Success
        assertEquals(1, state.data.articles.size)
        coVerify(exactly = 0) { getArticlePageUseCase(page = 2, pageSize = pageSize) }
    }

    @Test
    fun loadMore_failure_setsLoadMoreErrorAndSnackbar() = viewModelTest {
        val first = samplePage(page = 1, hasMore = true)
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns first
        coEvery { getArticlePageUseCase(page = 2, pageSize = pageSize) } throws IllegalStateException("more failed")
        val effects = mutableListOf<ArticleUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(ArticleUiEvent.LoadMore)
            advanceUntilIdle()
        }
        val state = viewModel.uiState.value as BaseUiState.Success
        assertTrue(state.data.isLoadMoreError)
        assertEquals(SnackbarType.Error, effects.filterIsInstance<ArticleUiEffect.ShowSnackbar>().last().type)
    }

    @Test
    fun refresh_success_emitsSuccessSnackbar() = viewModelTest {
        val first = samplePage(page = 1, hasMore = false)
        val refreshed = samplePage(page = 1, hasMore = false, idSuffix = "refreshed")
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns first
        coEvery {
            refreshArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns refreshed
        val effects = mutableListOf<ArticleUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(ArticleUiEvent.Refresh)
            advanceUntilIdle()
        }
        val successSnack = effects.filterIsInstance<ArticleUiEffect.ShowSnackbar>()
            .last { it.type == SnackbarType.Success }
        assertEquals("刷新成功", successSnack.message)
    }

    @Test
    fun refresh_afterFirstLoadFailure_recoversToSuccess() = viewModelTest {
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } throws IllegalStateException("first fail")
        coEvery {
            refreshArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns samplePage(page = 1, hasMore = false)
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is BaseUiState.Error)
        viewModel.onEvent(ArticleUiEvent.Refresh)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is BaseUiState.Success)
    }

    @Test
    fun articleClicked_valid_emitsNavigateEffect() = viewModelTest {
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns samplePage(page = 1, hasMore = false)
        val effects = mutableListOf<ArticleUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(ArticleUiEvent.ArticleClicked("id-1", "https://example.com/a"))
            yield()
        }
        val nav = effects.filterIsInstance<ArticleUiEffect.NavigateToDetail>().single()
        assertEquals(
            ArticleNavRoutes.detailPath(articleId = "id-1", detailUrl = "https://example.com/a"),
            nav.url,
        )
    }

    @Test
    fun articleClicked_invalid_emitsErrorSnackbar() = viewModelTest {
        coEvery {
            getArticlePageUseCase(page = ArticlePagingDefaults.FIRST_PAGE, pageSize = pageSize)
        } returns samplePage(page = 1, hasMore = false)
        val effects = mutableListOf<ArticleUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(ArticleUiEvent.ArticleClicked("", "https://example.com/a"))
            yield()
        }
        val snackbar = effects.filterIsInstance<ArticleUiEffect.ShowSnackbar>().single()
        assertEquals("无法打开详情", snackbar.message)
    }

    private fun viewModelTest(block: suspend TestScope.() -> Unit): Unit = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        block()
    }

    private suspend fun TestScope.withEffectCollector(
        viewModel: ArticleViewModel,
        sink: MutableList<ArticleUiEffect>,
        block: suspend () -> Unit,
    ) {
        backgroundScope.launch(Dispatchers.Main.immediate) {
            viewModel.uiEffect.collect { sink.add(it) }
        }
        yield()
        block()
        yield()
    }

    private fun createViewModel(): ArticleViewModel {
        return ArticleViewModel(
            getArticlePageUseCase = getArticlePageUseCase,
            refreshArticlePageUseCase = refreshArticlePageUseCase,
        )
    }

    private fun samplePage(
        page: Int,
        hasMore: Boolean = false,
        idSuffix: String = "1",
    ): ArticlePage {
        val article = Article(
            id = "article-$idSuffix",
            title = "标题$idSuffix",
            summary = "摘要",
            coverUrl = null,
            author = "作者",
            publishedAt = 1L,
            category = "科技",
            detailUrl = "https://example.com/$idSuffix",
        )
        return ArticlePage(
            articles = listOf(article),
            page = page,
            pageSize = pageSize,
            hasMore = hasMore,
        )
    }

    private fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
    }
}
