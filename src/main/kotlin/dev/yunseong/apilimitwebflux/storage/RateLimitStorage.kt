package dev.yunseong.apilimitwebflux.storage

import reactor.core.publisher.Mono
import kotlin.time.Duration

interface RateLimitStorage<T> {

    fun isAllowed(key: T, limit: Int, duration: Duration): Mono<Boolean>
}