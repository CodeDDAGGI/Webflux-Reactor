package com.study.webflux_reactor.repository

import com.study.webflux_reactor.model.Article
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux

interface ArticleRepository:R2dbcRepository<Article , Long> {
    // 반환형이 모노아니면 플럭스
    fun findAllByTitleContains(title: String): Flux<Article>

}