package dev.yunseong.apilimitwebflux.domain

import java.time.Duration

class LimitRule<out T>(
    val path: String,
    val limit: Int,
    val duration: Duration,
    val factor: Factor<out T>
) {
}