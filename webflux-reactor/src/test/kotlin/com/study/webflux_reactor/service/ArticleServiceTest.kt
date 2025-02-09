package com.study.webflux_reactor.service

import com.study.webflux_reactor.repository.ArticleRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Transaction
import org.springframework.boot.system.ApplicationPid
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono

@SpringBootTest
class ArticleServiceTest(
    @Autowired
    private val service: ArticleService,
    @Autowired
    private val repository: ArticleRepository
){
    @Test
    fun createAndGet(){
        service.create(ReqCreate("title1", body = "blabla")).flatMap { created ->
            // doOnNext : 비동기 머하고 나서 머해라 (순서보장용)
            service.get(created.id).doOnNext{ read ->
                // 앞뒤가 같은지
                assertEquals(created.id , read.id)
                assertEquals(created.title , read.title)
                assertEquals(created.body , read.body)
                assertEquals(created.authorId , read.authorId)
                assertNotNull(read.createdAt)
                assertNotNull(read.updatedAt)
            }
            // rollback은 커스텀
            // 써야하는 이유는 created가 제대로 이루어지지 않을때
        }.rollback().block()
    }

    @Test
    fun getAll(){
        // 큐플형식으로 flux를 mono로 처리할떄
        // 비교작업은 flatMap으로 평탄화를 해줌
        // 실제 테스트해서 크리에이트가 진행되서 실제 값이 들어감
        Mono.zip(
            service.create(ReqCreate("title1", body = "blabla")),
            service.create(ReqCreate("title2", body = "blabla")),
            service.create(ReqCreate("title match", body = "blabla"))
        ).flatMap {
            service.getAll().collectList().doOnNext{
                assertEquals(3, it.size)
            }
        }.flatMap {
            service.getAll("match").collectList().doOnNext{
                assertEquals(1 , it.size)
            }
        }.rollback().block()
    }
}

@Configuration
class RxTransactionManager: ApplicationContextAware {
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        rxtx = applicationContext.getBean(TransactionalOperator::class.java)
    }
    companion object{
        lateinit var rxtx: TransactionalOperator
            private set
    }
}

// 비동기작업에서는 직접만들어서 써야댐
fun <T> Mono<T>.rollback():Mono<T>{
    val publisher = this
    return RxTransactionManager.rxtx.execute {
        tx -> tx.setRollbackOnly()
        publisher
    }.next()
}