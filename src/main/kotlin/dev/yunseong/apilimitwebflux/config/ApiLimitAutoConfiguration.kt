package dev.yunseong.apilimitwebflux.config

import dev.yunseong.apilimitwebflux.domain.ClientIpResolver
import dev.yunseong.apilimitwebflux.domain.HeaderClientIpResolver
import dev.yunseong.apilimitwebflux.domain.LimitRule
import dev.yunseong.apilimitwebflux.domain.RemoteAddressClientIpResolver
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.context.annotation.Bean
import dev.yunseong.apilimitwebflux.storage.InMemoryRateLimitStorage
import dev.yunseong.apilimitwebflux.filter.ApiLimitFilter
import dev.yunseong.apilimitwebflux.storage.RateLimitStorage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties

@AutoConfiguration
@EnableConfigurationProperties(ApiLimitProperties::class)
@EnableScheduling
class ApiLimitAutoConfiguration(
    private val properties: ApiLimitProperties
) {

    companion object {
        private val log = LoggerFactory.getLogger(ApiLimitAutoConfiguration::class.java)
    }

    @Bean
    @ConditionalOnMissingBean
    fun rateLimitStorage(): InMemoryRateLimitStorage {
        log.debug("Creating InMemoryRateLimitStorage bean")
        return InMemoryRateLimitStorage()
    }

    /**
     * Backs off entirely when the application defines its own, which is the
     * supported way to derive the visitor address from something this library
     * does not model.
     */
    @Bean
    @ConditionalOnMissingBean
    fun clientIpResolver(): ClientIpResolver {
        val header = properties.clientIpHeader
        if (header.isNullOrBlank()) {
            log.info("Rate limiting by IP counts against the socket address")
            return RemoteAddressClientIpResolver()
        }
        log.info(
            "Rate limiting by IP counts against the '{}' header, falling back to the socket " +
                "address when it is absent or not an IP literal. Only safe while the origin " +
                "refuses requests that bypass the proxy.",
            header
        )
        return HeaderClientIpResolver(header)
    }

    @Bean
    @ConditionalOnMissingBean
    fun apiLimitFilter(
        ruleProvider: ObjectProvider<LimitRule<Any>>,
        storage: RateLimitStorage<Any>,
        clientIpResolver: ClientIpResolver
    ): ApiLimitFilter {
        log.debug("Creating ApiLimitFilter bean")
        val yamlRules = properties.rules.map { it.toDomain(clientIpResolver) }

        val customRules = ruleProvider.orderedStream().toList()

        val allRules = yamlRules + customRules

        log.info("Total {} rules loaded (YAML: {}, Custom: {})",
            allRules.size, yamlRules.size, customRules.size)
        return ApiLimitFilter(allRules, storage)
    }
}