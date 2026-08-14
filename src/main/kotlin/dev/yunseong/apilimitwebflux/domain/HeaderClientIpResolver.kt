package dev.yunseong.apilimitwebflux.domain

import org.springframework.web.server.ServerWebExchange

/**
 * Reads the visitor address from a request header — `CF-Connecting-IP` behind
 * Cloudflare, `X-Real-IP` behind a hand-rolled nginx, and so on.
 *
 * **Only safe when the origin refuses traffic that did not come through the
 * proxy.** The proxy overwrites whatever the client sent, so the header is
 * trustworthy on that path and forgeable on every other one; anyone who can reach
 * the origin directly can otherwise pick their own bucket and bypass the limit
 * entirely. That is why no header is trusted unless the application names one.
 *
 * A value that is not an IP literal is ignored in favour of the socket address,
 * which keeps a forged host name out of the storage key.
 */
class HeaderClientIpResolver(
    val headerName: String,
    private val fallback: ClientIpResolver = RemoteAddressClientIpResolver()
) : ClientIpResolver {

    init {
        require(headerName.isNotBlank()) { "headerName must not be blank" }
    }

    companion object {
        private val IPV4 = Regex("""\d{1,3}(\.\d{1,3}){3}""")

        /** Host names can not contain ':', so a match here is always an IPv6 literal. */
        private val IPV6 = Regex("""[0-9A-Fa-f:.]+""")

        /** Internal: the guard is what bounds a forged header, so it is tested directly. */
        internal fun isIpLiteral(value: String?): Boolean {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) {
                return false
            }
            return if (trimmed.contains(':')) IPV6.matches(trimmed) else IPV4.matches(trimmed)
        }
    }

    override fun resolve(exchange: ServerWebExchange): String {
        val forwarded = exchange.request.headers.getFirst(headerName)
        return if (isIpLiteral(forwarded)) forwarded!!.trim() else fallback.resolve(exchange)
    }
}
