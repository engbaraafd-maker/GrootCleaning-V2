package com.grootcleaning.automation.verification

import com.grootcleaning.automation.detection.NodeSnapshot
import com.grootcleaning.models.VerificationLevel
import com.grootcleaning.models.VerificationResult

/**
 * Evidence-driven verification. A successful click is never treated as proof by itself.
 * Storage statistics are the strongest evidence; UI-state change is an allowed fallback
 * for Basic/Standard when the target control becomes unavailable/disabled after the action.
 */
class VerificationEngine(private val level: VerificationLevel) {
    fun verifyCache(
        before: AppStorageStats?,
        after: AppStorageStats?,
        clickWorked: Boolean,
        beforeNodes: List<NodeSnapshot>,
        afterNodes: List<NodeSnapshot>,
        targetStillEnabled: Boolean
    ): VerificationResult {
        if (!clickWorked) return VerificationResult.FAILED

        if (before != null && after != null) {
            return when {
                after.cacheBytes < before.cacheBytes -> VerificationResult.SUCCESS
                before.cacheBytes == 0L && after.cacheBytes == 0L -> uiEvidence(beforeNodes, afterNodes, targetStillEnabled)
                after.cacheBytes > before.cacheBytes -> VerificationResult.UNCERTAIN
                else -> uiEvidence(beforeNodes, afterNodes, targetStillEnabled)
            }
        }

        return uiEvidence(beforeNodes, afterNodes, targetStillEnabled)
    }

    fun verifyData(
        before: AppStorageStats?,
        after: AppStorageStats?,
        clickWorked: Boolean,
        beforeNodes: List<NodeSnapshot>,
        afterNodes: List<NodeSnapshot>,
        targetStillEnabled: Boolean
    ): VerificationResult {
        if (!clickWorked) return VerificationResult.FAILED

        if (before != null && after != null) {
            return when {
                after.dataBytes < before.dataBytes -> VerificationResult.SUCCESS
                before.dataBytes == 0L && after.dataBytes == 0L -> uiEvidence(beforeNodes, afterNodes, targetStillEnabled)
                after.dataBytes > before.dataBytes -> VerificationResult.UNCERTAIN
                else -> uiEvidence(beforeNodes, afterNodes, targetStillEnabled)
            }
        }

        return uiEvidence(beforeNodes, afterNodes, targetStillEnabled)
    }

    fun verifyAccount(visibleAfter: Boolean, accessible: Boolean): VerificationResult = when {
        !accessible -> VerificationResult.UNCERTAIN
        !visibleAfter -> VerificationResult.SUCCESS
        else -> VerificationResult.FAILED
    }

    private fun uiEvidence(
        beforeNodes: List<NodeSnapshot>,
        afterNodes: List<NodeSnapshot>,
        targetStillEnabled: Boolean
    ): VerificationResult {
        if (level == VerificationLevel.STRICT) return VerificationResult.UNCERTAIN
        val changed = fingerprint(beforeNodes) != fingerprint(afterNodes)
        return when {
            changed && !targetStillEnabled -> VerificationResult.SUCCESS
            else -> VerificationResult.UNCERTAIN
        }
    }

    private fun fingerprint(nodes: List<NodeSnapshot>): String = nodes.asSequence()
        .filter { it.visible }
        .take(400)
        .joinToString("|") {
            listOf(
                it.text.trim(),
                it.contentDescription.trim(),
                it.viewId,
                it.className,
                it.enabled.toString(),
                it.clickable.toString(),
                it.parentTexts.joinToString(">")
            ).joinToString("#")
        }
}
