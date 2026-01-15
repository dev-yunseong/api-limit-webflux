package dev.yunseong.apilimitwebflux.config

import dev.yunseong.apilimitwebflux.domain.LimitRule
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.context.annotation.Bean
import dev.yunseong.apilimitwebflux.storage.InMemoryRateLimitStorage
import dev.yunseong.apilimitwebflux.filter.ApiLimitFilter
import dev.yunseong.apilimitwebflux.storage.RateLimitStorage
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties

@AutoConfiguration
@EnableConfigurationProperties(ApiLimitProperties::class)
@EnableScheduling
class ApiLimitAutoConfiguration(
    private val properties: ApiLimitProperties
) {

    @Bean
    fun limitRules(): List<LimitRule<Any>> {
        return properties.rules.map { it.toDomain() }
    }

    @Bean
    @ConditionalOnMissingBean
    fun rateLimitStorage(): InMemoryRateLimitStorage {
        return InMemoryRateLimitStorage()
    }

    @Bean
    @ConditionalOnMissingBean
    fun apiLimitFilter(ruleProvider: ObjectProvider<LimitRule<Any>>, storage: RateLimitStorage<Any>): ApiLimitFilter {
        return ApiLimitFilter(ruleProvider, storage)
    }
}