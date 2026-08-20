package com.example.zhttaskflow.base.mvi

/**
 * MVI 一次性副作用标记接口（Toast、导航等）；由 [BaseViewModel.uiEffect] Channel 分发，UI 层消费后不重放。
 */
interface BaseUiEffect
