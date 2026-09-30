package com.grootcleaning.automation.detection

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AccessibilityBridge(private val service: AccessibilityService) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun snapshot(): List<NodeSnapshot> = onMain {
        val root = service.rootInActiveWindow ?: return@onMain emptyList()
        val out = mutableListOf<NodeSnapshot>()
        walk(root, out)
        out
    }

    fun currentWindowPackage(): String? = onMain { service.rootInActiveWindow?.packageName?.toString() }

    fun click(spec: NodeMatchSpec): Boolean = onMain {
        val root = service.rootInActiveWindow ?: return@onMain false
        val target = find(root) { NodeMatcher.matches(it, spec) } ?: return@onMain false
        val clickTarget = clickableAncestor(target)
        val clicked = clickTarget?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        clicked
    }

    fun scrollForward(): Boolean = onMain {
        val root = service.rootInActiveWindow ?: return@onMain false
        root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun back(): Boolean = onMain { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }

    fun open(intent: Intent): Boolean = onMain {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun waitFor(timeoutMs: Long, intervalMs: Long = 220L, predicate: (List<NodeSnapshot>) -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val nodes = snapshot()
            if (predicate(nodes)) return true
            Thread.sleep(intervalMs)
        }
        return predicate(snapshot())
    }

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val latch = CountDownLatch(1)
        val result = AtomicReference<T?>()
        mainHandler.post {
            try { result.set(block()) } finally { latch.countDown() }
        }
        latch.await(2, TimeUnit.SECONDS)
        @Suppress("UNCHECKED_CAST")
        return result.get() as T
    }

    private fun walk(node: AccessibilityNodeInfo, out: MutableList<NodeSnapshot>) {
        if (node.isVisibleToUser || node.text != null || node.contentDescription != null) {
            out += NodeMatcher.snapshot(node)
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { child -> walk(child, out) }
    }

    private fun find(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(root)) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val hit = find(child, predicate)
            if (hit != null) return hit
        }
        return null
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(6) {
            if (current?.isClickable == true) return current
            current = current?.parent
        }
        return null
    }
}
