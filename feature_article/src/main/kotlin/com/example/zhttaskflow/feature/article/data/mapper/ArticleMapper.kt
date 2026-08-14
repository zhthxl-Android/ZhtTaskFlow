package com.example.zhttaskflow.feature.article.data.mapper

import com.example.zhttaskflow.feature.article.data.local.ArticleEntity
import com.example.zhttaskflow.feature.article.data.local.ArticlePageMetaEntity
import com.example.zhttaskflow.feature.article.data.remote.ArticleDto
import com.example.zhttaskflow.feature.article.data.remote.ArticlePageDto
import com.example.zhttaskflow.feature.article.domain.Article
import com.example.zhttaskflow.feature.article.domain.ArticlePage

/**
 * 领域 / 网络 / 本地实体之间的手动映射，隔离各层数据模型。
 */
object ArticleMapper {

    fun dtoToDomain(dto: ArticleDto): Article = Article(
        id = dto.id,
        title = dto.title,
        summary = dto.summary,
        coverUrl = dto.coverUrl,
        author = dto.author,
        publishedAt = dto.publishedAt,
    )

    fun domainToEntity(
        article: Article,
        listPage: Int,
        listPosition: Int,
    ): ArticleEntity = ArticleEntity(
        id = article.id,
        title = article.title,
        summary = article.summary,
        coverUrl = article.coverUrl,
        author = article.author,
        publishedAt = article.publishedAt,
        listPage = listPage,
        listPosition = listPosition,
    )

    fun entityToDomain(entity: ArticleEntity): Article = Article(
        id = entity.id,
        title = entity.title,
        summary = entity.summary,
        coverUrl = entity.coverUrl,
        author = entity.author,
        publishedAt = entity.publishedAt,
    )

    fun pageDtoToDomain(dto: ArticlePageDto): ArticlePage = ArticlePage(
        articles = dto.list.map { dtoToDomain(it) },
        page = dto.page,
        pageSize = dto.pageSize,
        hasMore = dto.hasMore,
    )

    fun pageDomainToEntities(page: ArticlePage): Pair<List<ArticleEntity>, ArticlePageMetaEntity> {
        val entities = page.articles.mapIndexed { index, article ->
            domainToEntity(article, page.page, index)
        }
        val meta = ArticlePageMetaEntity(
            page = page.page,
            pageSize = page.pageSize,
            hasMore = page.hasMore,
        )
        return entities to meta
    }

    fun entitiesToDomainPage(
        entities: List<ArticleEntity>,
        meta: ArticlePageMetaEntity?,
        page: Int,
        pageSize: Int,
    ): ArticlePage? {
        if (entities.isEmpty() && meta == null) {
            return null
        }
        return ArticlePage(
            articles = entities.map { entityToDomain(it) },
            page = meta?.page ?: page,
            pageSize = meta?.pageSize ?: pageSize,
            hasMore = meta?.hasMore ?: false,
        )
    }
}
