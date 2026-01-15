package dev.yunseong.apilimitwebflux.storage

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger


import kotlin.time.Duration

@Component
class InMemoryRateLimitStorage : RateLimitStorage<Any> {

    private val apiCounts = mutableMapOf<Any, ApiCount>()

    override fun isAllowed(
        key: Any,
        limit: Int,
        duration: Duration
    ): Mono<Boolean> {

        val apiCount = apiCounts.compute(key) { _, existing ->
            if (existing == null || existing.isExpired()) {
                ApiCount(duration).apply { getAndIncrement() }
            } else {
                existing.getAndIncrement()
                existing
            }
        }!!

        return Mono.just(apiCount.count.get() <= limit)
    }

    @Scheduled(cron = "0 */5 * * * *")
    private fun checkExpiredAndRemove() {
        apiCounts.entries.removeIf { entry -> entry.value.isExpired() }
    }

    private class ApiCount(duration: Duration) {

        var count: AtomicInteger = AtomicInteger(0)
        var ttl: Long = System.currentTimeMillis() + duration.inWholeMilliseconds

        fun getAndIncrement(): Int {
            return count.getAndIncrement()
        }

        fun isExpired(): Boolean {
            return ttl < System.currentTimeMillis()
        }
    }
}