package dev.yunseong.apilimitwebflux.domain

import org.springframework.web.server.ServerWebExchange

class IPFactor : Factor<String> {

    override fun getKey(exchange: ServerWebExchange): String {
        return exchange.request.remoteAddress?.address?.hostAddress ?: ""
    }
}