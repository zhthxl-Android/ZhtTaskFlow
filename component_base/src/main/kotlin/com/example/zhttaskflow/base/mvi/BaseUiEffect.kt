package com.example.zhttaskflow.base.mvi

/**
 * MVI 一次性副作用标记接口（Toast、导航等）；由 [BaseViewModel.uiEffect] SharedFlow 分发，多订阅方可分别消费。
 */
interface BaseUiEffect
