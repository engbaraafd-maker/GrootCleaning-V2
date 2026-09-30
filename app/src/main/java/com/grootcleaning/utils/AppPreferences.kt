package com.grootcleaning.utils

import android.content.Context

object AppPreferences {
    private const val PREF = "groot_ui_prefs"
    private const val DISCLOSURE_ACCEPTED = "accessibility_disclosure_accepted"
    private const val LAST_RUN_ID = "last_run_id"
    private const val RESUME_RUN_ID = "resume_run_id"
    private const val RESUME_INDEX = "resume_index"
    private const val RESUME_STEP = "resume_step"

    private fun p(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun disclosureAccepted(context: Context) = p(context).getBoolean(DISCLOSURE_ACCEPTED, false)
    fun setDisclosureAccepted(context: Context, accepted: Boolean) = p(context).edit().putBoolean(DISCLOSURE_ACCEPTED, accepted).apply()
    fun lastRunId(context: Context) = p(context).getLong(LAST_RUN_ID, 0L)
    fun setLastRunId(context: Context, id: Long) = p(context).edit().putLong(LAST_RUN_ID, id).apply()
    fun saveResume(context: Context, runId: Long, index: Int, step: Int) = p(context).edit().putLong(RESUME_RUN_ID, runId).putInt(RESUME_INDEX, index).putInt(RESUME_STEP, step).apply()
    fun getResume(context: Context): Triple<Long, Int, Int>? {
        val id = p(context).getLong(RESUME_RUN_ID, 0L)
        if (id == 0L) return null
        return Triple(id, p(context).getInt(RESUME_INDEX, 0), p(context).getInt(RESUME_STEP, 0))
    }
    fun clearResume(context: Context) = p(context).edit().remove(RESUME_RUN_ID).remove(RESUME_INDEX).remove(RESUME_STEP).apply()
}

object GcLog {
    const val TAG = "GrootCleaning"
    fun automation(message: String) = android.util.Log.i("GC-Automation", message)
    fun accessibility(message: String) = android.util.Log.i("GC-Accessibility", message)
    fun verification(message: String) = android.util.Log.i("GC-Verification", message)
    fun accounts(message: String) = android.util.Log.i("GC-Accounts", message)
    fun storage(message: String) = android.util.Log.i("GC-Storage", message)
    fun error(area: String, message: String) = android.util.Log.e("GC-$area", message)
}
