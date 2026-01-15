package dev.yunseong.apilimitwebflux.config

import dev.yunseong.apilimitwebflux.domain.LimitRule
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

    @Bean
    @ConditionalOnMissingBean
    fun apiLimitFilter(ruleProvider: ObjectProvider<LimitRule<Any>>, storage: RateLimitStorage<Any>): ApiLimitFilter {
        log.debug("Creating ApiLimitFilter bean")
        val yamlRules = properties.rules.map { it.toDomain() }

        val customRules = ruleProvider.orderedStream().toList()

        val allRules = yamlRules + customRules

        log.info("Total {} rules loaded (YAML: {}, Custom: {})",
            allRules.size, yamlRules.size, customRules.size)
        return ApiLimitFilter(allRules, storage)
    }
}