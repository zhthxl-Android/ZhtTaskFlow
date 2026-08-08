package com.example.zhttaskflow.base.mvi

/**
 * MVI 一次性副作用根类型（导航、Toast、弹窗等）。
 * 通过 [BaseViewModel] 的 Effect 通道下发，UI 层单次消费。
 */
interface BaseUiEffect
