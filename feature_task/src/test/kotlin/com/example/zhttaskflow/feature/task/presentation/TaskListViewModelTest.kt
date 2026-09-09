package com.example.zhttaskflow.feature.task.presentation

import android.util.Log
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskDataChanged
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import com.example.zhttaskflow.feature.task.domain.usecase.AddTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.DeleteTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.GetTaskListUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.ObserveTaskDataChangesUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.UpdateTaskUseCase
import com.example.zhttaskflow.nav.route.TaskNavRoutes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [TaskViewModel] 任务列表核心 MVI 分支：刷新、增删改同步、导航与 Snackbar。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val getTaskListUseCase = mockk<GetTaskListUseCase>()
    private val observeTaskDataChangesUseCase = mockk<ObserveTaskDataChangesUseCase>()
    private val addTaskUseCase = mockk<AddTaskUseCase>()
    private val updateTaskUseCase = mockk<UpdateTaskUseCase>(relaxed = true)
    private val deleteTaskUseCase = mockk<DeleteTaskUseCase>(relaxed = true)

    private val dataChanges = MutableSharedFlow<TaskDataChanged>(extraBufferCapacity = 1)

    @Before
    fun setUp() {
        mockAndroidLog()
        coEvery { observeTaskDataChangesUseCase() } returns dataChanges
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun init_loadSuccess_emitsSuccess() = viewModelTest {
        val tasks = listOf(sampleTask("t1"))
        coEvery { getTaskListUseCase() } returns tasks
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value as BaseUiState.Success
        assertEquals(1, state.data.tasks.size)
    }

    @Test
    fun init_loadFailure_emitsError() = viewModelTest {
        coEvery { getTaskListUseCase() } throws IllegalStateException("load failed")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is BaseUiState.Error)
    }

    @Test
    fun refresh_success_emitsRefreshSuccessSnackbar() = viewModelTest {
        val tasks = listOf(sampleTask("t1"))
        coEvery { getTaskListUseCase() } returns tasks
        val effects = mutableListOf<TaskUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskUiEvent.Refresh)
            advanceUntilIdle()
        }
        val success = effects.filterIsInstance<TaskUiEffect.ShowSnackbar>()
            .last { it.type == SnackbarType.Success }
        assertEquals("刷新成功", success.message)
    }

    @Test
    fun refresh_failure_emitsErrorSnackbar() = viewModelTest {
        coEvery { getTaskListUseCase() } returns listOf(sampleTask("t1")) andThenThrows
            IllegalStateException("refresh failed")
        val effects = mutableListOf<TaskUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskUiEvent.Refresh)
            advanceUntilIdle()
        }
        assertTrue(viewModel.uiState.value is BaseUiState.Success)
        assertEquals(SnackbarType.Error, effects.filterIsInstance<TaskUiEffect.ShowSnackbar>().last().type)
    }

    @Test
    fun addTask_success_emitsSuccessSnackbar() = viewModelTest {
        coEvery { getTaskListUseCase() } returns emptyList()
        coEvery { addTaskUseCase(any()) } just Runs
        val effects = mutableListOf<TaskUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskUiEvent.AddTask(title = "新任务", content = "内容"))
            advanceUntilIdle()
        }
        coVerify { addTaskUseCase(match { it.title == "新任务" }) }
        val snackbar = effects.filterIsInstance<TaskUiEffect.ShowSnackbar>()
            .last { it.type == SnackbarType.Success }
        assertEquals("任务已添加", snackbar.message)
    }

    @Test
    fun addTask_blankTitle_emitsValidationSnackbar() = viewModelTest {
        coEvery { getTaskListUseCase() } returns emptyList()
        val effects = mutableListOf<TaskUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskUiEvent.AddTask(title = "  ", content = ""))
            yield()
        }
        assertEquals("请输入任务标题", effects.filterIsInstance<TaskUiEffect.ShowSnackbar>().single().message)
        coVerify(exactly = 0) { addTaskUseCase(any()) }
    }

    @Test
    fun repositoryDataChanged_syncsListAfterEditOrDelete() = viewModelTest {
        val initial = listOf(sampleTask("t1"))
        val afterSync = listOf(sampleTask("t1", title = "已编辑"), sampleTask("t2"))
        coEvery { getTaskListUseCase() } returnsMany listOf(initial, afterSync)
        val viewModel = createViewModel()
        advanceUntilIdle()
        dataChanges.emit(TaskDataChanged)
        advanceUntilIdle()
        val state = viewModel.uiState.value as BaseUiState.Success
        assertEquals(2, state.data.tasks.size)
        assertEquals("已编辑", state.data.tasks.first().title)
        coVerify(atLeast = 2) { getTaskListUseCase() }
    }

    @Test
    fun taskItemClicked_valid_emitsNavigateEffect() = viewModelTest {
        coEvery { getTaskListUseCase() } returns listOf(sampleTask("task-42"))
        val effects = mutableListOf<TaskUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskUiEvent.TaskItemClicked("task-42"))
            yield()
        }
        val nav = effects.filterIsInstance<TaskUiEffect.NavigateToEdit>().single()
        assertEquals(TaskNavRoutes.detailPath("task-42"), nav.url)
    }

    @Test
    fun taskItemClicked_blankId_emitsErrorSnackbar() = viewModelTest {
        coEvery { getTaskListUseCase() } returns listOf(sampleTask("t1"))
        val effects = mutableListOf<TaskUiEffect>()
        val viewModel = createViewModel()
        advanceUntilIdle()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskUiEvent.TaskItemClicked("  "))
            yield()
        }
        assertEquals("任务标识无效", effects.filterIsInstance<TaskUiEffect.ShowSnackbar>().single().message)
    }

    private fun viewModelTest(block: suspend TestScope.() -> Unit): Unit = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        block()
    }

    private suspend fun TestScope.withEffectCollector(
        viewModel: TaskViewModel,
        sink: MutableList<TaskUiEffect>,
        block: suspend () -> Unit,
    ) {
        backgroundScope.launch(Dispatchers.Main.immediate) {
            viewModel.uiEffect.collect { sink.add(it) }
        }
        yield()
        block()
        yield()
    }

    private fun createViewModel(): TaskViewModel {
        return TaskViewModel(
            getTaskListUseCase = getTaskListUseCase,
            observeTaskDataChangesUseCase = observeTaskDataChangesUseCase,
            addTaskUseCase = addTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
        )
    }

    private fun sampleTask(id: String, title: String = "示例任务"): Task {
        return Task(
            id = id,
            title = title,
            content = "内容",
            createdAt = 1L,
            status = TaskStatus.PENDING,
        )
    }

    private fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
    }
}
