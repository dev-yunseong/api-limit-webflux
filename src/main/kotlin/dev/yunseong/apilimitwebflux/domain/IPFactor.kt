package dev.yunseong.apilimitwebflux.domain

import org.springframework.web.server.ServerWebExchange

class IPFactor(
    /** Defaults to the socket peer, which is what this factor has always counted against. */
    private val clientIpResolver: ClientIpResolver = RemoteAddressClientIpResolver()
) : Factor<String> {

    override fun getKey(exchange: ServerWebExchange): String {
        return clientIpResolver.resolve(exchange)
    }
}
