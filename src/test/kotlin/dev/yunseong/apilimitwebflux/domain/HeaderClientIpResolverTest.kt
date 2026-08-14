package dev.yunseong.apilimitwebflux.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import java.net.InetSocketAddress

class HeaderClientIpResolverTest {

    private val header = "CF-Connecting-IP"
    private val edgeIp = "172.71.18.5"

    private val resolver = HeaderClientIpResolver(header)

    private fun exchange(headerValue: String? = null): ServerWebExchange {
        val request = MockServerHttpRequest.get("/api/anything")
            .remoteAddress(InetSocketAddress(edgeIp, 443))
            .apply { headerValue?.let { header(header, it) } }
            .build()
        return MockServerWebExchange.from(request)
    }

    @Test
    fun `prefers the address the proxy forwards`() {
        assertEquals("203.0.113.42", resolver.resolve(exchange("203.0.113.42")))
    }

    @Test
    fun `keeps IPv6 visitors intact`() {
        assertEquals("2001:db8::1", resolver.resolve(exchange("2001:db8::1")))
        assertEquals("::ffff:1.2.3.4", resolver.resolve(exchange("::ffff:1.2.3.4")))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals("203.0.113.42", resolver.resolve(exchange(" 203.0.113.42 ")))
    }

    @Test
    fun `falls back to the socket address without the header`() {
        assertEquals(edgeIp, resolver.resolve(exchange()))
    }

    @Test
    fun `falls back when the header is blank`() {
        assertEquals(edgeIp, resolver.resolve(exchange("")))
        assertEquals(edgeIp, resolver.resolve(exchange("   ")))
    }

    @Test
    fun `falls back when the header is not an address`() {
        // A forged header must not become the storage key verbatim.
        assertEquals(edgeIp, resolver.resolve(exchange("attacker.example.com")))
        assertEquals(edgeIp, resolver.resolve(exchange("203.0.113.42, 198.51.100.7")))
        assertEquals(edgeIp, resolver.resolve(exchange("x".repeat(500))))
    }

    @Test
    fun `reads the header case insensitively`() {
        val request = MockServerHttpRequest.get("/api/anything")
            .remoteAddress(InetSocketAddress(edgeIp, 443))
            .header("cf-connecting-ip", "203.0.113.42")
            .build()

        assertEquals("203.0.113.42", resolver.resolve(MockServerWebExchange.from(request)))
    }

    @Test
    fun `rejects a blank header name`() {
        assertThrows<IllegalArgumentException> { HeaderClientIpResolver("  ") }
    }

    @Test
    fun `isIpLiteral accepts addresses and rejects everything else`() {
        assertTrue(HeaderClientIpResolver.isIpLiteral("8.8.8.8"))
        assertTrue(HeaderClientIpResolver.isIpLiteral("203.0.113.42"))
        assertTrue(HeaderClientIpResolver.isIpLiteral("2001:db8::1"))

        assertFalse(HeaderClientIpResolver.isIpLiteral("attacker.example.com"))
        assertFalse(HeaderClientIpResolver.isIpLiteral("localhost"))
        assertFalse(HeaderClientIpResolver.isIpLiteral("1.2.3.4.example.com"))
        assertFalse(HeaderClientIpResolver.isIpLiteral(""))
        assertFalse(HeaderClientIpResolver.isIpLiteral(null))
    }
}
