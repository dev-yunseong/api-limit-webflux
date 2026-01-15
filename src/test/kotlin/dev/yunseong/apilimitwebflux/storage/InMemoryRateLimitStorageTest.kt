package dev.yunseong.apilimitwebflux.storage

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class InMemoryRateLimitStorageTest {

    private lateinit var storage: InMemoryRateLimitStorage

    @BeforeEach
    fun setUp() {
        storage = InMemoryRateLimitStorage()
    }

    @Test
    fun `isAllowed should return true when below limit`() = runTest {
        val key = "test-key"
        val limit = 5
        val duration = Duration.ofMinutes(1)

        repeat(limit - 1) {
            storage.isAllowed(key, limit, duration).block()
        }

        val isAllowed = storage.isAllowed(key, limit, duration).block()
        assertTrue(isAllowed!!)
    }

    @Test
    fun `isAllowed should return false when limit is exceeded`() = runTest {
        val key = "test-key"
        val limit = 3
        val duration = Duration.ofMinutes(1)

        repeat(limit) {
            storage.isAllowed(key, limit, duration).block()
        }

        val isAllowed = storage.isAllowed(key, limit, duration).block()
        assertFalse(isAllowed!!)
    }
}
