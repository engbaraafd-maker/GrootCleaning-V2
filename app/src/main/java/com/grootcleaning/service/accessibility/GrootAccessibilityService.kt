package com.grootcleaning.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.grootcleaning.automation.engine.AutomationEngine
import com.grootcleaning.utils.GcLog

class GrootAccessibilityService : AccessibilityService() {
    lateinit var engine: AutomationEngine
        private set

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_START -> engine.startFresh()
                ACTION_STOP -> engine.requestStop()
                ACTION_RESUME -> engine.resume()
                ACTION_RETRY_FAILED -> engine.retryFailed()
                ACTION_SKIP -> engine.skipCurrent()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        INSTANCE = this
        engine = AutomationEngine(applicationContext, this)
        val filter = IntentFilter().apply {
            addAction(ACTION_START); addAction(ACTION_STOP); addAction(ACTION_RESUME); addAction(ACTION_RETRY_FAILED); addAction(ACTION_SKIP)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(commandReceiver, filter, RECEIVER_NOT_EXPORTED) else registerReceiver(commandReceiver, filter)
        GcLog.accessibility("Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        engine.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        if (::engine.isInitialized) engine.requestStop()
        GcLog.accessibility("Service interrupted")
    }

    override fun onDestroy() {
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        INSTANCE = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.grootcleaning.action.START"
        const val ACTION_STOP = "com.grootcleaning.action.STOP"
        const val ACTION_RESUME = "com.grootcleaning.action.RESUME"
        const val ACTION_RETRY_FAILED = "com.grootcleaning.action.RETRY_FAILED"
        const val ACTION_SKIP = "com.grootcleaning.action.SKIP"
        const val ACTION_STATE = "com.grootcleaning.action.STATE"
        const val EXTRA_MESSAGE = "message"
        @Volatile var INSTANCE: GrootAccessibilityService? = null
    }
}
