package dev.yunseong.apilimitwebflux.filter

import dev.yunseong.apilimitwebflux.domain.LimitRule
import dev.yunseong.apilimitwebflux.storage.RateLimitStorage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono

@Component
class ApiLimitFilter(val rules: List<LimitRule<Any>>, val rateLimitStorage: RateLimitStorage<Any>) : WebFilter {

    private val pathParser = PathPatternParser()

    companion object {
        private val log = LoggerFactory.getLogger(ApiLimitFilter::class.java)
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void> {
        val requestPath = exchange.request.path.pathWithinApplication()
        log.debug("Request path: {}", requestPath)

        val matchedRules = rules.stream().filter { rule ->
            log.debug("Matching rule path: {}", rule.path)
            val pattern = pathParser.parse(rule.path)
            pattern.matches(requestPath)
        }.toList()
        log.debug("Matched rules: {}", matchedRules)

        return mono {
            if (isAllowed(exchange, matchedRules)) {
                log.debug("Request allowed")
                return@mono chain.filter(exchange).awaitSingleOrNull()
            } else {
                log.warn("Request blocked")
                exchange.response.statusCode = org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
                return@mono exchange.response.setComplete().awaitSingleOrNull()
            }
        }.then()
    }

    private suspend fun isAllowed(exchange: ServerWebExchange, rules: List<LimitRule<Any>> ): Boolean {
        var fullyAllowed = true

        for (rule in rules) {
            val key = rule.factor.getKey(exchange)
            val isAllowed = rateLimitStorage.isAllowed(
                key,
                rule.limit,
                rule.duration,
            ).awaitSingle()
            log.debug("Rule: {}, Key: {}, Allowed: {}", rule, key, isAllowed)

            fullyAllowed = fullyAllowed && isAllowed
        }

        return fullyAllowed
    }
}