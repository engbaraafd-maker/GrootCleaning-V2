package com.grootcleaning.automation.verification

import com.grootcleaning.models.VerificationLevel

/** Central policy hook for changing verification strictness without touching UI or automation actions. */
object VerificationPolicy {
    fun requiresStorageEvidence(level: VerificationLevel): Boolean = level == VerificationLevel.STRICT
    fun allowsUiFallback(level: VerificationLevel): Boolean = level != VerificationLevel.STRICT
}
