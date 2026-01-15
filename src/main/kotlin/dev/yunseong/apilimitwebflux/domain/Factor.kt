package dev.yunseong.apilimitwebflux.domain

import org.springframework.web.server.ServerWebExchange

interface Factor<T> {

    fun getKey(exchange: ServerWebExchange): T
}