package com.grootcleaning.automation

import com.grootcleaning.automation.engine.TaskQueue
import com.grootcleaning.models.AutomationTask
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskQueueTest {
    @Test fun advancesDeterministically() {
        val q = TaskQueue(listOf(AutomationTask(1,"a","A",operation=com.grootcleaning.models.Operation.CLEAR_CACHE,taskLabel="A",taskType="APP"), AutomationTask(2,"b","B",operation=com.grootcleaning.models.Operation.CLEAR_DATA,taskLabel="B",taskType="APP")))
        assertEquals(0, q.currentIndex())
        assertEquals("A", q.current()?.appName)
        q.moveNext()
        assertEquals("B", q.current()?.appName)
    }
}
