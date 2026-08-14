package dev.yunseong.apilimitwebflux.filter

import dev.yunseong.apilimitwebflux.domain.ClientIpResolver
import dev.yunseong.apilimitwebflux.domain.HeaderClientIpResolver
import dev.yunseong.apilimitwebflux.domain.IPFactor
import dev.yunseong.apilimitwebflux.domain.LimitRule
import dev.yunseong.apilimitwebflux.storage.InMemoryRateLimitStorage
import dev.yunseong.apilimitwebflux.storage.RateLimitStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.time.Duration

/**
 * The behaviour the library is really judged on: which requests end up sharing a
 * bucket. Everything here goes through the filter rather than the factor, because
 * a per-visitor limit that silently becomes site-wide looks fine at every smaller
 * seam.
 */
class ApiLimitFilterTest {

    private val path = "/api/public/chat"
    private val header = "CF-Connecting-IP"
    private val edgeIp = "172.71.18.5"

    private lateinit var storage: RateLimitStorage<Any>

    @BeforeEach
    fun setUp() {
        storage = InMemoryRateLimitStorage()
    }

    private fun filterWith(resolver: ClientIpResolver, limit: Int): ApiLimitFilter {
        val rule = LimitRule<Any>(path, limit, Duration.ofDays(1), IPFactor(resolver))
        return ApiLimitFilter(listOf(rule), storage)
    }

    /** A request arriving through a proxy: same socket peer, different forwarded visitor. */
    private fun requestFrom(visitorIp: String?, requestPath: String = path): ServerWebExchange {
        val request = MockServerHttpRequest.get(requestPath)
            .remoteAddress(InetSocketAddress(edgeIp, 443))
            .apply { visitorIp?.let { header(header, it) } }
            .build()
        return MockServerWebExchange.from(request)
    }

    private fun send(filter: ApiLimitFilter, exchange: ServerWebExchange): HttpStatus {
        val chain = WebFilterChain { Mono.empty() }
        filter.filter(exchange, chain).block()
        return exchange.response.statusCode as? HttpStatus ?: HttpStatus.OK
    }

    @Test
    fun `counts each forwarded visitor separately`() {
        val filter = filterWith(HeaderClientIpResolver(header), 1)

        assertEquals(HttpStatus.OK, send(filter, requestFrom("203.0.113.42")))

        // A different visitor behind the same proxy still has their own quota.
        assertEquals(HttpStatus.OK, send(filter, requestFrom("198.51.100.7")))

        // The first visitor has spent theirs.
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, send(filter, requestFrom("203.0.113.42")))
    }

    @Test
    fun `without a resolver every visitor behind the proxy shares one quota`() {
        // The pre-fix behaviour, pinned so the regression is visible rather than implied.
        val filter = filterWith(
            ClientIpResolver { it.request.remoteAddress?.address?.hostAddress ?: "" },
            1
        )

        assertEquals(HttpStatus.OK, send(filter, requestFrom("203.0.113.42")))
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, send(filter, requestFrom("198.51.100.7")))
    }

    @Test
    fun `blocked requests do not reach the chain`() {
        val filter = filterWith(HeaderClientIpResolver(header), 1)
        send(filter, requestFrom("203.0.113.42"))

        var reached = false
        val chain = WebFilterChain { reached = true; Mono.empty() }
        val exchange = requestFrom("203.0.113.42")
        filter.filter(exchange, chain).block()

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.response.statusCode)
        assertFalse(reached)
    }

    @Test
    fun `allowed requests reach the chain`() {
        val filter = filterWith(HeaderClientIpResolver(header), 1)

        var reached = false
        val chain = WebFilterChain { reached = true; Mono.empty() }
        filter.filter(requestFrom("203.0.113.42"), chain).block()

        assertTrue(reached)
    }

    @Test
    fun `requests outside the rule path are not counted`() {
        val filter = filterWith(HeaderClientIpResolver(header), 1)

        repeat(5) {
            assertEquals(
                HttpStatus.OK,
                send(filter, requestFrom("203.0.113.42", "/api/public/other"))
            )
        }
    }

    @Test
    fun `a visitor without the header falls back to the socket address`() {
        val filter = filterWith(HeaderClientIpResolver(header), 1)

        assertEquals(HttpStatus.OK, send(filter, requestFrom(null)))
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, send(filter, requestFrom(null)))
    }

    @Test
    fun `separate visitors produce separate keys`() {
        val factor = IPFactor(HeaderClientIpResolver(header))

        assertNotEquals(
            factor.getKey(requestFrom("203.0.113.42")),
            factor.getKey(requestFrom("198.51.100.7"))
        )
    }
}
