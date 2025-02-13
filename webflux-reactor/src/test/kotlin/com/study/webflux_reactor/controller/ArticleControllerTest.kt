package com.study.webflux_reactor.controller

import com.study.webflux_reactor.repository.ArticleRepository
import com.study.webflux_reactor.service.ReqCreate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.context.event.annotation.AfterTestClass
import org.springframework.test.context.event.annotation.AfterTestExecution
import org.springframework.test.context.transaction.AfterTransaction
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class ArticleControllerTest(
    @Autowired private val context : ApplicationContext,
    @Autowired private val repository: ArticleRepository
){
    // 사용자가 요청하는 것처럼 클라이언트로 확인가능
    val client = WebTestClient.bindToApplicationContext(context).build()

    @AfterEach
    fun clean() {
        // 테스트가 종료가 되면 delete를 해라
        repository.deleteAll()
    }

    @Test
    fun create() {
        val request = ReqCreate("title 1" , "it is r2dbc demo" , 9978)
        // 보유한 uri를 보여줌
        client.post().uri("/article").accept(MediaType.APPLICATION_JSON).bodyValue(request).exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("title").isEqualTo(request.title)
            // 간단한 테스트 같은 경우에는 !! 붙여줌
            .jsonPath("body").isEqualTo(request.body!!)
            .jsonPath("authorId").isEqualTo(request.authorId!!)

    }

    @Test
    fun getAll(){
        // 응답 값이 어떻게 올것인지 체크
        repeat(5) { i ->
            val request = ReqCreate("title $i" , "it is r2dbc demo" , i.toLong())
            client.post().uri("/article").accept(MediaType.APPLICATION_JSON).bodyValue(request).exchange()
        }

        // 위의 방법도 되고 아래방법도 됨
        client.post().uri("/article").accept(MediaType.APPLICATION_JSON).bodyValue(
            ReqCreate("title matched" , "it is r2dbc demo")
        ).exchange()

        val cnt = client.get().uri("/article/all").accept(MediaType.APPLICATION_JSON).exchange()
            .expectStatus().isOk
            .expectBody(List::class.java)
            .returnResult().responseBody?.size ?: 0

        assertTrue(cnt > 0)

        client.get().uri("/article/all?title=matched").accept(MediaType.APPLICATION_JSON).exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
    }

    @Test
    fun get() {
        val request = ReqCreate("title" , "body" , 1234)
        client.post().uri("/article").accept(MediaType.APPLICATION_JSON).bodyValue(request).exchange()

        val clientGet = client.get().uri("/article/2").accept(MediaType.APPLICATION_JSON).exchange()
        println(clientGet)
    }


    // get , update , delete 숙제
    // docker에 레디스 띄우기
    // 레디스는 기본포트가 6379
    // 컴포즈 yml로 해서
    // 레디스를 왜 써야하는지 ,병목현상이 왜 일어나는지 , 동시성
    // 레디스르 왜 써야할까라는 고민
    // 스프링부트 데이터 레디스 jpa로 간단하게 쓸수있음
    // 사용 용도를 모르고 쓰면 오버스펙
    // 부하테스트
    // 부하 , 분산이 카프카가 들어가면 필요없음

}