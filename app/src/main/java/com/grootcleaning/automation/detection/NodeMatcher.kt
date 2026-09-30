package com.grootcleaning.automation.detection

import android.view.accessibility.AccessibilityNodeInfo

data class NodeSnapshot(
    val text: String,
    val contentDescription: String,
    val viewId: String,
    val className: String,
    val packageName: String,
    val clickable: Boolean,
    val enabled: Boolean,
    val visible: Boolean,
    val parentTexts: List<String>
)

data class NodeMatchSpec(
    val texts: List<String> = emptyList(),
    val contentDescriptions: List<String> = emptyList(),
    val viewIds: List<String> = emptyList(),
    val classNames: List<String> = emptyList(),
    val requireClickable: Boolean = false,
    val requireEnabled: Boolean = false,
    val packageNames: List<String> = emptyList()
)

object NodeMatcher {
    fun matches(node: AccessibilityNodeInfo, spec: NodeMatchSpec): Boolean {
        if (spec.requireClickable && !node.isClickable) return false
        if (spec.requireEnabled && !node.isEnabled) return false
        if (spec.packageNames.isNotEmpty() && !spec.packageNames.contains(node.packageName?.toString())) return false
        if (spec.classNames.isNotEmpty() && spec.classNames.none { node.className?.toString()?.contains(it, ignoreCase = true) == true }) return false

        val text = normalizeUiText(node.text)
        val desc = normalizeUiText(node.contentDescription)
        val viewId = normalizeUiText(node.viewIdResourceName)

        val textMatches = spec.texts.isEmpty() || spec.texts.any { containsAny(text, listOf(it)) || containsAny(desc, listOf(it)) }
        val descMatches = spec.contentDescriptions.isEmpty() || spec.contentDescriptions.any { containsAny(desc, listOf(it)) }
        val idMatches = spec.viewIds.isEmpty() || spec.viewIds.any { viewId == normalizeUiText(it) || viewId.endsWith(":" + normalizeUiText(it)) || viewId.contains(normalizeUiText(it)) }
        return textMatches && descMatches && idMatches
    }

    fun snapshot(node: AccessibilityNodeInfo, maxParents: Int = 3): NodeSnapshot {
        val parents = mutableListOf<String>()
        var p = node.parent
        repeat(maxParents) {
            if (p == null) return@repeat
            val value = p.text?.toString()?.trim().orEmpty()
            if (value.isNotEmpty()) parents += value
            p = p.parent
        }
        return NodeSnapshot(
            text = node.text?.toString().orEmpty(),
            contentDescription = node.contentDescription?.toString().orEmpty(),
            viewId = node.viewIdResourceName.orEmpty(),
            className = node.className?.toString().orEmpty(),
            packageName = node.packageName?.toString().orEmpty(),
            clickable = node.isClickable,
            enabled = node.isEnabled,
            visible = node.isVisibleToUser,
            parentTexts = parents
        )
    }
}

object NodeSnapshotMatcher {
    fun anyText(nodes: List<NodeSnapshot>, terms: List<String>): Boolean = nodes.any { n ->
        terms.any { term -> containsAny(n.text, listOf(term)) || containsAny(n.contentDescription, listOf(term)) }
    }
}
