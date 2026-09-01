package com.example.zhttaskflow.base.mvi

import com.example.zhttaskflow.base.ext.TaskFlowUiEffectConsumption

/**
 * MVI 一次性副作用根类型（Snackbar、导航等）；由 [BaseViewModel.uiEffect] SharedFlow 分发。
 *
 * 具体 Effect 应额外实现 [TaskFlowNavigationUiEffect] 或 [TaskFlowPresentationUiEffect]，
 * 并分别在 RouteHost / Screen 层订阅消费，详见 [TaskFlowUiEffectConsumption]。
 */
interface BaseUiEffect
