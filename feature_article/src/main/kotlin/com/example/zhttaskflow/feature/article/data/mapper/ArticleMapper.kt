package com.example.zhttaskflow.feature.article.data.mapper

import com.example.zhttaskflow.feature.article.data.ArticleDataConstants
import com.example.zhttaskflow.feature.article.data.local.ArticleEntity
import com.example.zhttaskflow.feature.article.data.local.ArticlePageMetaEntity
import com.example.zhttaskflow.feature.article.data.remote.ArticleItemDto
import com.example.zhttaskflow.feature.article.data.remote.ArticlePageDto
import com.example.zhttaskflow.feature.article.domain.Article
import com.example.zhttaskflow.feature.article.domain.ArticlePage

/**
 * 领域 / 网络 / 本地实体之间的手动映射，隔离各层数据模型。
 */
object ArticleMapper {

    fun itemDtoToDomain(dto: ArticleItemDto): Article {
        val articleId = dto.id?.toString() ?: ""
        val detailUrl = dto.link?.takeIf { it.isNotBlank() }
            ?: "${ArticleDataConstants.DEFAULT_DETAIL_URL_PREFIX}$articleId"
        val summary = dto.desc?.takeIf { it.isNotBlank() }
            ?: dto.niceShareDate?.takeIf { it.isNotBlank() }
            ?: dto.niceDate.orEmpty()
        val publishedAt = dto.shareDate ?: dto.publishTime ?: 0L
        return Article(
            id = articleId,
            title = dto.title.orEmpty(),
            summary = summary,
            coverUrl = dto.envelopePic?.takeIf { it.isNotBlank() },
            author = dto.author.orEmpty(),
            publishedAt = publishedAt,
            category = dto.superChapterName?.takeIf { it.isNotBlank() }
                ?: dto.chapterName?.takeIf { it.isNotBlank() }
                ?: ArticleDataConstants.DEFAULT_CATEGORY,
            detailUrl = detailUrl,
        )
    }

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
        category = ArticleDataConstants.DEFAULT_CATEGORY,
        detailUrl = "${ArticleDataConstants.DEFAULT_DETAIL_URL_PREFIX}${entity.id}",
    )

    fun pageDtoToDomain(dto: ArticlePageDto, requestedPageSize: Int): ArticlePage {
        val page = dto.curPage ?: 1
        val pageSize = dto.size ?: requestedPageSize
        val hasMore = dto.over?.let { !it } ?: (dto.articleList.size >= pageSize)
        return ArticlePage(
            articles = dto.articleList.map { itemDtoToDomain(it) },
            page = page,
            pageSize = pageSize,
            hasMore = hasMore,
        )
    }

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
