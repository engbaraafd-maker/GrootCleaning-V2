package com.grootcleaning.automation

import com.grootcleaning.automation.engine.RetryPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class RetryPolicyTest {
    @Test fun retriesUntilSuccess() {
        var attempts = 0
        val result = RetryPolicy(2).run({ ++attempts }) { it < 3 }
        assertEquals(3, result)
        assertEquals(3, attempts)
    }
}
