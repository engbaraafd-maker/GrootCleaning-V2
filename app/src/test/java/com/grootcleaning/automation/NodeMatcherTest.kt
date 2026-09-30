package com.grootcleaning.automation

import com.grootcleaning.automation.detection.NodeSnapshot
import com.grootcleaning.automation.detection.NodeSnapshotMatcher
import com.grootcleaning.automation.detection.TextCatalog
import org.junit.Assert.*
import org.junit.Test

class NodeMatcherTest {
    @Test fun clearDataEnglishMatches() {
        val n = NodeSnapshot("Clear data", "", "", "android.widget.Button", "com.android.settings", true, true, true, emptyList())
        assertTrue(NodeSnapshotMatcher.anyText(listOf(n), TextCatalog.clearData))
    }
    @Test fun clearCacheArabicMatches() {
        val n = NodeSnapshot("محو ذاكرة التخزين المؤقت", "", "", "android.widget.Button", "com.android.settings", true, true, true, emptyList())
        assertTrue(NodeSnapshotMatcher.anyText(listOf(n), TextCatalog.clearCache))
    }
}
