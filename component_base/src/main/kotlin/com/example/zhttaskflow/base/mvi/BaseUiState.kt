package com.example.zhttaskflow.base.mvi

/**
 * MVI 页面状态泛型密封基类：外层承载全页面通用态，内层 [Success.data] 承载业务专属字段。
 *
 * ## 设计思路
 * - **Loading / Empty / Error**：与具体业务无关，可在 [component_base] 与通用 UI（骨架屏、空态、错误页）中复用。
 * - **Success(data: T)**：各 Feature 定义 `data class XxxListData`，通过 `typealias XxxUiState = BaseUiState<XxxListData>` 统一范式。
 *
 * ## 状态流转（典型列表页）
 * 1. 进入页面 → [Loading]
 * 2. 首屏成功且无数据 → [Empty]；有数据 → [Success]
 * 3. 首屏失败 → [Error]
 * 4. 已在 [Success] 时刷新、分页等仅更新 [Success.data]，不降级为 [Loading]（首屏除外）
 *
 * ## 适用场景
 * 单页列表、详情、表单等以「加载结果 + 业务载荷」为主的 Compose 页面；复杂多区域页面可扩展 [T] 为多字段聚合数据类。
 */
sealed interface BaseUiState<out T> {

    /** 页面首次加载中，无业务数据。 */
    data object Loading : BaseUiState<Nothing>

    /** 请求成功但业务数据为空。 */
    data object Empty : BaseUiState<Nothing>

    /**
     * 请求失败或不可恢复错误。
     *
     * @param message 用于错误页展示的用户可读文案
     */
    data class Error(val message: String) : BaseUiState<Nothing>

    /**
     * 加载成功，携带业务数据。
     *
     * @param data 页面专属不可变业务载荷
     */
    data class Success<T>(val data: T) : BaseUiState<T>
}

/** 当前是否为首次加载态。 */
val BaseUiState<*>.isLoading: Boolean
    get() = this is BaseUiState.Loading

/** 当前是否为成功态（含业务数据）。 */
val BaseUiState<*>.isSuccess: Boolean
    get() = this is BaseUiState.Success

/** 当前是否为错误态。 */
val BaseUiState<*>.isError: Boolean
    get() = this is BaseUiState.Error

/** 成功态下的业务数据；非 [BaseUiState.Success] 时返回 null。 */
fun <T> BaseUiState<T>.getDataOrNull(): T? =
    (this as? BaseUiState.Success)?.data
