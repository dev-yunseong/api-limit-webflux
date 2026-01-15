package dev.yunseong.apilimitwebflux.filter

import dev.yunseong.apilimitwebflux.domain.LimitRule
import dev.yunseong.apilimitwebflux.storage.RateLimitStorage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class ApiLimitFilter(val ruleProvider: ObjectProvider<LimitRule<Any>>, val rateLimitStorage: RateLimitStorage<Any>) : WebFilter {

    private val pathParser = PathPatternParser()

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void> {
        val requestPath = exchange.request.path.pathWithinApplication()

        val matchedRules = ruleProvider.orderedStream().filter { rule ->
            val pattern = pathParser.parse(rule.path)
            pattern.matches(requestPath)
        }.toList()

        return mono {
            if (isAllowed(exchange, matchedRules)) {
                return@mono chain.filter(exchange).awaitSingleOrNull()
            } else {
                exchange.response.statusCode = org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
                return@mono exchange.response.setComplete().awaitSingleOrNull()
            }
        }.then()
    }

    private suspend fun isAllowed(exchange: ServerWebExchange, rules: List<LimitRule<Any>> ): Boolean {
        var fullyAllowed = true

        for (rule in rules) {
            val isAllowed = rateLimitStorage.isAllowed(
                rule.factor.getKey(exchange),
                rule.limit,
                rule.duration,
            ).awaitSingle()

            fullyAllowed = fullyAllowed && isAllowed
        }

        return fullyAllowed
    }
}