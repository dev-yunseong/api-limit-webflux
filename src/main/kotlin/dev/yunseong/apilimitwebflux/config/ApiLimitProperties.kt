package dev.yunseong.apilimitwebflux.config

import dev.yunseong.apilimitwebflux.domain.IPFactor
import dev.yunseong.apilimitwebflux.domain.LimitRule
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("api-limit")
data class ApiLimitProperties(
    val rules: List<Rule> = emptyList()
)

data class Rule(
    val path: String,
    val limit: Int,         // 허용 횟수
    val duration: Duration,    // 기간 (예: 1m, 1h)
    val factor: LimitFactor // 제한 기준 (IP, HEADER 등)
) {
    fun toDomain(): LimitRule<Any> {
        return LimitRule(path, limit, duration, factor.domain)
    }
}

enum class LimitFactor(val domain: IPFactor) {
    IP(IPFactor())

}