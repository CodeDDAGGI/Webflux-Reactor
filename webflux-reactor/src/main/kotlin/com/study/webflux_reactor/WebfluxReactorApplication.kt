package com.study.webflux_reactor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing

@SpringBootApplication
@EnableR2dbcAuditing // 시간 추가
class WebfluxReactorApplication

fun main(args: Array<String>) {
	runApplication<WebfluxReactorApplication>(*args)
}
