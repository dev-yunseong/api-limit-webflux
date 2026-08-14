package dev.yunseong.apilimitwebflux.domain

import org.springframework.web.server.ServerWebExchange

/**
 * Derives the address [IPFactor] counts requests against.
 *
 * Exists because the socket peer is not always the visitor. Behind a reverse
 * proxy or a CDN the peer is the proxy, and every visitor then collapses into a
 * single bucket, turning a per-visitor limit into a site-wide one. Applications
 * that know how their proxy forwards the original address can supply their own
 * implementation as a bean; the auto-configuration backs off when one is present.
 */
fun interface ClientIpResolver {

    /** @return the address to rate limit against, never null. */
    fun resolve(exchange: ServerWebExchange): String
}
