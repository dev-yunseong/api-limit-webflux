package dev.yunseong.apilimitwebflux.storage

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

import java.time.Duration

@Component
class InMemoryRateLimitStorage : RateLimitStorage<Any> {

    private val apiCounts = mutableMapOf<Any, ApiCount>()

    companion object {
        private val log = LoggerFactory.getLogger(InMemoryRateLimitStorage::class.java)
    }

    override fun isAllowed(
        key: Any,
        limit: Int,
        duration: Duration
    ): Mono<Boolean> {
        log.debug("Checking if key '{}' is allowed with limit '{}' and duration '{}'", key, limit, duration)

        val apiCount = apiCounts.compute(key) { _, existing ->
            if (existing == null || existing.isExpired()) {
                log.debug("Creating new entry for key '{}'", key)
                ApiCount(duration).apply { getAndIncrement() }
            } else {
                val count = existing.getAndIncrement()
                log.debug("Incrementing count for key '{}' to {}", key, count + 1)
                existing
            }
        }!!

        val isAllowed = apiCount.count.get() <= limit
        log.debug("Key '{}' is allowed: {}", key, isAllowed)
        return Mono.just(isAllowed)
    }

    @Scheduled(cron = "0 */5 * * * *")
    private fun checkExpiredAndRemove() {
        log.debug("Running scheduled cleanup task")
        val initialSize = apiCounts.size
        apiCounts.entries.removeIf { entry ->
            val expired = entry.value.isExpired()
            if (expired) {
                log.debug("Removing expired key '{}'", entry.key)
            }
            expired
        }
        val finalSize = apiCounts.size
        log.debug("Cleanup task finished. Removed {} entries.", initialSize - finalSize)
    }

    private class ApiCount(duration: Duration) {

        var count: AtomicInteger = AtomicInteger(0)
        var ttl: Long = System.currentTimeMillis() + duration.toMillis()

        fun getAndIncrement(): Int {
            return count.getAndIncrement()
        }

        fun isExpired(): Boolean {
            return ttl < System.currentTimeMillis()
        }
    }
}