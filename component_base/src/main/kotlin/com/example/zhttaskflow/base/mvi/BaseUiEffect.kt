package com.example.zhttaskflow.base.mvi

import com.example.zhttaskflow.base.ext.UiEffectConsumption

/**
 * MVI 一次性副作用根类型（Snackbar、导航等）；由 [BaseViewModel.uiEffect] SharedFlow 分发。
 *
 * 具体 Effect 应额外实现 [NavigationUiEffect] 或 [PresentationUiEffect]，
 * 并分别在 RouteHost / Screen 层订阅消费，详见 [UiEffectConsumption]。
 *
 * @see com.example.zhttaskflow.base.doc.BaseArchitecture
 */
interface BaseUiEffect
