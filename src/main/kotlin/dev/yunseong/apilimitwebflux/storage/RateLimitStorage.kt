package dev.yunseong.apilimitwebflux.storage

import reactor.core.publisher.Mono
import java.time.Duration

interface RateLimitStorage<T> {

    fun isAllowed(key: T, limit: Int, duration: Duration): Mono<Boolean>
}