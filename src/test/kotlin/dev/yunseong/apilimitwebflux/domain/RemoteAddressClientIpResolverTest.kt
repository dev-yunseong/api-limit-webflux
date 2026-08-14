package dev.yunseong.apilimitwebflux.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import java.net.InetSocketAddress

class RemoteAddressClientIpResolverTest {

    private val resolver = RemoteAddressClientIpResolver()

    @Test
    fun `returns the socket address`() {
        val request = MockServerHttpRequest.get("/api/anything")
            .remoteAddress(InetSocketAddress("203.0.113.42", 443))
            .build()

        assertEquals("203.0.113.42", resolver.resolve(MockServerWebExchange.from(request)))
    }

    @Test
    fun `ignores any forwarding header`() {
        val request = MockServerHttpRequest.get("/api/anything")
            .remoteAddress(InetSocketAddress("172.71.18.5", 443))
            .header("CF-Connecting-IP", "203.0.113.42")
            .build()

        assertEquals("172.71.18.5", resolver.resolve(MockServerWebExchange.from(request)))
    }

    @Test
    fun `falls back to the shared unknown key when there is no peer address`() {
        val request = MockServerHttpRequest.get("/api/anything").build()

        assertEquals(
            RemoteAddressClientIpResolver.UNKNOWN_ADDRESS,
            resolver.resolve(MockServerWebExchange.from(request))
        )
    }
}
