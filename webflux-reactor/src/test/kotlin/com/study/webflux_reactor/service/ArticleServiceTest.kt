package com.study.webflux_reactor.service

import com.study.webflux_reactor.model.Article
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
    // 서비스는 단위 테스트
    @Autowired
    private val service: ArticleService,
    @Autowired
    private val repository: ArticleRepository
){
    @Test
    fun createAndGet() {
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

//    @Test
//    fun update(){
//        service.create(ReqCreate("타이틀" , body = "바디")).flatMap { create ->
//            service.update(create.id, ReqUpdate("수정된 타이틀" , body = "수정된 바디" ))
//                .flatMap { updated -> service.get(updated.id).doOnNext{ read ->
//                    assertEquals(updated.id , read.id)
//                    assertEquals(updated.title, read.title)
//                    assertEquals(updated.body , read.body)
//                    assertNotNull(updated.updatedAt)
//                }}
//        }.rollback().block()
//    }
//
//    @Test
//    fun delete(){
//        service.create(ReqCreate("타이틀1" , "바디1")).flatMap {
//            deleted -> service.delete(deleted.id)
//        }.rollback().block()
//    }



//     개발자님 답
//    @Test
//    fun update() {
//        val request = ReqUpdate(
//            title = "updated !",
//            body = "update body !"
//        )
//
//        service.create(ReqCreate("title1" , body = "1" , authorId = 1234)).flatMap { new ->
//            service.update(new.id , request).flatMap {
//                service.get(new.id)
//            }.doOnNext{ updated ->
//                assertEquals(request.title, updated.title)
//                assertEquals(request.body, updated.body)
//            }
//        }.rollback().block()
//    }
//    @Test
//    fun delete() {
//        // 해당 레파짓토리에서 카운트를 몇개 가지고있는지 Long타입으로 나옴
//        repository.count().flatMap { prevSize ->
//            service.create(ReqCreate("title1", body = "blabal")).flatMap { new ->
//                service.delete(new.id).flatMap {
//                    // 생성을 하고 삭제를 했으니 다시 0
//                    repository.count().doOnNext {currSize ->
//                        assertEquals(prevSize , currSize)
//                    }
//                }
//            }
//            // void값이여서
//        }.rollback()
//    }

    // 마이그레이션을 해야하는 경우
    @Test
    fun deleteOnRollbackFunctionally() {
        // 사이사이의 테스트가능
        // 추가 검증이 필요할때
        // 함수형 람다식 체이닝
        // flatMap은 연산이 없음
        repository.count().flatMap { prevSize ->
            service.create(ReqCreate("title1" , body = "blabla")).flatMap { created ->
                // 튜플 형태
                Mono.zip(Mono.just(prevSize), Mono.just(created))
            }
        }.flatMap { context ->
            // Mono.zip의 2번째값 Mono.just(created)
            val created = context.t2
            service.delete(created.id).flatMap {
            //Mono.zip의 1번째값 Mono.just(prevSize)
                Mono.zip(Mono.just(context.t1) , Mono.just(created))
            }
        }.flatMap { context ->
            repository.count().flatMap { currSize ->
                Mono.zip(Mono.just(context.t1), Mono.just(context.t2),Mono.just(currSize))
            }
        }.doOnNext{
            val prevSize = it.t1
            val currSize = it.t3
            assertEquals(prevSize , currSize)
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