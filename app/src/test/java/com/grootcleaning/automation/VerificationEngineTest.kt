package com.grootcleaning.automation

import com.grootcleaning.automation.detection.NodeSnapshot
import com.grootcleaning.automation.verification.AppStorageStats
import com.grootcleaning.automation.verification.VerificationEngine
import com.grootcleaning.models.VerificationLevel
import com.grootcleaning.models.VerificationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationEngineTest {
    private fun node(text: String, enabled: Boolean = true) = NodeSnapshot(
        text = text, contentDescription = "", viewId = "", className = "android.widget.Button",
        packageName = "com.android.settings", clickable = true, enabled = enabled, visible = true, parentTexts = emptyList()
    )

    @Test fun cacheDecreaseIsSuccess() {
        val v = VerificationEngine(VerificationLevel.STANDARD)
        assertEquals(
            VerificationResult.SUCCESS,
            v.verifyCache(AppStorageStats(100, 500), AppStorageStats(100, 0), true, listOf(node("Clear cache")), emptyList(), false)
        )
    }

    @Test fun uiChangeWithoutTargetIsSuccessAtStandard() {
        val v = VerificationEngine(VerificationLevel.STANDARD)
        assertEquals(
            VerificationResult.SUCCESS,
            v.verifyData(null, null, true, listOf(node("Clear data")), listOf(node("Storage")), false)
        )
    }

    @Test fun clickAloneIsNotSuccess() {
        val v = VerificationEngine(VerificationLevel.STANDARD)
        assertEquals(
            VerificationResult.UNCERTAIN,
            v.verifyData(null, null, true, listOf(node("Clear data")), listOf(node("Clear data")), true)
        )
    }

    @Test fun strictWithoutStatsIsUncertain() {
        val v = VerificationEngine(VerificationLevel.STRICT)
        assertEquals(
            VerificationResult.UNCERTAIN,
            v.verifyData(null, null, true, listOf(node("Clear data")), emptyList(), false)
        )
    }
}
