package com.example.zhttaskflow.feature.task.presentation

import android.util.Log
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskAttachment
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import com.example.zhttaskflow.feature.task.domain.TaskRepository
import com.example.zhttaskflow.feature.task.domain.usecase.GetTaskByIdUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.UpdateTaskUseCase
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [TaskDetailViewModel] 核心 MVI 分支回归（加载 / 编辑 / 状态 / 副作用）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    private val taskRepository = mockk<TaskRepository>()
    private val getTaskByIdUseCase = GetTaskByIdUseCase(taskRepository)
    private val updateTaskUseCase = UpdateTaskUseCase(taskRepository)

    @Before
    fun setUp() {
        mockAndroidLog()
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { taskRepository.observeTaskDataChanges() } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun init_blankTaskId_emitsEmptyState() {
        val viewModel = createViewModel(taskId = "  ")
        assertEquals(BaseUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun load_success_emitsSuccessWithTask() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
        val state = viewModel.uiState.value
        assertTrue(state is BaseUiState.Success)
        assertEquals(task, (state as BaseUiState.Success).data.task)
    }

    @Test
    fun load_taskNotFound_emitsEmpty() = viewModelTest {
        coEvery { taskRepository.getTaskById(TASK_ID) } returns null
        val viewModel = createViewModel()
        viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
        assertEquals(BaseUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun load_failure_emitsError() = viewModelTest {
        coEvery { taskRepository.getTaskById(TASK_ID) } throws IllegalStateException("boom")
        val viewModel = createViewModel()
        viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
        assertTrue(viewModel.uiState.value is BaseUiState.Error)
    }

    @Test
    fun load_blankId_emitsEmptyWithoutCallingUseCase() = viewModelTest {
        val viewModel = createViewModel()
        viewModel.onEvent(TaskDetailUiEvent.Load(""))
        assertEquals(BaseUiState.Empty, viewModel.uiState.value)
        coVerify(exactly = 0) { taskRepository.getTaskById(any<String>()) }
    }

    @Test
    fun retry_usesRouteTaskId() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById("other") } returns null
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        viewModel.onEvent(TaskDetailUiEvent.Load("other"))
        viewModel.onEvent(TaskDetailUiEvent.Retry)
        assertTrue(viewModel.uiState.value is BaseUiState.Success)
        coVerify(exactly = 1) { taskRepository.getTaskById(TASK_ID) }
    }

    @Test
    fun startEdit_setsEditingFlag() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
        viewModel.onEvent(TaskDetailUiEvent.StartEdit)
        val data = (viewModel.uiState.value as BaseUiState.Success).data
        assertTrue(data.isEditing)
        assertEquals(task.title, data.draftTitle)
    }

    @Test
    fun cancelEdit_restoresDraftAndSendsEffect() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.StartEdit)
            viewModel.onEvent(TaskDetailUiEvent.DraftTitleChanged("draft"))
            viewModel.onEvent(TaskDetailUiEvent.CancelEdit)
        }
        val data = (viewModel.uiState.value as BaseUiState.Success).data
        assertFalse(data.isEditing)
        assertEquals(task.title, data.draftTitle)
        val snackbar = effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().last()
        assertEquals("已取消编辑", snackbar.message)
        assertEquals("task_detail_edit_cancel", snackbar.actionId)
    }

    @Test
    fun saveEdit_blankTitle_sendsValidationEffect() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.StartEdit)
            viewModel.onEvent(TaskDetailUiEvent.DraftTitleChanged("   "))
            viewModel.onEvent(TaskDetailUiEvent.SaveEdit)
        }
        val snackbar = effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().single()
        assertEquals(SnackbarType.Error, snackbar.type)
        assertEquals("task_detail_save_validation", snackbar.actionId)
        coVerify(exactly = 0) { taskRepository.updateTask(any()) }
    }

    @Test
    fun saveEdit_success_updatesTaskAndSendsSuccessEffect() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        coEvery { taskRepository.updateTask(any()) } just Runs
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.StartEdit)
            viewModel.onEvent(TaskDetailUiEvent.DraftTitleChanged("新标题"))
            viewModel.onEvent(TaskDetailUiEvent.DraftContentChanged("新内容"))
            viewModel.onEvent(TaskDetailUiEvent.SaveEdit)
        }
        val data = (viewModel.uiState.value as BaseUiState.Success).data
        assertFalse(data.isEditing)
        assertEquals("新标题", data.task.title)
        assertEquals("新内容", data.task.content)
        coVerify(exactly = 1) { taskRepository.updateTask(match { it.title == "新标题" && it.content == "新内容" }) }
        val snackbar = effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().last()
        assertEquals(SnackbarType.Success, snackbar.type)
        assertEquals("task_detail_save", snackbar.actionId)
    }

    @Test
    fun saveEdit_failure_sendsErrorEffect() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        coEvery { taskRepository.updateTask(any()) } throws IllegalStateException("save failed")
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.StartEdit)
            viewModel.onEvent(TaskDetailUiEvent.SaveEdit)
        }
        val data = (viewModel.uiState.value as BaseUiState.Success).data
        assertFalse(data.isSubmitting)
        val snackbar = effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().last()
        assertEquals(SnackbarType.Error, snackbar.type)
        assertEquals("task_detail_save_failure", snackbar.actionId)
    }

    @Test
    fun changeStatus_invalidTransition_sendsErrorEffect() = viewModelTest {
        val task = sampleTask(status = TaskStatus.COMPLETED)
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.ChangeStatus(TaskStatus.IN_PROGRESS))
        }
        val snackbar = effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().single()
        assertEquals("task_detail_status_invalid", snackbar.actionId)
        coVerify(exactly = 0) { taskRepository.updateTask(any()) }
    }

    @Test
    fun changeStatus_allowed_updatesAndSendsSuccessEffect() = viewModelTest {
        val task = sampleTask(status = TaskStatus.PENDING)
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        coEvery { taskRepository.updateTask(any()) } just Runs
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.ChangeStatus(TaskStatus.IN_PROGRESS))
        }
        val data = (viewModel.uiState.value as BaseUiState.Success).data
        assertEquals(TaskStatus.IN_PROGRESS, data.task.status)
        assertEquals("task_detail_status_change", effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().last().actionId)
    }

    @Test
    fun attachmentClicked_missing_sendsErrorEffect() = viewModelTest {
        val task = sampleTask()
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.AttachmentClicked("missing"))
        }
        assertEquals("task_detail_attachment_missing", effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().single().actionId)
    }

    @Test
    fun attachmentClicked_found_sendsOpenEffect() = viewModelTest {
        val attachment = TaskAttachment(
            id = "att-1",
            displayName = "spec.pdf",
            sizeBytes = 1024L,
            mimeType = "application/pdf",
        )
        val task = sampleTask(attachments = listOf(attachment))
        coEvery { taskRepository.getTaskById(TASK_ID) } returns task
        val viewModel = createViewModel()
        val effects = mutableListOf<TaskDetailUiEffect>()
        withEffectCollector(viewModel, effects) {
            viewModel.onEvent(TaskDetailUiEvent.Load(TASK_ID))
            viewModel.onEvent(TaskDetailUiEvent.AttachmentClicked("att-1"))
        }
        val snackbar = effects.filterIsInstance<TaskDetailUiEffect.ShowSnackbar>().single()
        assertEquals("task_detail_attachment_open", snackbar.actionId)
        assertTrue(snackbar.message.contains("spec.pdf"))
    }

    private fun viewModelTest(block: suspend TestScope.() -> Unit): Unit = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        block()
    }

    private suspend fun TestScope.withEffectCollector(
        viewModel: TaskDetailViewModel,
        sink: MutableList<TaskDetailUiEffect>,
        block: suspend () -> Unit,
    ) {
        backgroundScope.launch(Dispatchers.Main.immediate) {
            viewModel.uiEffect.collect { sink.add(it) }
        }
        yield()
        block()
        yield()
    }

    private fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
    }

    private fun createViewModel(taskId: String = TASK_ID): TaskDetailViewModel {
        return TaskDetailViewModel(
            taskId = taskId,
            getTaskByIdUseCase = getTaskByIdUseCase,
            updateTaskUseCase = updateTaskUseCase,
        )
    }

    private fun sampleTask(
        id: String = TASK_ID,
        status: TaskStatus = TaskStatus.PENDING,
        attachments: List<TaskAttachment> = emptyList(),
    ): Task {
        return Task(
            id = id,
            title = "示例任务",
            content = "示例内容",
            createdAt = 1_700_000_000_000L,
            status = status,
            attachments = attachments,
        )
    }

    private companion object {
        const val TASK_ID: String = "task-1"
    }
}
