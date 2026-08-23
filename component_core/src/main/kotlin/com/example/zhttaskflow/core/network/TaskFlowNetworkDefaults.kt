package com.example.zhttaskflow.core.network

/**
 * 网络客户端默认参数（core 内部统一维护，禁止业务层散落硬编码）。
 */
internal object TaskFlowNetworkDefaults {
    const val CONNECT_TIMEOUT_SECONDS = 5L
    const val READ_TIMEOUT_SECONDS = 10L
    const val WRITE_TIMEOUT_SECONDS = 5L
}
