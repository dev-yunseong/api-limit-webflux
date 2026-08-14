package dev.yunseong.apilimitwebflux.domain

import org.slf4j.LoggerFactory
import org.springframework.web.server.ServerWebExchange

/**
 * Reads the socket peer, which is the visitor only when nothing sits between the
 * client and this application — or when a proxy in front of it is recognised as
 * trusted by `server.forward-headers-strategy`, so the platform has already
 * rewritten the address.
 *
 * The default, because it is the one choice that cannot be spoofed. When a proxy
 * is not recognised as trusted — a CDN with public edge addresses is not — use
 * [HeaderClientIpResolver] instead.
 */
class RemoteAddressClientIpResolver : ClientIpResolver {

    companion object {
        private val log = LoggerFactory.getLogger(RemoteAddressClientIpResolver::class.java)

        /**
         * Requests with no peer address all collapse onto this one key. Kept for
         * compatibility, but logged: unrelated callers throttling each other is
         * worth noticing rather than discovering from a support report.
         */
        const val UNKNOWN_ADDRESS = ""
    }

    override fun resolve(exchange: ServerWebExchange): String {
        val address = exchange.request.remoteAddress?.address?.hostAddress
        if (address == null) {
            log.warn(
                "No remote address on the request for {}; rate limiting counts it against a shared key. " +
                    "Set api-limit.client-ip-header, or supply a ClientIpResolver bean, if this is not a one-off.",
                exchange.request.path
            )
            return UNKNOWN_ADDRESS
        }
        return address
    }
}
