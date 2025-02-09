package com.study.webflux_reactor.controller

import com.study.webflux_reactor.model.Article
import com.study.webflux_reactor.service.ArticleService
import com.study.webflux_reactor.service.ReqCreate
import com.study.webflux_reactor.service.ReqUpdate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/article")
class ArticleController(
    private val service:ArticleService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: ReqCreate): Mono<Article> =
        service.create(request)

    @GetMapping("/{id}")
    fun get(@PathVariable id : Long):Mono<Article> =
        service.get(id)

    @GetMapping("/all")
    fun getAll(@RequestParam title: String?):Flux<Article> =
        service.getAll(title)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long , @RequestBody request: ReqUpdate): Mono<Article> {
        logger.debug {">> request : $request"}
        return service.update(id, request)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id:Long): Mono<Void> =
        service.delete(id)

}