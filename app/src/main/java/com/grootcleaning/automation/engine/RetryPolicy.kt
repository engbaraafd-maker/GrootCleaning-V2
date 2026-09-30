package com.grootcleaning.automation.engine

class RetryPolicy(private val retryCount: Int) {
    fun <T> run(block: () -> T, shouldRetry: (T) -> Boolean): T {
        var last: T? = null
        repeat((retryCount + 1).coerceAtLeast(1)) {
            val result = block()
            last = result
            if (!shouldRetry(result)) return result
        }
        @Suppress("UNCHECKED_CAST")
        return last as T
    }
}
