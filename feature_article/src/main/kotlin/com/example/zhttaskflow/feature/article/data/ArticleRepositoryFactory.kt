package com.example.zhttaskflow.feature.article.data

import android.content.Context
import com.example.zhttaskflow.core.network.RetrofitServiceFactory
import com.example.zhttaskflow.feature.article.data.local.ArticleLocalDataSource
import com.example.zhttaskflow.feature.article.data.remote.ArticleApi
import com.example.zhttaskflow.feature.article.data.remote.ArticleRemoteDataSource
import com.example.zhttaskflow.feature.article.domain.ArticleRepository

/**
 * [ArticleRepository] 手动装配入口（无 DI 框架）。
 *
 * - Room：[ArticleLocalDataSource] → [com.example.zhttaskflow.core.persistence.room.TaskFlowRoomTemplate]
 * - 网络：[RetrofitServiceFactory.createApi] 获取 [ArticleApi]，业务层不构建 OkHttp/Retrofit
 */
object ArticleRepositoryFactory {

    /**
     * 创建可注入 presentation 层的 [ArticleRepository] 实例。
     *
     * @param context 建议使用 [Context.getApplicationContext]
     * @param config 业务专属网络根地址等配置
     */
    fun create(context: Context, config: ArticleDataConfig): ArticleRepository {
        val localDataSource = ArticleLocalDataSource.create(context)

        val articleApi = RetrofitServiceFactory.createApi(
            context = context,
            baseUrl = config.baseUrl,
            serviceClass = ArticleApi::class.java,
        )
        val remoteDataSource = ArticleRemoteDataSource(articleApi)

        return ArticleRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
        )
    }
}
