package com.example.zhttaskflow.feature.article.data

import android.content.Context
import com.example.zhttaskflow.core.network.RetrofitServiceFactory
import com.example.zhttaskflow.core.network.TaskFlowNetworkConfig
import com.example.zhttaskflow.feature.article.data.local.ArticleLocalDataSource
import com.example.zhttaskflow.feature.article.data.remote.ArticleApi
import com.example.zhttaskflow.feature.article.data.remote.ArticleRemoteDataSource
import com.example.zhttaskflow.feature.article.domain.ArticleRepository

/**
 * [ArticleRepository] 手动装配入口（无 DI 框架）。
 *
 * 使用 core 层 [RetrofitServiceFactory] 与 [ArticleLocalDataSource]（内部经 [com.example.zhttaskflow.core.persistence.room.TaskFlowRoomTemplate] 访问 Room）。
 */
object ArticleRepositoryFactory {

    /**
     * 创建可注入 presentation 层的 [ArticleRepository] 实例。
     *
     * @param context 建议使用 [Context.getApplicationContext]
     * @param config 网络与日志等行为配置
     */
    fun create(context: Context, config: ArticleDataConfig): ArticleRepository {
        val localDataSource = ArticleLocalDataSource.create(context)

        val networkConfig = TaskFlowNetworkConfig(
            baseUrl = config.baseUrl,
            enableLogging = config.enableNetworkLogging,
        )
        val retrofitFactory = RetrofitServiceFactory(networkConfig)
        val okHttpClient = retrofitFactory.createOkHttpClient()
        val articleApi = retrofitFactory.createService(ArticleApi::class.java, okHttpClient)
        val remoteDataSource = ArticleRemoteDataSource(articleApi)

        return ArticleRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
        )
    }
}
