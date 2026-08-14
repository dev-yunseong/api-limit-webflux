package dev.yunseong.apilimitwebflux.config

import dev.yunseong.apilimitwebflux.domain.ClientIpResolver
import dev.yunseong.apilimitwebflux.domain.Factor
import dev.yunseong.apilimitwebflux.domain.IPFactor
import dev.yunseong.apilimitwebflux.domain.LimitRule
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("api-limit")
data class ApiLimitProperties(
    val rules: List<Rule> = emptyList(),
    /**
     * Header carrying the visitor address, for `factor: IP` behind a proxy the
     * platform does not treat as trusted. Unset means the socket peer is used.
     * Only set it when the origin refuses traffic that bypasses the proxy — any
     * named header is forgeable by a direct caller.
     */
    val clientIpHeader: String? = null
)

data class Rule(
    val path: String,
    val limit: Int,         // 허용 횟수
    val duration: Duration,    // 기간 (예: 1m, 1h)
    val factor: LimitFactor // 제한 기준 (IP, HEADER 등)
) {
    fun toDomain(clientIpResolver: ClientIpResolver): LimitRule<Any> {
        return LimitRule(path, limit, duration, factor.toFactor(clientIpResolver))
    }
}

enum class LimitFactor {
    IP;

    /**
     * Built per rule rather than held on the constant: the IP factor needs the
     * application's resolver, which does not exist yet when the enum loads.
     */
    fun toFactor(clientIpResolver: ClientIpResolver): Factor<Any> {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            IP -> IPFactor(clientIpResolver) as Factor<Any>
        }
    }
}
