package com.study.webflux_reactor.service

import com.study.webflux_reactor.exception.NoArticleException
import com.study.webflux_reactor.model.Article
import com.study.webflux_reactor.repository.ArticleRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

private val logger = KotlinLogging.logger {}

@Service
class ArticleService(
    private val repository: ArticleRepository
) {
    @Transactional
    fun create(request: ReqCreate): Mono<Article> {
        logger.debug { "Creating article with request: $request" }
        return repository.save(request.toArticle())
            .doOnError { e -> logger.error(e) { "Error creating article" } }
    }
    fun get(id: Long): Mono<Article> =
        repository.findById(id)
            //null이면 Empty값을 정해줘야댐
            .switchIfEmpty(Mono.error(NoArticleException("id: $id")))

    fun getAll(title: String? = null): Flux<Article> =
        if (title.isNullOrEmpty()) repository.findAll()
        else repository.findAllByTitleContains(title)

    @Transactional
    fun update(id: Long, request: ReqUpdate): Mono<Article> =
        repository.findById(id)
            .switchIfEmpty(Mono.error(NoArticleException("id: $id")))
            .flatMap { article ->
                request.title?.let { article.title = it}
                request.body?.let { article.body = it}
                request.authorId?.let { article.authorId = it}
                repository.save(article) // 더티체킹개념이없어서 다시만들어서 저장을 해줘야댐
            }

    // 타입추론도 가능하지만 넣어주는게 좋음 jpa는 unit인데 R2dbc는 넣어주는게 좋음
    @Transactional
    fun delete(id:Long):Mono<Void> =
        repository.deleteById(id)
}
data class ReqUpdate(
    val title : String? = null,
    val body : String? = null,
    val authorId: Long? = null,
)

data class ReqCreate(
    val title : String,
    val body : String? = null,
    val authorId: Long? = null,
){
    fun toArticle():Article =
        Article(
            title = title,
            body = body,
            authorId = authorId
        )
}